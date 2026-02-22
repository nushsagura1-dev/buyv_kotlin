"""
Admin Dashboard API Endpoints
Provides statistics and management endpoints for mobile admin panel
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from datetime import datetime, timedelta
from typing import List, Optional
from pydantic import BaseModel
import json as _json

from .database import get_db
from .models import (
    User, Post, Order, OrderItem, Commission, PromoterWallet,
    Follow, PostLike, Comment, Notification, WithdrawalRequest
)
from .auth import require_admin_role

router = APIRouter(prefix="/api/admin", tags=["Admin Dashboard"])


# ============ Response Schemas ============

class DashboardStatsResponse(BaseModel):
    # Users stats
    total_users: int
    verified_users: int
    new_users_today: int
    new_users_this_week: int
    
    # Content stats
    total_posts: int
    total_reels: int
    total_products: int
    total_comments: int
    total_likes: int
    
    # Social stats
    total_follows: int
    
    # Commerce stats
    total_orders: int
    pending_orders: int
    total_commissions: int
    pending_commissions: int
    total_revenue: float
    
    # Withdrawal stats
    pending_withdrawals: int
    pending_withdrawals_amount: float


class RecentUserResponse(BaseModel):
    id: str
    username: str
    email: str
    display_name: str
    is_verified: bool
    created_at: datetime
    
    class Config:
        from_attributes = True


class RecentOrderResponse(BaseModel):
    id: int
    buyer_email: str
    total_amount: float
    status: str
    created_at: datetime
    
    class Config:
        from_attributes = True


class UserManagementListResponse(BaseModel):
    id: str
    username: str
    email: str
    display_name: str
    is_verified: bool
    followers_count: int
    following_count: int
    reels_count: int
    created_at: datetime
    
    class Config:
        from_attributes = True


class UserActionRequest(BaseModel):
    user_ids: List[str]


# ============ Dashboard Stats Endpoint ============

@router.get("/dashboard/stats", response_model=DashboardStatsResponse)
def get_dashboard_stats(
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """
    Get comprehensive dashboard statistics for admin panel
    All admin roles can access this endpoint
    """
    # Calculate date ranges
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    week_start = today_start - timedelta(days=7)
    
    # Users stats
    total_users = db.query(func.count(User.id)).scalar()
    verified_users = db.query(func.count(User.id)).filter(User.is_verified == True).scalar()
    new_users_today = db.query(func.count(User.id)).filter(User.created_at >= today_start).scalar()
    new_users_this_week = db.query(func.count(User.id)).filter(User.created_at >= week_start).scalar()
    
    # Content stats
    total_posts = db.query(func.count(Post.id)).scalar()
    total_reels = db.query(func.count(Post.id)).filter(Post.type == 'reel').scalar()
    total_products = db.query(func.count(Post.id)).filter(Post.type == 'product').scalar()
    total_comments = db.query(func.count(Comment.id)).scalar()
    total_likes = db.query(func.count(PostLike.id)).scalar()
    
    # Social stats
    total_follows = db.query(func.count(Follow.id)).scalar()
    
    # Commerce stats (only for super_admin and finance)
    if admin.role in ["super_admin", "finance"]:
        total_orders = db.query(func.count(Order.id)).scalar()
        pending_orders = db.query(func.count(Order.id)).filter(Order.status == 'pending').scalar()
        total_commissions = db.query(func.count(Commission.id)).scalar()
        pending_commissions = db.query(func.count(Commission.id)).filter(Commission.status == 'pending').scalar()
        
        # Calculate revenue (sum of paid commissions)
        paid_commissions = db.query(Commission).filter(Commission.status == 'paid').all()
        total_revenue = sum(c.commission_amount for c in paid_commissions if c.commission_amount)
        
        # Withdrawal stats
        pending_withdrawals = db.query(func.count(WithdrawalRequest.id)).filter(
            WithdrawalRequest.status == 'pending'
        ).scalar()
        
        pending_withdrawals_sum = db.query(func.sum(WithdrawalRequest.amount)).filter(
            WithdrawalRequest.status == 'pending'
        ).scalar()
        pending_withdrawals_amount = float(pending_withdrawals_sum or 0)
    else:
        # Moderators don't see financial data
        total_orders = 0
        pending_orders = 0
        total_commissions = 0
        pending_commissions = 0
        total_revenue = 0.0
        pending_withdrawals = 0
        pending_withdrawals_amount = 0.0
    
    return DashboardStatsResponse(
        total_users=total_users,
        verified_users=verified_users,
        new_users_today=new_users_today,
        new_users_this_week=new_users_this_week,
        total_posts=total_posts,
        total_reels=total_reels,
        total_products=total_products,
        total_comments=total_comments,
        total_likes=total_likes,
        total_follows=total_follows,
        total_orders=total_orders,
        pending_orders=pending_orders,
        total_commissions=total_commissions,
        pending_commissions=pending_commissions,
        total_revenue=total_revenue,
        pending_withdrawals=pending_withdrawals,
        pending_withdrawals_amount=pending_withdrawals_amount
    )


# ============ Recent Activity Endpoints ============

@router.get("/dashboard/recent-users", response_model=List[RecentUserResponse])
def get_recent_users(
    limit: int = 10,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Get recently registered users"""
    users = db.query(User).order_by(desc(User.created_at)).limit(limit).all()
    return [
        RecentUserResponse(
            id=user.uid,
            username=user.username,
            email=user.email,
            display_name=user.display_name,
            is_verified=user.is_verified,
            created_at=user.created_at
        )
        for user in users
    ]


@router.get("/dashboard/recent-orders", response_model=List[RecentOrderResponse])
def get_recent_orders(
    limit: int = 10,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Get recent orders (finance and super_admin only)"""
    orders = db.query(Order).order_by(desc(Order.created_at)).limit(limit).all()
    return [
        RecentOrderResponse(
            id=order.id,
            buyer_email=order.user.email if order.user else "N/A",
            total_amount=order.total or 0,
            status=order.status,
            created_at=order.created_at
        )
        for order in orders
    ]


# ============ User Management Endpoints ============

@router.get("/users", response_model=List[UserManagementListResponse])
def list_users(
    search: Optional[str] = None,
    is_verified: Optional[bool] = None,
    limit: int = 50,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """List users with optional filters"""
    query = db.query(User)
    
    # Apply filters
    if search:
        search_pattern = f"%{search}%"
        query = query.filter(
            (User.username.ilike(search_pattern)) |
            (User.email.ilike(search_pattern)) |
            (User.display_name.ilike(search_pattern))
        )
    
    if is_verified is not None:
        query = query.filter(User.is_verified == is_verified)
    
    users = query.order_by(desc(User.created_at)).limit(limit).offset(offset).all()
    
    return [
        UserManagementListResponse(
            id=user.uid,
            username=user.username,
            email=user.email,
            display_name=user.display_name,
            is_verified=user.is_verified,
            followers_count=user.followers_count,
            following_count=user.following_count,
            reels_count=user.reels_count,
            created_at=user.created_at
        )
        for user in users
    ]


@router.post("/users/verify")
def verify_users(
    request: UserActionRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Verify multiple users"""
    count = 0
    for uid in request.user_ids:
        user = db.query(User).filter(User.uid == uid).first()
        if user and not user.is_verified:
            user.is_verified = True
            count += 1
    
    db.commit()
    return {"message": f"Successfully verified {count} users", "count": count}


@router.post("/users/unverify")
def unverify_users(
    request: UserActionRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Unverify multiple users"""
    count = 0
    for uid in request.user_ids:
        user = db.query(User).filter(User.uid == uid).first()
        if user and user.is_verified:
            user.is_verified = False
            count += 1
    
    db.commit()
    return {"message": f"Successfully unverified {count} users", "count": count}


@router.delete("/users/{user_uid}")
def delete_user(
    user_uid: str,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Delete a user (super_admin only)"""
    user = db.query(User).filter(User.uid == user_uid).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Check for related records
    has_orders = db.query(Order).filter(Order.user_id == user.id).count() > 0
    has_commissions = db.query(Commission).filter(Commission.user_id == user.id).count() > 0
    
    if has_orders or has_commissions:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete user with {db.query(Order).filter(Order.user_id == user.id).count()} orders and {db.query(Commission).filter(Commission.user_id == user.id).count()} commissions"
        )
    
    db.delete(user)
    db.commit()
    
    return {"message": f"User {user.username} deleted successfully"}


# ============ Post Management Endpoints ============

class AdminPostResponse(BaseModel):
    id: int
    uid: str
    username: str
    user_uid: str
    type: str
    caption: Optional[str] = None
    media_url: str
    thumbnail_url: Optional[str] = None
    likes_count: int
    comments_count: int
    views_count: int
    is_promoted: bool
    created_at: datetime

    class Config:
        from_attributes = True


@router.get("/posts", response_model=List[AdminPostResponse])
def list_admin_posts(
    search: Optional[str] = None,
    post_type: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """List all posts with filters for admin moderation"""
    query = db.query(Post)

    if search:
        query = query.filter(Post.caption.ilike(f"%{search}%"))

    if post_type:
        query = query.filter(Post.type == post_type)

    posts = query.order_by(desc(Post.created_at)).limit(limit).offset(offset).all()

    return [
        AdminPostResponse(
            id=p.id,
            uid=p.uid,
            username=p.user.username if p.user else "deleted",
            user_uid=p.user.uid if p.user else "",
            type=p.type or "photo",
            caption=p.caption,
            media_url=p.media_url,
            thumbnail_url=p.thumbnail_url,
            likes_count=p.likes_count or 0,
            comments_count=p.comments_count or 0,
            views_count=p.views_count or 0,
            is_promoted=p.is_promoted or False,
            created_at=p.created_at
        )
        for p in posts
    ]


@router.delete("/posts/{post_uid}")
def admin_delete_post(
    post_uid: str,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Delete a post (admin moderation)"""
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")

    db.delete(post)
    db.commit()
    return {"message": f"Post {post_uid} deleted successfully"}


# ============ Comment Management Endpoints ============

class AdminCommentResponse(BaseModel):
    id: int
    username: str
    user_uid: str
    post_uid: str
    content: str
    created_at: datetime

    class Config:
        from_attributes = True


@router.get("/comments", response_model=List[AdminCommentResponse])
def list_admin_comments(
    search: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """List all comments for admin moderation"""
    query = db.query(Comment)

    if search:
        query = query.filter(Comment.content.ilike(f"%{search}%"))

    comments = query.order_by(desc(Comment.created_at)).limit(limit).offset(offset).all()

    return [
        AdminCommentResponse(
            id=c.id,
            username=c.user.username if c.user else "deleted",
            user_uid=c.user.uid if c.user else "",
            post_uid=c.post.uid if c.post else "",
            content=c.content,
            created_at=c.created_at
        )
        for c in comments
    ]


@router.delete("/comments/{comment_id}")
def admin_delete_comment(
    comment_id: int,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Delete a comment (admin moderation)"""
    comment = db.query(Comment).filter(Comment.id == comment_id).first()
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")

    # Decrement the post's comment count
    if comment.post:
        comment.post.comments_count = max(0, (comment.post.comments_count or 0) - 1)

    db.delete(comment)
    db.commit()
    return {"message": f"Comment {comment_id} deleted successfully"}


# ============ Follow Stats Endpoint ============

class FollowStatsResponse(BaseModel):
    total_follows: int
    new_follows_today: int
    new_follows_this_week: int
    top_followed_users: list


@router.get("/follows/stats", response_model=FollowStatsResponse)
def get_follow_stats(
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Get follow relationship statistics"""
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    week_start = today_start - timedelta(days=7)

    total = db.query(func.count(Follow.id)).scalar() or 0
    today = db.query(func.count(Follow.id)).filter(Follow.created_at >= today_start).scalar() or 0
    week = db.query(func.count(Follow.id)).filter(Follow.created_at >= week_start).scalar() or 0

    # Top 10 most followed users
    top_followed = (
        db.query(
            User.uid,
            User.username,
            User.display_name,
            User.followers_count
        )
        .order_by(desc(User.followers_count))
        .limit(10)
        .all()
    )

    return FollowStatsResponse(
        total_follows=total,
        new_follows_today=today,
        new_follows_this_week=week,
        top_followed_users=[
            {
                "uid": u.uid,
                "username": u.username,
                "display_name": u.display_name,
                "followers_count": u.followers_count or 0
            }
            for u in top_followed
        ]
    )


# ============ Notification Management Endpoints ============

class AdminNotificationResponse(BaseModel):
    id: int
    user_uid: str
    username: str
    title: str
    body: str
    type: str
    is_read: bool
    created_at: datetime

    class Config:
        from_attributes = True


class SendNotificationRequest(BaseModel):
    title: str
    body: str
    type: str = "admin_broadcast"
    target_user_uids: Optional[List[str]] = None  # None = broadcast to all


@router.get("/notifications", response_model=List[AdminNotificationResponse])
def list_admin_notifications(
    limit: int = 50,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """List all notifications"""
    notifs = (
        db.query(Notification)
        .order_by(desc(Notification.created_at))
        .limit(limit)
        .offset(offset)
        .all()
    )

    return [
        AdminNotificationResponse(
            id=n.id,
            user_uid=n.user.uid if n.user else "",
            username=n.user.username if n.user else "deleted",
            title=n.title,
            body=n.body,
            type=n.type,
            is_read=n.is_read,
            created_at=n.created_at
        )
        for n in notifs
    ]


@router.post("/notifications/send")
def send_admin_notification(
    request: SendNotificationRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """Send a notification to specific users or broadcast to all"""
    if request.target_user_uids:
        users = db.query(User).filter(User.uid.in_(request.target_user_uids)).all()
    else:
        users = db.query(User).all()

    count = 0
    for user in users:
        notification = Notification(
            user_id=user.id,
            title=request.title,
            body=request.body,
            type=request.type,
            data=None
        )
        db.add(notification)
        count += 1

    db.commit()
    return {"message": f"Notification sent to {count} users", "count": count}


# ============================================================
# Admin Order Management Router  (/api/orders/...)
# ============================================================

orders_admin_router = APIRouter(prefix="/api/orders", tags=["Admin Orders"])


def _map_order_admin(order: Order, db: Session) -> dict:
    """Map an Order to the admin-facing JSON format expected by the Kotlin app."""
    user = db.query(User).filter(User.id == order.user_id).first()
    try:
        addr = _json.loads(order.shipping_address) if order.shipping_address else None
    except Exception:
        addr = None

    shipping = None
    if addr:
        shipping = {
            "name": addr.get("fullName") or addr.get("name") or "",
            "phone": addr.get("phone") or "",
            "address": addr.get("address") or addr.get("street") or "",
            "city": addr.get("city") or "",
            "state": addr.get("state"),
            "country": addr.get("country") or "",
            "postal_code": addr.get("zipCode") or addr.get("postal_code") or "",
        }

    items = [
        {
            "id": str(i.id),
            "product_id": str(i.product_id),
            "product_name": i.product_name or "",
            "product_image": i.product_image,
            "quantity": i.quantity,
            "price": float(i.price or 0),
            "total": round(float(i.price or 0) * int(i.quantity or 1), 2),
        }
        for i in (order.items or [])
    ]

    return {
        "id": str(order.id),
        "order_number": order.order_number or "",
        "user_id": user.uid if user else str(order.user_id),
        "user_name": user.display_name if user else None,
        "user_email": user.email if user else None,
        "items": items,
        "total_amount": float(order.total or 0),
        "status": order.status or "pending",
        "payment_status": order.status or "pending",
        "payment_method": order.payment_method,
        "shipping_address": shipping,
        "tracking_number": order.tracking_number,
        "created_at": order.created_at.isoformat() if order.created_at else "",
        "updated_at": order.updated_at.isoformat() if order.updated_at else "",
    }


@orders_admin_router.get("/admin/all")
def admin_get_all_orders(
    limit: int = 100,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """List all orders — admin only."""
    orders = (
        db.query(Order)
        .order_by(desc(Order.created_at))
        .limit(limit)
        .offset(offset)
        .all()
    )
    return [_map_order_admin(o, db) for o in orders]


@orders_admin_router.get("/admin/status")
def admin_get_orders_by_status(
    status: str,
    limit: int = 100,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """List orders filtered by status — admin only."""
    orders = (
        db.query(Order)
        .filter(Order.status == status)
        .order_by(desc(Order.created_at))
        .limit(limit)
        .offset(offset)
        .all()
    )
    return [_map_order_admin(o, db) for o in orders]


class OrderStatusUpdateRequest(BaseModel):
    status: str
    tracking_number: Optional[str] = None
    notes: Optional[str] = None


@orders_admin_router.patch("/{order_id}/status")
def admin_update_order_status(
    order_id: int,
    payload: OrderStatusUpdateRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """Update order status with commission/wallet side-effects — admin only."""
    order = db.query(Order).filter(Order.id == order_id).first()
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")

    order.status = payload.status
    if payload.tracking_number:
        order.tracking_number = payload.tracking_number
    if payload.notes:
        order.notes = payload.notes
    order.updated_at = datetime.utcnow()
    db.add(order)
    db.commit()

    if payload.status.lower() == "delivered":
        for c in db.query(Commission).filter(Commission.order_id == order.id).all():
            if c.status == "pending":
                c.status = "paid"
                c.paid_at = datetime.utcnow()
                c.updated_at = datetime.utcnow()
                db.add(c)
                if c.user_uid:
                    wallet = db.query(PromoterWallet).filter(
                        PromoterWallet.user_id == c.user_uid
                    ).first()
                    if wallet:
                        amount = float(c.commission_amount or 0)
                        wallet.pending_amount = round(max(0.0, (wallet.pending_amount or 0.0) - amount), 2)
                        wallet.available_amount = round((wallet.available_amount or 0.0) + amount, 2)
                        wallet.total_earned = round((wallet.total_earned or 0.0) + amount, 2)
        db.commit()

    elif payload.status.lower() in ("canceled", "cancelled"):
        for c in db.query(Commission).filter(Commission.order_id == order.id).all():
            if c.status == "pending":
                c.status = "canceled"
                c.updated_at = datetime.utcnow()
                db.add(c)
                if c.user_uid:
                    wallet = db.query(PromoterWallet).filter(
                        PromoterWallet.user_id == c.user_uid
                    ).first()
                    if wallet:
                        amount = float(c.commission_amount or 0)
                        wallet.pending_amount = round(max(0.0, (wallet.pending_amount or 0.0) - amount), 2)
                        wallet.total_sales_count = max(0, (wallet.total_sales_count or 0) - 1)
        db.commit()

    db.refresh(order)
    return {"status": "ok", "order_id": order_id, "new_status": payload.status}


# ============================================================
# Admin Commission Management Router  (/api/commissions/...)
# ============================================================

commissions_admin_router = APIRouter(prefix="/api/commissions", tags=["Admin Commissions"])


def _map_commission_admin(c: Commission, db: Session) -> dict:
    """Map a Commission to the Kotlin Commission domain model format."""
    uid = None
    if c.user_id:
        u = db.query(User).filter(User.id == c.user_id).first()
        uid = u.uid if u else None
    if not uid:
        uid = c.user_uid or ""

    try:
        metadata = _json.loads(c.metadata_json) if c.metadata_json else None
    except Exception:
        metadata = None

    return {
        "id": c.id,
        "userId": uid,
        "orderId": str(c.order_id) if c.order_id is not None else "",
        "orderItemId": str(c.order_item_id) if c.order_item_id is not None else None,
        "productId": c.product_id or "",
        "productName": c.product_name or "",
        "productPrice": float(c.product_price or 0.0),
        "commissionRate": float(c.commission_rate or 0.0),
        "commissionAmount": float(c.commission_amount or 0.0),
        "status": c.status or "pending",
        "createdAt": c.created_at.isoformat() if c.created_at else "",
        "updatedAt": c.updated_at.isoformat() if c.updated_at else "",
        "paidAt": c.paid_at.isoformat() if c.paid_at else None,
        "metadata": metadata,
    }


@commissions_admin_router.get("/admin/all")
def admin_get_all_commissions(
    limit: int = 100,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """List all commissions — admin only."""
    rows = (
        db.query(Commission)
        .order_by(desc(Commission.created_at))
        .limit(limit)
        .offset(offset)
        .all()
    )
    return [_map_commission_admin(r, db) for r in rows]


@commissions_admin_router.get("/admin/status")
def admin_get_commissions_by_status(
    status: str,
    limit: int = 100,
    offset: int = 0,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """List commissions by status — admin only."""
    rows = (
        db.query(Commission)
        .filter(Commission.status == status)
        .order_by(desc(Commission.created_at))
        .limit(limit)
        .offset(offset)
        .all()
    )
    return [_map_commission_admin(r, db) for r in rows]


class CommissionStatusUpdateRequest(BaseModel):
    status: str  # pending | approved | paid | canceled


@commissions_admin_router.patch("/{commission_id}/status")
def admin_update_commission_status(
    commission_id: int,
    payload: CommissionStatusUpdateRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db),
):
    """Update a commission status — admin only."""
    c = db.query(Commission).filter(Commission.id == commission_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Commission not found")

    c.status = payload.status
    if payload.status.lower() == "paid":
        c.paid_at = datetime.utcnow()
    c.updated_at = datetime.utcnow()
    db.add(c)
    db.commit()
    return {"status": "ok", "commission_id": commission_id, "new_status": payload.status}
