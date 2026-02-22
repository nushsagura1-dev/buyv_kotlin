"""
Schemas Pydantic pour le Marketplace.
"""
from pydantic import BaseModel, Field, validator
from typing import Optional, List, Dict, Any
from datetime import datetime
from decimal import Decimal
from uuid import UUID
import json


# ============================================
# PRODUCT CATEGORIES
# ============================================

class CategoryBase(BaseModel):
    name: str
    name_ar: Optional[str] = None
    slug: str
    icon_url: Optional[str] = None
    parent_id: Optional[UUID] = None
    display_order: int = 0
    is_active: bool = True


class CategoryCreate(CategoryBase):
    pass


class CategoryUpdate(BaseModel):
    name: Optional[str] = None
    name_ar: Optional[str] = None
    icon_url: Optional[str] = None
    display_order: Optional[int] = None
    is_active: Optional[bool] = None


class CategoryResponse(CategoryBase):
    id: UUID
    created_at: datetime
    
    class Config:
        from_attributes = True


# ============================================
# MARKETPLACE PRODUCTS
# ============================================

class ProductBase(BaseModel):
    name: str
    description: Optional[str] = None
    short_description: Optional[str] = None
    main_image_url: Optional[str] = None
    images: List[str] = []
    thumbnail_url: Optional[str] = None
    original_price: Decimal
    selling_price: Decimal
    currency: str = "USD"
    commission_rate: Decimal = Decimal("10.00")
    commission_amount: Optional[Decimal] = None
    commission_type: str = "percentage"
    category_id: Optional[UUID] = None
    tags: List[str] = []
    status: str = "active"
    is_featured: bool = False
    is_choice: bool = False


class ProductCreate(ProductBase):
    cj_product_id: Optional[str] = None
    cj_variant_id: Optional[str] = None
    cj_product_data: Optional[Dict[str, Any]] = None


class ProductUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    short_description: Optional[str] = None
    main_image_url: Optional[str] = None
    images: Optional[List[str]] = None
    original_price: Optional[Decimal] = None
    selling_price: Optional[Decimal] = None
    commission_rate: Optional[Decimal] = None
    commission_amount: Optional[Decimal] = None
    commission_type: Optional[str] = None
    category_id: Optional[UUID] = None
    tags: Optional[List[str]] = None
    status: Optional[str] = None
    is_featured: Optional[bool] = None
    is_choice: Optional[bool] = None


class ProductResponse(ProductBase):
    id: UUID
    cj_product_id: Optional[str] = None
    total_sales: int = 0
    total_views: int = 0
    total_promotions: int = 0
    average_rating: Decimal = Decimal("0")
    rating_count: int = 0
    created_at: datetime
    updated_at: datetime
    category: Optional[CategoryResponse] = None
    reel_video_url: Optional[str] = None  # URL vidéo Cloudinary uploadée par le promoteur
    promoter_user_id: Optional[str] = None  # UID du promoteur lié (pour le split de commission)
    post_uid: Optional[str] = None  # UID du post lié (pour les commentaires)
    post_likes_count: Optional[int] = None  # Nombre de likes du post lié
    is_liked: bool = False       # Si l'utilisateur courant a liké le post lié
    is_bookmarked: bool = False  # Si l'utilisateur courant a bookmarké le post lié
    
    # Calcul commission estimée
    estimated_commission: Optional[Decimal] = None
    
    @validator('images', 'tags', pre=True)
    def parse_json_fields(cls, v):
        """Parse JSON strings to lists for images and tags fields."""
        if isinstance(v, str):
            try:
                parsed = json.loads(v)
                return parsed if isinstance(parsed, list) else []
            except (json.JSONDecodeError, TypeError):
                return []
        return v if isinstance(v, list) else []
    
    class Config:
        from_attributes = True
    
    @validator('estimated_commission', always=True)
    def calculate_estimated_commission(cls, v, values):
        if values.get('commission_type') == 'percentage':
            return values.get('selling_price', 0) * (values.get('commission_rate', 0) / 100)
        return values.get('commission_amount', 0)


class ProductListResponse(BaseModel):
    items: List[ProductResponse]
    total: int
    page: int
    limit: int
    total_pages: int


# ============================================
# CJ DROPSHIPPING
# ============================================

class CJProductSearch(BaseModel):
    query: str
    category: Optional[str] = None
    page: int = 1
    page_size: int = 20


class CJProductImport(BaseModel):
    cj_product_id: str
    cj_variant_id: Optional[str] = None
    commission_rate: Decimal = Decimal("10.00")
    category_id: Optional[UUID] = None
    custom_description: Optional[str] = None
    selling_price: Optional[Decimal] = None


class CJProductData(BaseModel):
    """Structure simplifiée des données CJ"""
    product_id: str
    product_name: str
    product_image: str
    sell_price: float
    original_price: Optional[float] = None
    product_url: Optional[str] = None
    category_name: Optional[str] = None
    
    class Config:
        extra = "allow"  # Permet champs additionnels


# ============================================
# PROMOTIONS
# ============================================

class PromotionCreate(BaseModel):
    post_id: str
    product_id: UUID
    promotion_type: str = "reel"  # 'reel', 'broadcast', 'story'
    is_official: bool = False


class PromotionResponse(BaseModel):
    id: UUID
    post_id: str
    product_id: UUID
    promoter_user_id: str
    views_count: int = 0
    clicks_count: int = 0
    sales_count: int = 0
    total_revenue: Decimal = Decimal("0")
    total_commission_earned: Decimal = Decimal("0")
    promotion_type: str
    is_official: bool
    created_at: datetime
    product: Optional[ProductResponse] = None
    
    class Config:
        from_attributes = True


# ============================================
# AFFILIATE SALES
# ============================================

class AffiliateSaleCreate(BaseModel):
    order_id: UUID
    product_id: UUID
    promotion_id: Optional[UUID] = None
    buyer_user_id: str
    promoter_user_id: Optional[str] = None
    sale_amount: Decimal
    product_price: Decimal
    quantity: int = 1
    commission_rate: Decimal
    commission_amount: Decimal


class AffiliateSaleResponse(BaseModel):
    id: UUID
    order_id: UUID
    product_id: UUID
    promotion_id: Optional[UUID] = None
    buyer_user_id: str
    promoter_user_id: Optional[str] = None
    sale_amount: Decimal
    product_price: Decimal
    quantity: int
    commission_rate: Decimal
    commission_amount: Decimal
    commission_status: str
    paid_at: Optional[datetime] = None
    payment_reference: Optional[str] = None
    created_at: datetime
    product: Optional[ProductResponse] = None
    
    class Config:
        from_attributes = True


class SaleStatusUpdate(BaseModel):
    status: str  # 'approved', 'paid', 'cancelled'
    payment_reference: Optional[str] = None
    payment_notes: Optional[str] = None


# ============================================
# WALLETS
# ============================================

class WalletResponse(BaseModel):
    id: str
    user_id: str
    total_earned: Decimal
    pending_amount: Decimal
    available_amount: Decimal
    withdrawn_amount: Decimal
    promoter_level: Optional[str] = None
    total_sales_count: int
    created_at: datetime
    
    @validator('id', pre=True, always=True)
    def coerce_id_to_str(cls, v):
        return str(v)
    
    class Config:
        from_attributes = True


class WalletTransactionResponse(BaseModel):
    id: str
    type: str
    amount: Decimal
    balance_after: Decimal
    description: Optional[str] = None
    created_at: datetime
    
    @validator('id', pre=True, always=True)
    def coerce_id_to_str(cls, v):
        return str(v)
    
    class Config:
        from_attributes = True


# ============================================
# WITHDRAWALS
# ============================================

class WithdrawalRequest(BaseModel):
    amount: Decimal = Field(..., gt=0)
    payment_method: str = "bank_transfer"
    bank_name: Optional[str] = None
    bank_account_number: Optional[str] = None
    bank_account_holder: Optional[str] = None
    bank_swift_code: Optional[str] = None


class WithdrawalResponse(BaseModel):
    id: UUID
    amount: Decimal
    payment_method: str
    status: str
    created_at: datetime
    processed_at: Optional[datetime] = None
    rejection_reason: Optional[str] = None
    week_start_date: Optional[datetime] = None
    week_end_date: Optional[datetime] = None
    
    class Config:
        from_attributes = True


class WithdrawalProcessRequest(BaseModel):
    status: str  # 'completed' or 'rejected'
    payment_reference: Optional[str] = None
    rejection_reason: Optional[str] = None


# ============================================
# ANALYTICS & DASHBOARD
# ============================================

class PromoterDashboard(BaseModel):
    wallet: WalletResponse
    pending_sales_count: int
    this_week_earnings: Decimal
    last_week_earnings: Decimal
    top_products: List[Dict[str, Any]]
    recent_sales: List[AffiliateSaleResponse]


class PromoterAnalytics(BaseModel):
    total_views: int
    total_clicks: int
    total_sales: int
    conversion_rate: float
    total_revenue: Decimal
    total_commission: Decimal
    average_commission_per_sale: Decimal
    sales_by_product: List[Dict[str, Any]]
    earnings_by_period: List[Dict[str, Any]]
