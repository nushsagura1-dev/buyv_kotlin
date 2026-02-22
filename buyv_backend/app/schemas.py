from pydantic import BaseModel, EmailStr, Field, ConfigDict
from typing import Optional, List, Any, Dict
from datetime import datetime

def to_camel(string: str) -> str:
    words = string.split('_')
    return words[0] + ''.join(word.capitalize() for word in words[1:])

class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int

# -------------------- Users --------------------

class UserBase(CamelModel):
    email: EmailStr
    username: str
    display_name: str
    profile_image_url: Optional[str] = None
    bio: Optional[str] = None
    followers_count: int = 0
    following_count: int = 0
    reels_count: int = 0
    is_verified: bool = False
    created_at: datetime
    updated_at: datetime
    interests: List[str] = []
    settings: Optional[dict] = None

class UserCreate(CamelModel):
    email: EmailStr
    password: str
    username: str
    display_name: str

class UserOut(CamelModel):
    id: str = Field(alias="id") # uid from DB map to id
    email: EmailStr
    username: str
    display_name: str
    profile_image_url: Optional[str] = None
    bio: Optional[str] = None
    followers_count: int
    following_count: int
    reels_count: int
    is_verified: bool
    role: str = "user"  # Include role for unified authentication
    created_at: datetime
    updated_at: datetime
    interests: List[str] = []
    settings: Optional[dict] = None

class UserUpdate(CamelModel):
    display_name: Optional[str] = None
    profile_image_url: Optional[str] = None
    bio: Optional[str] = None
    interests: Optional[List[str]] = None
    settings: Optional[dict] = None

class UserStats(CamelModel):
    followers_count: int
    following_count: int
    reels_count: int
    products_count: int
    total_likes: int
    saved_posts_count: int  # Nombre de posts bookmarkés

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

class PasswordResetRequest(BaseModel):
    email: EmailStr

class PasswordResetConfirm(BaseModel):
    token: str
    new_password: str

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserOut
    refresh_token: Optional[str] = None

class NotificationCreate(CamelModel):
    user_id: str
    title: str
    body: str
    type: str
    data: Optional[dict] = None

class NotificationOut(CamelModel):
    id: int
    user_id: str
    title: str
    body: str
    type: str
    data: Optional[dict] = None
    is_read: bool
    created_at: datetime

# -------------------- Orders & Commissions --------------------

class OrderItemCreate(CamelModel):
    id: Optional[str] = None
    product_id: str
    product_name: str
    product_image: str
    price: float
    quantity: int
    size: Optional[str] = None
    color: Optional[str] = None
    attributes: Optional[Dict[str, str]] = None
    is_promoted_product: bool = False
    promoter_id: Optional[str] = None

class OrderItemOut(CamelModel):
    id: int
    product_id: str
    product_name: str
    product_image: str
    price: float
    quantity: int
    size: Optional[str] = None
    color: Optional[str] = None
    attributes: Optional[Dict[str, str]] = None
    is_promoted_product: bool = False
    promoter_uid: Optional[str] = Field(default=None, alias="promoterId")

class Address(CamelModel):
    id: Optional[str] = None
    full_name: str = Field(..., alias="fullName") # Flutter expects fullName
    address: str = Field(..., alias="address")    # Flutter expects address (DB has street/address logic?)
    city: str
    state: str
    zip_code: str
    country: str
    phone: str
    is_default: bool = False

class PaymentInfo(CamelModel):
    method: str
    status: str
    amount: float
    transaction_id: Optional[str] = None

class OrderCreate(CamelModel):
    order_number: Optional[str] = None
    items: List[OrderItemCreate]
    status: str = "pending"
    subtotal: float
    shipping: float
    tax: float
    total: float
    shipping_address: Optional[Address] = None
    payment_method: str
    estimated_delivery: Optional[datetime] = None
    tracking_number: Optional[str] = None
    notes: Optional[str] = ""
    promoter_id: Optional[str] = None
    payment_intent_id: Optional[str] = None  # Stripe PaymentIntent ID for verification

class OrderOut(CamelModel):
    id: int
    user_id: int # Start with int ID, might need UID mapping if Flutter expects string UUID
    order_number: str
    items: List[OrderItemOut]
    status: str
    subtotal: float
    shipping: float
    tax: float
    total: float = Field(..., validation_alias="total_amount", serialization_alias="total")

    shipping_address: Optional[Address]
    
    # Flutter expects paymentInfo object. DB has payment_method string.
    # We will construct paymentInfo in the router or use a property.
    payment_info: PaymentInfo = Field(..., alias="paymentInfo")

    created_at: datetime
    updated_at: datetime
    estimated_delivery: Optional[datetime]
    tracking_number: Optional[str]
    notes: Optional[str] = ""
    promoter_uid: Optional[str] = Field(default=None, alias="promoterId")

class StatusUpdate(CamelModel):
    status: str

class TrackingUpdate(CamelModel):
    tracking_number: str

class CancelOrderRequest(CamelModel):
    reason: Optional[str] = None

class CommissionOut(CamelModel):
    id: int
    user_id: Optional[str] = None
    order_id: int
    order_item_id: Optional[int] = None
    product_id: str
    product_name: str
    product_price: float
    commission_rate: float
    commission_amount: float
    status: str
    created_at: datetime
    updated_at: datetime
    paid_at: Optional[datetime] = None
    metadata: Optional[dict] = None

# -------------------- Posts --------------------

class PostOut(CamelModel):
    id: str = Field(alias="id") # post uid
    user_id: str = Field(alias="userId") # user uid string
    
    # Flattened User Data for Flutter ReelModel
    # These must be populated in the route handler
    username: str 
    display_name: str
    user_profile_image: Optional[str] = None
    is_user_verified: bool = False
    
    type: str  # 'reel' | 'product' | 'photo'
    
    # Aly to videoUrl for Reels
    video_url: str = Field(..., alias="videoUrl", validation_alias="media_url")
    thumbnail_url: Optional[str] = None
    
    caption: Optional[str] = None
    likes_count: int
    
    # ReelModel fields
    comments_count: int = 0
    shares_count: int = 0
    views_count: int = 0
    
    created_at: datetime
    updated_at: datetime
    is_liked: bool = False
    is_bookmarked: bool = False
    
    duration: float = 0.0
    metadata: Optional[dict] = None
    marketplace_product_uid: Optional[str] = None  # Linked marketplace product

class CountResponse(BaseModel):
    count: int

class PostCreate(CamelModel):
    type: str
    media_url: str
    caption: Optional[str] = None
    additional_data: Optional[dict] = None


# -------------------- Comments --------------------

class CommentCreate(CamelModel):
    content: str

class CommentOut(CamelModel):
    id: int
    user_id: str = Field(alias="userId")
    username: str
    display_name: str
    user_profile_image: Optional[str] = None
    post_id: str = Field(alias="postId")
    content: str
    likes_count: int = 0
    is_liked: bool = False
    created_at: datetime
    updated_at: datetime

