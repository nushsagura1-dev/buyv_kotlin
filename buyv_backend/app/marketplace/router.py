"""
Router API pour le Marketplace.
"""
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from typing import List, Optional
from uuid import UUID
from decimal import Decimal

from app.database import get_db
from app.auth import get_current_user, require_admin_role, get_current_user_optional
from app.models import User, Post, PostLike, PostBookmark
from app.marketplace.service import MarketplaceService
from app.marketplace.cj_service import CJAuthError
from app.marketplace.schemas import (
    ProductResponse, ProductListResponse, ProductCreate, ProductUpdate,
    CategoryResponse, CategoryCreate, CategoryUpdate,
    PromotionCreate, PromotionResponse,
    AffiliateSaleResponse, AffiliateSaleCreate, SaleStatusUpdate,
    WalletResponse, WalletTransactionResponse,
    WithdrawalRequest, WithdrawalResponse, WithdrawalProcessRequest,
    PromoterDashboard, CJProductSearch, CJProductImport
)

router = APIRouter(prefix="/api/v1", tags=["marketplace"])


def _enrich_with_like_bookmark_status(products, db: Session, current_user):
    """Inject is_liked / is_bookmarked onto marketplace product instances."""
    if not current_user or not products:
        return products
    post_uids = [p.post_uid for p in products if p.post_uid]
    if not post_uids:
        return products
    posts = db.query(Post).filter(Post.uid.in_(post_uids)).all()
    uid_to_int_id = {p.uid: p.id for p in posts}
    int_ids = list(uid_to_int_id.values())
    if int_ids:
        liked_ids = {
            r.post_id for r in db.query(PostLike).filter(
                PostLike.post_id.in_(int_ids),
                PostLike.user_id == current_user.id
            ).all()
        }
        bookmarked_ids = {
            r.post_id for r in db.query(PostBookmark).filter(
                PostBookmark.post_id.in_(int_ids),
                PostBookmark.user_id == current_user.id
            ).all()
        }
    else:
        liked_ids = set()
        bookmarked_ids = set()
    for product in products:
        int_id = uid_to_int_id.get(product.post_uid) if product.post_uid else None
        product.is_liked = int_id in liked_ids if int_id is not None else False
        product.is_bookmarked = int_id in bookmarked_ids if int_id is not None else False
    return products


# ============================================
# PRODUCTS - Public
# ============================================

@router.get("/marketplace/products", response_model=ProductListResponse)
def get_products(
    category: Optional[UUID] = None,
    min_price: Optional[float] = None,
    max_price: Optional[float] = None,
    min_commission: Optional[float] = None,
    search: Optional[str] = None,
    sort_by: str = Query("relevance", regex="^(relevance|price_asc|price_desc|commission|rating|sales|recent|popular)$"),
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    """Liste des produits avec filtres."""
    service = MarketplaceService(db)
    result = service.get_products(
        category_id=category,
        min_price=min_price,
        max_price=max_price,
        min_commission=min_commission,
        search=search,
        sort_by=sort_by,
        page=page,
        limit=limit
    )
    _enrich_with_like_bookmark_status(result["items"], db, current_user)
    return result


@router.get("/marketplace/products/featured", response_model=List[ProductResponse])
def get_featured_products(
    limit: int = Query(10, ge=1, le=50),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    """Produits mis en avant."""
    service = MarketplaceService(db)
    products = service.get_featured_products(limit)
    _enrich_with_like_bookmark_status(products, db, current_user)
    return products


@router.get("/marketplace/products/{product_id}", response_model=ProductResponse)
def get_product(
    product_id: UUID,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    """Détails d'un produit."""
    service = MarketplaceService(db)
    product = service.get_product(product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    _enrich_with_like_bookmark_status([product], db, current_user)
    return product


@router.get("/marketplace/categories", response_model=List[CategoryResponse])
def get_categories(
    parent_id: Optional[UUID] = None,
    db: Session = Depends(get_db)
):
    """Liste des catégories."""
    service = MarketplaceService(db)
    return service.get_categories(parent_id)


# ============================================
# PRODUCTS - Admin
# ============================================

@router.post("/admin/marketplace/products", response_model=ProductResponse)
def create_product(
    product: ProductCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Créer un produit (Admin)."""
    service = MarketplaceService(db)
    return service.create_product(product)


@router.put("/admin/marketplace/products/{product_id}", response_model=ProductResponse)
def update_product(
    product_id: UUID,
    product: ProductUpdate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Modifier un produit (Admin)."""
    service = MarketplaceService(db)
    updated = service.update_product(product_id, product)
    if not updated:
        raise HTTPException(status_code=404, detail="Product not found")
    return updated


@router.delete("/admin/marketplace/products/{product_id}")
def delete_product(
    product_id: UUID,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Supprimer un produit (Admin - Super Admin only)."""
    service = MarketplaceService(db)
    if not service.delete_product(product_id):
        raise HTTPException(status_code=404, detail="Product not found")
    return {"message": "Product deleted"}


@router.patch("/admin/marketplace/products/{product_id}/commission")
def update_product_commission(
    product_id: UUID,
    commission_rate: Decimal,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Modifier le taux de commission (Admin - Finance permissions required)."""
    service = MarketplaceService(db)
    updated = service.update_product(product_id, ProductUpdate(commission_rate=commission_rate))
    if not updated:
        raise HTTPException(status_code=404, detail="Product not found")
    return updated


@router.post("/admin/marketplace/products/{product_id}/feature")
def feature_product(
    product_id: UUID,
    featured: bool = True,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Mettre en avant un produit (Admin)."""
    service = MarketplaceService(db)
    updated = service.update_product(product_id, ProductUpdate(is_featured=featured))
    if not updated:
        raise HTTPException(status_code=404, detail="Product not found")
    return updated


@router.post("/admin/marketplace/categories", response_model=CategoryResponse)
def create_category(
    category: CategoryCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Créer une catégorie (Admin)."""
    service = MarketplaceService(db)
    return service.create_category(category)


@router.put("/admin/marketplace/categories/{category_id}", response_model=CategoryResponse)
def update_category(
    category_id: UUID,
    category: CategoryUpdate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Modifier une catégorie (Admin)."""
    from app.marketplace.models import ProductCategory
    db_category = db.query(ProductCategory).filter(ProductCategory.id == category_id).first()
    if not db_category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    update_data = category.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_category, key, value)
    
    db.commit()
    db.refresh(db_category)
    return db_category


@router.delete("/admin/marketplace/categories/{category_id}")
def delete_category(
    category_id: UUID,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Supprimer une catégorie (Admin)."""
    from app.marketplace.models import ProductCategory
    db_category = db.query(ProductCategory).filter(ProductCategory.id == category_id).first()
    if not db_category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # Check if category has products
    if db_category.products:
        raise HTTPException(
            status_code=400,
            detail="Cannot delete category with associated products. Reassign products first."
        )
    
    db.delete(db_category)
    db.commit()
    return {"message": "Category deleted"}


# ============================================
# CJ DROPSHIPPING - Admin
# ============================================

@router.get("/admin/cj/search")
async def search_cj_products(
    query: str,
    category: Optional[str] = None,
    page: int = 1,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Rechercher produits sur CJ (Admin)."""
    service = MarketplaceService(db)
    try:
        return await service.search_cj_products(query, category, page)
    except CJAuthError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/admin/cj/import", response_model=ProductResponse)
async def import_cj_product(
    import_data: CJProductImport,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Importer un produit de CJ (Admin)."""
    service = MarketplaceService(db)
    try:
        return await service.import_cj_product(
            import_data.cj_product_id,
            import_data.commission_rate,
            import_data.category_id,
            import_data.custom_description,
            import_data.selling_price
        )
    except CJAuthError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/admin/cj/sync/{product_id}")
async def sync_product_with_cj(
    product_id: UUID,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Synchroniser prix/stock avec CJ (Admin)."""
    service = MarketplaceService(db)
    success = await service.sync_product_with_cj(product_id)
    if not success:
        raise HTTPException(status_code=400, detail="Failed to sync product")
    return {"message": "Product synced successfully"}


# ============================================
# PROMOTIONS
# ============================================

@router.post("/promotions", response_model=PromotionResponse)
def create_promotion(
    promotion: PromotionCreate,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Créer une promotion (lier vidéo à produit)."""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else current_user.get("uid", str(current_user.id))
    return service.create_promotion(promotion, uid)


@router.get("/promotions/post/{post_id}", response_model=PromotionResponse)
def get_promotion_by_post(
    post_id: str,
    db: Session = Depends(get_db)
):
    """Obtenir le produit lié à un post."""
    service = MarketplaceService(db)
    promotion = service.get_promotion_by_post(post_id)
    if not promotion:
        raise HTTPException(status_code=404, detail="Promotion not found")
    return promotion


@router.get("/promotions/product/{product_id}", response_model=List[PromotionResponse])
def get_promotions_by_product(
    product_id: UUID,
    db: Session = Depends(get_db)
):
    """Toutes les promotions d'un produit."""
    service = MarketplaceService(db)
    return service.get_promotions_by_product(product_id)


@router.get("/promotions/user/{user_id}", response_model=List[PromotionResponse])
def get_user_promotions(
    user_id: str,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Promotions d'un utilisateur."""
    # Vérifier que l'utilisateur demande ses propres promotions ou est admin
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    is_admin = current_user.is_admin if hasattr(current_user, 'is_admin') else False
    if user_id != uid and not is_admin:
        raise HTTPException(status_code=403, detail="Access denied")
    
    service = MarketplaceService(db)
    return service.get_user_promotions(user_id)


@router.delete("/promotions/{promotion_id}", status_code=204)
def delete_promotion(
    promotion_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Supprimer une promotion (dé-lier une vidéo d'un produit)."""
    from app.marketplace.models import ProductPromotion
    promotion = db.query(ProductPromotion).filter(ProductPromotion.id == promotion_id).first()
    if not promotion:
        raise HTTPException(status_code=404, detail="Promotion not found")
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    is_admin = current_user.is_admin if hasattr(current_user, 'is_admin') else False
    if promotion.promoter_uid != uid and not is_admin:
        raise HTTPException(status_code=403, detail="Access denied")
    db.delete(promotion)
    db.commit()


# ============================================
# AFFILIATE SALES
# ============================================

@router.get("/affiliates/sales", response_model=List[AffiliateSaleResponse])
def get_my_sales(
    status: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Mes ventes affiliées."""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    return service.get_promoter_sales(uid, status)


@router.get("/admin/sales", response_model=List[AffiliateSaleResponse])
def get_all_affiliate_sales(
    status: Optional[str] = None,
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Liste toutes les ventes affiliées (Admin)."""
    from app.marketplace.models import AffiliateSale
    query = db.query(AffiliateSale)
    
    if status:
        query = query.filter(AffiliateSale.commission_status == status)
    
    total = query.count()
    sales = query.order_by(AffiliateSale.created_at.desc()).offset(offset).limit(limit).all()
    return sales


@router.patch("/admin/sales/{sale_id}/status", response_model=AffiliateSaleResponse)
def update_sale_status(
    sale_id: UUID,
    status_update: SaleStatusUpdate,
    db: Session = Depends(get_db),
    current_user = Depends(require_admin_role)
):
    """Changer statut d'une vente (Admin)."""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    
    if status_update.status == "approved":
        sale = service.approve_sale_commission(sale_id, uid)
    elif status_update.status == "paid":
        sale = service.mark_sale_paid(
            sale_id,
            status_update.payment_reference or "",
            "bank_transfer",
            status_update.payment_notes
        )
    else:
        raise HTTPException(status_code=400, detail="Invalid status")
    
    if not sale:
        raise HTTPException(status_code=404, detail="Sale not found")
    return sale


# ============================================
# WALLETS
# ============================================

@router.get("/wallet", response_model=WalletResponse)
def get_my_wallet(
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Mon portefeuille."""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    return service.get_wallet(uid)


@router.get("/wallet/transactions", response_model=List[WalletTransactionResponse])
def get_wallet_transactions(
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Historique des transactions."""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    return service.get_wallet_transactions(uid, limit)


# ============================================
# WITHDRAWALS
# ============================================

# ============================================
# WITHDRAWALS
# NOTE: These endpoints duplicate /api/marketplace/withdrawal/* (withdrawal.py).
# Both use the same WithdrawalRequest model from app/models.py.
# For new clients, prefer the /api/marketplace/withdrawal/* endpoints.
# These are kept for backward compatibility with existing mobile clients.
# ============================================

@router.post("/wallet/withdraw", response_model=WithdrawalResponse, deprecated=True)
def request_withdrawal(
    withdrawal: WithdrawalRequest,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Demander un retrait. [DEPRECATED: use POST /api/marketplace/withdrawal/request]"""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    try:
        return service.create_withdrawal_request(uid, withdrawal)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/wallet/withdrawals", response_model=List[WithdrawalResponse], deprecated=True)
def get_my_withdrawals(
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Mes demandes de retrait. [DEPRECATED: use GET /api/marketplace/withdrawal/history]"""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    return service.get_user_withdrawals(uid)


@router.get("/admin/withdrawals", response_model=List[WithdrawalResponse], deprecated=True)
def get_all_withdrawals(
    status: Optional[str] = None,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin"))
):
    """Toutes les demandes de retrait. [DEPRECATED: use GET /api/marketplace/withdrawal/admin/list]"""
    from app.marketplace.models import WithdrawalRequest as WithdrawalModel
    query = db.query(WithdrawalModel)
    
    if status:
        query = query.filter(WithdrawalModel.status == status)
    
    return query.order_by(WithdrawalModel.created_at.desc()).all()


@router.patch("/admin/withdrawals/{withdrawal_id}", response_model=WithdrawalResponse)
@router.patch("/admin/withdrawals/{withdrawal_id}", response_model=WithdrawalResponse, deprecated=True)
def process_withdrawal(
    withdrawal_id: UUID,
    process_data: WithdrawalProcessRequest,
    db: Session = Depends(get_db),
    current_user = Depends(require_admin_role)
):
    """Traiter une demande de retrait. [DEPRECATED: use /api/marketplace/withdrawal/admin/{id}/approve|reject|complete]"""
    service = MarketplaceService(db)
    uid = current_user.uid if hasattr(current_user, 'uid') else str(current_user.id)
    withdrawal = service.process_withdrawal(
        withdrawal_id,
        uid,
        process_data.status,
        process_data.payment_reference,
        process_data.rejection_reason
    )
    
    if not withdrawal:
        raise HTTPException(status_code=404, detail="Withdrawal not found")
    return withdrawal
