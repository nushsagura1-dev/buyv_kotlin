from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey, UniqueConstraint, Float
from sqlalchemy.orm import relationship, Mapped, mapped_column
from datetime import datetime
import uuid
from .database import Base

class User(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    uid: Mapped[str] = mapped_column(String(36), unique=True, index=True, default=lambda: str(uuid.uuid4()))
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    username: Mapped[str] = mapped_column(String(100), unique=True, index=True, nullable=False)
    display_name: Mapped[str] = mapped_column(String(150), nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(50), default="user", nullable=False)  # user or admin (promoter is a status, not a role)

    profile_image_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    bio: Mapped[str | None] = mapped_column(Text, nullable=True)
    followers_count: Mapped[int] = mapped_column(Integer, default=0)
    following_count: Mapped[int] = mapped_column(Integer, default=0)
    reels_count: Mapped[int] = mapped_column(Integer, default=0)
    is_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    interests: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON string
    settings: Mapped[str | None] = mapped_column(Text, nullable=True)   # JSON string
    
    # FCM token for push notifications
    fcm_token: Mapped[str | None] = mapped_column(String(512), nullable=True)

    def is_promoter(self, db_session) -> bool:
        """Check if user is a promoter by checking if they have a PromoterWallet"""
        from sqlalchemy import select
        stmt = select(PromoterWallet).where(PromoterWallet.user_id == self.id)
        return db_session.execute(stmt).first() is not None

class Notification(Base):
    __tablename__ = "notifications"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    type: Mapped[str] = mapped_column(String(100), nullable=False)
    data: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON string
    is_read: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user = relationship("User")

class Follow(Base):
    __tablename__ = "follows"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    follower_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    followed_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint('follower_id', 'followed_id', name='uq_follow_pair'),
    )

    follower = relationship("User", foreign_keys=[follower_id])
    followed = relationship("User", foreign_keys=[followed_id])


class Order(Base):
    __tablename__ = "orders"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    order_number: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    status: Mapped[str] = mapped_column(String(50), default="pending")
    subtotal: Mapped[float] = mapped_column(Float, default=0.0)
    shipping: Mapped[float] = mapped_column(Float, default=0.0)
    tax: Mapped[float] = mapped_column(Float, default=0.0)
    total: Mapped[float] = mapped_column(Float, default=0.0)
    shipping_address: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON string
    payment_method: Mapped[str] = mapped_column(String(100))
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    estimated_delivery: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    tracking_number: Mapped[str | None] = mapped_column(String(100), nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    promoter_uid: Mapped[str | None] = mapped_column(String(36), nullable=True)
    payment_intent_id: Mapped[str | None] = mapped_column(String(200), nullable=True)  # Stripe PaymentIntent ID

    user = relationship("User")
    items = relationship("OrderItem", back_populates="order", cascade="all, delete-orphan")
    commissions = relationship("Commission", back_populates="order")


class OrderItem(Base):
    __tablename__ = "order_items"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    order_id: Mapped[int] = mapped_column(Integer, ForeignKey("orders.id"), nullable=False)
    product_id: Mapped[str] = mapped_column(String(100))
    product_name: Mapped[str] = mapped_column(String(255))
    product_image: Mapped[str] = mapped_column(String(512))
    price: Mapped[float] = mapped_column(Float)
    quantity: Mapped[int] = mapped_column(Integer)
    size: Mapped[str | None] = mapped_column(String(50), nullable=True)
    color: Mapped[str | None] = mapped_column(String(50), nullable=True)
    attributes: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON string
    is_promoted_product: Mapped[bool] = mapped_column(Boolean, default=False)
    promoter_uid: Mapped[str | None] = mapped_column(String(36), nullable=True)

    order = relationship("Order", back_populates="items")
    commissions = relationship("Commission", back_populates="order_item")


class Commission(Base):
    __tablename__ = "commissions"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("users.id"), nullable=True)
    user_uid: Mapped[str | None] = mapped_column(String(36), nullable=True)  # promoter UID
    order_id: Mapped[int] = mapped_column(Integer, ForeignKey("orders.id"), nullable=False)
    order_item_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("order_items.id"), nullable=True)
    product_id: Mapped[str] = mapped_column(String(100))
    product_name: Mapped[str] = mapped_column(String(255))
    product_price: Mapped[float] = mapped_column(Float)
    commission_rate: Mapped[float] = mapped_column(Float, default=0.01)
    commission_amount: Mapped[float] = mapped_column(Float)
    status: Mapped[str] = mapped_column(String(50), default="pending")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    paid_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    metadata_json: Mapped[str | None] = mapped_column(Text, nullable=True)

    order = relationship("Order", back_populates="commissions")
    order_item = relationship("OrderItem", back_populates="commissions")
    user = relationship("User")


class Post(Base):
    __tablename__ = "posts"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    uid: Mapped[str] = mapped_column(String(36), unique=True, index=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    type: Mapped[str] = mapped_column(String(20))  # 'reel' | 'product' | 'photo'
    media_url: Mapped[str] = mapped_column(String(512))
    thumbnail_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    caption: Mapped[str | None] = mapped_column(Text, nullable=True)
    likes_count: Mapped[int] = mapped_column(Integer, default=0)
    comments_count: Mapped[int] = mapped_column(Integer, default=0)
    shares_count: Mapped[int] = mapped_column(Integer, default=0)
    views_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Marketplace/promotion fields
    product_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    marketplace_product_uid: Mapped[str | None] = mapped_column(String(36), nullable=True, index=True)
    is_promoted: Mapped[bool] = mapped_column(Boolean, default=False)

    user = relationship("User")
    likes = relationship("PostLike", back_populates="post", cascade="all, delete-orphan")
    comments = relationship("Comment", back_populates="post", cascade="all, delete-orphan")


class PostLike(Base):
    __tablename__ = "post_likes"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    post_id: Mapped[int] = mapped_column(Integer, ForeignKey("posts.id"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint('post_id', 'user_id', name='uq_post_like'),
    )

    post = relationship("Post", back_populates="likes")
    user = relationship("User")


class PostBookmark(Base):
    __tablename__ = "post_bookmarks"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    post_id: Mapped[int] = mapped_column(Integer, ForeignKey("posts.id"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint('post_id', 'user_id', name='uq_post_bookmark'),
    )

    post = relationship("Post")
    user = relationship("User")


class Comment(Base):
    __tablename__ = "comments"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    post_id: Mapped[int] = mapped_column(Integer, ForeignKey("posts.id"), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    likes_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    user = relationship("User")
    post = relationship("Post", back_populates="comments")
    likes = relationship("CommentLike", back_populates="comment", cascade="all, delete-orphan")


class CommentLike(Base):
    __tablename__ = "comment_likes"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    comment_id: Mapped[int] = mapped_column(Integer, ForeignKey("comments.id"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint('comment_id', 'user_id', name='uq_comment_like'),
    )

    comment = relationship("Comment", back_populates="likes")
    user = relationship("User")


# Phase 6: Tracking & Analytics Models
class AffiliateClick(Base):
    """Track when users click on marketplace product badges in Reels"""
    __tablename__ = "affiliate_clicks"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    
    # Who clicked
    viewer_uid: Mapped[str | None] = mapped_column(String(36), nullable=True, index=True)  # nullable for anonymous
    
    # What was clicked
    reel_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)  # Firebase post ID
    product_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)  # Marketplace product ID
    
    # Who promoted it
    promoter_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)  # Creator who posted Reel
    
    # Tracking details
    session_id: Mapped[str | None] = mapped_column(String(100), nullable=True)  # Track user session
    device_info: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON: device type, OS, app version
    
    # Metadata
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    
    # Conversion tracking (updated when purchase happens)
    converted: Mapped[bool] = mapped_column(Boolean, default=False, index=True)
    converted_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    order_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("orders.id"), nullable=True)


class ReelView(Base):
    """Track Reel impressions for analytics"""
    __tablename__ = "reel_views"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    
    # What was viewed
    reel_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)  # Firebase post ID
    promoter_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)  # Creator UID
    product_id: Mapped[str | None] = mapped_column(String(100), nullable=True, index=True)  # Linked product (if any)
    
    # Who viewed
    viewer_uid: Mapped[str | None] = mapped_column(String(36), nullable=True, index=True)  # nullable for anonymous
    
    # View metrics
    watch_duration: Mapped[int | None] = mapped_column(Integer, nullable=True)  # seconds watched
    completion_rate: Mapped[float | None] = mapped_column(Float, nullable=True)  # 0.0 to 1.0
    
    # Tracking
    session_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    
    # Unique constraint: one view per user per reel per session
    __table_args__ = (
        UniqueConstraint('reel_id', 'viewer_uid', 'session_id', name='uq_reel_view'),
    )


class PromoterWallet(Base):
    """Track promoter earnings and withdrawals"""
    __tablename__ = "promoter_wallets"
    __table_args__ = {'extend_existing': True}
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    # TEMP: user_id stocke l'UID (String) pour correspondre à la DB actuelle
    # TODO: Créer migration Alembic pour changer en Integer FK vers users.id
    user_id: Mapped[str] = mapped_column(String(36), unique=True, nullable=False, index=True)
    
    # Balance tracking
    total_earned: Mapped[float] = mapped_column(Float, default=0.0)  # All-time earnings
    pending_amount: Mapped[float] = mapped_column(Float, default=0.0)  # Pending approval
    available_amount: Mapped[float] = mapped_column(Float, default=0.0)  # Ready to withdraw (previously available_balance)
    withdrawn_amount: Mapped[float] = mapped_column(Float, default=0.0)  # Total withdrawn (previously total_withdrawn)
    
    # Stats
    total_sales_count: Mapped[int] = mapped_column(Integer, default=0)  # Number of sales generated
    promoter_level: Mapped[str | None] = mapped_column(String(50), nullable=True)
    
    # Bank details
    bank_name: Mapped[str | None] = mapped_column(String(100), nullable=True)
    bank_account_number: Mapped[str | None] = mapped_column(String(100), nullable=True)
    bank_account_holder: Mapped[str | None] = mapped_column(String(100), nullable=True)
    bank_swift_code: Mapped[str | None] = mapped_column(String(50), nullable=True)
    
    # Timestamps
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    # TODO: Uncomment when WalletTransaction model is created
    # transactions: Mapped[list["WalletTransaction"]] = relationship("WalletTransaction", back_populates="wallet", lazy="dynamic")


class BlockedUser(Base):
    """Track blocked users"""
    __tablename__ = "blocked_users"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    blocker_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    blocked_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint('blocker_uid', 'blocked_uid', name='uq_block_pair'),
    )


class Report(Base):
    """Track user reports on content and users"""
    __tablename__ = "reports"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    reporter_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    target_type: Mapped[str] = mapped_column(String(20), nullable=False)  # "post", "comment", "user"
    target_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    reason: Mapped[str] = mapped_column(String(50), nullable=False)  # spam, harassment, etc.
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    status: Mapped[str] = mapped_column(String(20), default="pending", index=True)  # pending, reviewed, resolved, dismissed
    admin_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Sound(Base):
    """Sound/music library for Reels"""
    __tablename__ = "sounds"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    uid: Mapped[str] = mapped_column(String(36), unique=True, index=True, default=lambda: str(uuid.uuid4()))
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    artist: Mapped[str] = mapped_column(String(255), nullable=False)
    audio_url: Mapped[str] = mapped_column(String(512), nullable=False)
    cover_image_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    duration: Mapped[float] = mapped_column(Float, default=0.0)  # seconds
    genre: Mapped[str | None] = mapped_column(String(50), nullable=True)
    usage_count: Mapped[int] = mapped_column(Integer, default=0)
    is_featured: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class WithdrawalRequest(Base):
    """Track withdrawal requests from promoters"""
    __tablename__ = "withdrawal_requests"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    wallet_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    user_id: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    
    # Request details
    amount: Mapped[float] = mapped_column(Float, nullable=False)
    payment_method: Mapped[str] = mapped_column(String(50), nullable=False)  # paypal, bank
    payment_details: Mapped[str] = mapped_column(Text, nullable=False)  # JSON: account info
    
    # Status tracking
    status: Mapped[str] = mapped_column(String(50), default="pending", index=True)  # pending, approved, rejected, completed
    rejection_reason: Mapped[str | None] = mapped_column(Text, nullable=True)
    
    # Week tracking
    week_start_date: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    week_end_date: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    
    # Timestamps
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    processed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    
    # Processing
    processed_by: Mapped[str | None] = mapped_column(String(100), nullable=True)  # Admin user who processed


class RevokedToken(Base):
    """Server-side token blacklist for secure logout (H-3 security fix).
    
    When a user logs out, their current access token's JTI (JWT ID) is stored here.
    The token is checked against this table on every authenticated request.
    Expired entries are automatically cleaned up by a background task.
    """
    __tablename__ = "revoked_tokens"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    jti: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)  # JWT ID claim
    user_uid: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    revoked_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    expires_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)  # Auto-cleanup after token expiry