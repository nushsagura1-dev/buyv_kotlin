"""
Phase 6: Tracking & Analytics Endpoints
Handles tracking of affiliate clicks, Reel views, and conversions
"""
from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from sqlalchemy import func, and_
from datetime import datetime, timedelta
from typing import Optional
from pydantic import BaseModel

from .database import get_db
from .models import AffiliateClick, ReelView, PromoterWallet, Commission, Order, OrderItem, User
from .marketplace.models import MarketplaceProduct
from .auth import get_current_user_uid, get_current_user_optional

router = APIRouter(prefix="/api/marketplace", tags=["Tracking"])


# ============ Schemas ============
class TrackReelViewRequest(BaseModel):
    reel_id: str
    promoter_uid: str
    product_id: Optional[str] = None
    viewer_uid: Optional[str] = None  # null for anonymous
    session_id: Optional[str] = None
    watch_duration: Optional[int] = None  # seconds
    completion_rate: Optional[float] = None  # 0.0 to 1.0


class TrackClickRequest(BaseModel):
    reel_id: str
    product_id: str
    promoter_uid: str
    viewer_uid: Optional[str] = None  # null for anonymous
    session_id: Optional[str] = None
    device_info: Optional[dict] = None  # {"device": "Android", "os": "13", "app_version": "1.0.0"}


class TrackConversionRequest(BaseModel):
    """Called when order is created from affiliate link"""
    order_id: int
    click_session_id: str  # Match with AffiliateClick.session_id


class TrackingResponse(BaseModel):
    success: bool
    message: str
    tracking_id: Optional[int] = None


# ============ Endpoints ============
@router.post("/track/view", response_model=TrackingResponse)
async def track_reel_view(
    request: TrackReelViewRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User | None = Depends(get_current_user_optional)
):
    """
    Track Reel view/impression
    Called automatically when Reel appears on screen (>50% visible for 1+ seconds)
    """
    try:
        # Check if view already exists (prevent duplicate tracking)
        if request.viewer_uid and request.session_id:
            existing = db.query(ReelView).filter(
                and_(
                    ReelView.reel_id == request.reel_id,
                    ReelView.viewer_uid == request.viewer_uid,
                    ReelView.session_id == request.session_id
                )
            ).first()
            if existing:
                return TrackingResponse(
                    success=True,
                    message="View already tracked",
                    tracking_id=existing.id
                )
        
        # Create view record
        view = ReelView(
            reel_id=request.reel_id,
            promoter_uid=request.promoter_uid,
            product_id=request.product_id,
            viewer_uid=request.viewer_uid,
            session_id=request.session_id,
            watch_duration=request.watch_duration,
            completion_rate=request.completion_rate,
            created_at=datetime.utcnow()
        )
        db.add(view)
        db.commit()
        db.refresh(view)
        
        # Update promoter wallet stats (async)
        background_tasks.add_task(update_promoter_views, db, request.promoter_uid)
        
        return TrackingResponse(
            success=True,
            message="Reel view tracked",
            tracking_id=view.id
        )
    
    except Exception as e:
        db.rollback()
        print(f"Error tracking view: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to track view: {str(e)}")


@router.post("/track/click", response_model=TrackingResponse)
async def track_affiliate_click(
    request: TrackClickRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User | None = Depends(get_current_user_optional)
):
    """
    Track click on Marketplace product badge in Reel
    Called when user taps the orange product badge
    """
    try:
        # Create click record
        click = AffiliateClick(
            viewer_uid=request.viewer_uid,
            reel_id=request.reel_id,
            product_id=request.product_id,
            promoter_uid=request.promoter_uid,
            session_id=request.session_id,
            device_info=str(request.device_info) if request.device_info else None,
            created_at=datetime.utcnow(),
            converted=False
        )
        db.add(click)
        db.commit()
        db.refresh(click)
        
        # Update promoter wallet stats (async)
        background_tasks.add_task(update_promoter_clicks, db, request.promoter_uid)
        
        return TrackingResponse(
            success=True,
            message="Click tracked",
            tracking_id=click.id
        )
    
    except Exception as e:
        db.rollback()
        print(f"Error tracking click: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to track click: {str(e)}")


@router.post("/track/conversion", response_model=TrackingResponse)
async def track_conversion(
    request: TrackConversionRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user_uid: str = Depends(get_current_user_uid)
):
    """
    Track conversion (purchase made from affiliate link)
    Called when order is placed and contains items from affiliate click
    """
    try:
        # Find the click record
        click = db.query(AffiliateClick).filter(
            AffiliateClick.session_id == request.click_session_id,
            AffiliateClick.converted == False
        ).first()
        
        if not click:
            return TrackingResponse(
                success=False,
                message="No matching click found or already converted"
            )
        
        # Update click record
        click.converted = True
        click.converted_at = datetime.utcnow()
        click.order_id = request.order_id
        
        # Calculate commission
        order = db.query(Order).filter(Order.id == request.order_id).first()
        if order:
            background_tasks.add_task(
                calculate_and_create_commission,
                db,
                order,
                click.promoter_uid,
                click.product_id
            )
        
        db.commit()
        
        return TrackingResponse(
            success=True,
            message="Conversion tracked successfully",
            tracking_id=click.id
        )
    
    except Exception as e:
        db.rollback()
        print(f"Error tracking conversion: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to track conversion: {str(e)}")


# ============ Analytics Endpoints ============
@router.get("/analytics/promoter/{promoter_uid}")
async def get_promoter_analytics(
    promoter_uid: str,
    days: int = 30,
    db: Session = Depends(get_db),
    current_user_uid: str = Depends(get_current_user_uid)
):
    """
    Get analytics for a promoter (views, clicks, conversions, earnings)
    Only accessible by the promoter themselves
    """
    if current_user_uid != promoter_uid:
        raise HTTPException(status_code=403, detail="Unauthorized")
    
    # Date range
    start_date = datetime.utcnow() - timedelta(days=days)
    
    # Get wallet info
    wallet = db.query(PromoterWallet).filter(
        PromoterWallet.user_id == promoter_uid
    ).first()
    
    if not wallet:
        # Create wallet if doesn't exist
        wallet = PromoterWallet(user_id=promoter_uid)
        db.add(wallet)
        db.commit()
        db.refresh(wallet)
    
    # Get metrics
    total_views = db.query(func.count(ReelView.id)).filter(
        ReelView.promoter_uid == promoter_uid,
        ReelView.created_at >= start_date
    ).scalar() or 0
    
    total_clicks = db.query(func.count(AffiliateClick.id)).filter(
        AffiliateClick.promoter_uid == promoter_uid,
        AffiliateClick.created_at >= start_date
    ).scalar() or 0
    
    total_conversions = db.query(func.count(AffiliateClick.id)).filter(
        AffiliateClick.promoter_uid == promoter_uid,
        AffiliateClick.converted == True,
        AffiliateClick.created_at >= start_date
    ).scalar() or 0
    
    # Get earnings
    pending_commissions = db.query(func.sum(Commission.commission_amount)).filter(
        Commission.user_uid == promoter_uid,
        Commission.status == "pending"
    ).scalar() or 0.0
    
    approved_commissions = db.query(func.sum(Commission.commission_amount)).filter(
        Commission.user_uid == promoter_uid,
        Commission.status == "approved"
    ).scalar() or 0.0
    
    # CTR and conversion rate
    ctr = (total_clicks / total_views * 100) if total_views > 0 else 0.0
    conversion_rate = (total_conversions / total_clicks * 100) if total_clicks > 0 else 0.0
    
    return {
        "promoter_uid": promoter_uid,
        "period_days": days,
        "metrics": {
            "views": total_views,
            "clicks": total_clicks,
            "conversions": total_conversions,
            "ctr": round(ctr, 2),  # Click-through rate
            "conversion_rate": round(conversion_rate, 2)
        },
        "earnings": {
            "total_earned": float(wallet.total_earned or 0.0),
            "pending_balance": float(wallet.pending_amount or 0.0),
            "available_balance": float(wallet.available_amount or 0.0),
            "withdrawn_total": float(wallet.withdrawn_amount or 0.0),
            "pending_commissions": float(pending_commissions or 0.0),
            "approved_commissions": float(approved_commissions or 0.0)
        },
        "stats": {
            "total_sales": int(wallet.total_sales_count or 0),
            "avg_commission_per_sale": round(float(wallet.total_earned or 0) / int(wallet.total_sales_count or 1), 2) if wallet.total_sales_count else 0.0
        }
    }


# ============ Background Tasks ============
def update_promoter_views(db: Session, promoter_uid: str):
    """Update view count in promoter wallet"""
    try:
        wallet = db.query(PromoterWallet).filter(
            PromoterWallet.user_id == promoter_uid
        ).first()
        
        if not wallet:
            wallet = PromoterWallet(user_id=promoter_uid)
            db.add(wallet)
        
        # Count total views (stored in-memory, not on model)
        # Views are tracked via ReelView table, not on wallet
        wallet.updated_at = datetime.utcnow()
        db.commit()
    except Exception as e:
        print(f"Error updating promoter views: {e}")
        db.rollback()


def update_promoter_clicks(db: Session, promoter_uid: str):
    """Update click count in promoter wallet"""
    try:
        wallet = db.query(PromoterWallet).filter(
            PromoterWallet.user_id == promoter_uid
        ).first()
        
        if not wallet:
            wallet = PromoterWallet(user_id=promoter_uid)
            db.add(wallet)
        
        # Clicks and conversions are tracked via AffiliateClick table
        # Update wallet timestamp only
        wallet.updated_at = datetime.utcnow()
        db.commit()
    except Exception as e:
        print(f"Error updating promoter clicks: {e}")
        db.rollback()


def calculate_and_create_commission(
    db: Session,
    order: Order,
    promoter_uid: str,
    product_id: str
):
    """
    Calculate commission for order and create Commission record
    Commission = product_price * commission_rate (from product settings)
    """
    try:
        # Find order items matching the promoted product
        for item in order.items:
            if item.product_id == product_id:
                # Look up commission rate from marketplace product (default 5%)
                commission_rate = 0.05
                try:
                    marketplace_product = db.query(MarketplaceProduct).filter(
                        MarketplaceProduct.id == item.product_id
                    ).first()
                    if marketplace_product and marketplace_product.commission_rate:
                        commission_rate = float(marketplace_product.commission_rate) / 100.0
                except Exception:
                    pass  # Use default rate if product lookup fails
                
                # Calculate commission
                commission_amount = item.price * item.quantity * commission_rate
                
                # Create commission record
                commission = Commission(
                    user_uid=promoter_uid,
                    order_id=order.id,
                    order_item_id=item.id,
                    product_id=item.product_id,
                    product_name=item.product_name,
                    product_price=item.price,
                    commission_rate=commission_rate,
                    commission_amount=commission_amount,
                    status="pending",  # Pending until order is completed
                    created_at=datetime.utcnow()
                )
                db.add(commission)
                
                # Update promoter wallet
                wallet = db.query(PromoterWallet).filter(
                    PromoterWallet.user_id == promoter_uid
                ).first()
                
                if not wallet:
                    wallet = PromoterWallet(user_id=promoter_uid)
                    db.add(wallet)
                
                wallet.total_earned += commission_amount
                wallet.pending_amount += commission_amount
                wallet.total_sales_count += 1
                wallet.updated_at = datetime.utcnow()
        
        db.commit()
        print(f"✅ Commission created for promoter {promoter_uid}: ${commission_amount}")
    
    except Exception as e:
        print(f"Error calculating commission: {e}")
        db.rollback()
