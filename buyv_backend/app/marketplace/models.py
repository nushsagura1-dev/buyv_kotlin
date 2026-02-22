"""
Modèles SQLAlchemy pour le Marketplace.
"""
from sqlalchemy import Column, String, Integer, Float, Boolean, DateTime, Text, ForeignKey, DECIMAL, ARRAY
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import uuid
from app.database import Base


class ProductCategory(Base):
    """Catégories de produits."""
    __tablename__ = "product_categories"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(100), nullable=False)
    name_ar = Column(String(100))  # Nom en arabe
    slug = Column(String(100), unique=True, nullable=False)
    icon_url = Column(String(500))
    parent_id = Column(UUID(as_uuid=True), ForeignKey('product_categories.id'))
    display_order = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relations
    parent = relationship("ProductCategory", remote_side=[id])
    products = relationship("MarketplaceProduct", back_populates="category")
    
    def __str__(self):
        return self.name
    
    def __repr__(self):
        return f"<ProductCategory {self.name}>"


class MarketplaceProduct(Base):
    """Produits du marketplace (importés de CJ Dropshipping)."""
    __tablename__ = "marketplace_products"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # Informations produit
    name = Column(String(255), nullable=False)
    description = Column(Text)
    short_description = Column(String(500))
    
    # Images
    main_image_url = Column(String(500))
    images = Column(JSONB, default=[])  # Liste d'URLs
    thumbnail_url = Column(String(500))
    
    # Prix
    original_price = Column(DECIMAL(10, 2), nullable=False)
    selling_price = Column(DECIMAL(10, 2), nullable=False)
    currency = Column(String(3), default='USD')
    
    # Commission pour affiliés
    commission_rate = Column(DECIMAL(5, 2), nullable=False, default=10.00)
    commission_amount = Column(DECIMAL(10, 2))  # Montant fixe (alternative)
    commission_type = Column(String(20), default='percentage')  # 'percentage' ou 'fixed'
    
    # Catégorie et tags
    category_id = Column(UUID(as_uuid=True), ForeignKey('product_categories.id'))
    tags = Column(JSONB, default=[])
    
    # Source CJ Dropshipping
    cj_product_id = Column(String(100))
    cj_variant_id = Column(String(100))
    cj_product_data = Column(JSONB)  # Données brutes CJ pour référence
    
    # Statistiques
    total_sales = Column(Integer, default=0)
    total_views = Column(Integer, default=0)
    total_promotions = Column(Integer, default=0)  # Nombre de vidéos liées
    average_rating = Column(DECIMAL(3, 2), default=0)
    rating_count = Column(Integer, default=0)
    
    # Statut
    status = Column(String(20), default='active')  # 'active', 'inactive', 'out_of_stock'
    is_featured = Column(Boolean, default=False)
    is_choice = Column(Boolean, default=False)  # Badge "Choice"
    
    # Reel vidéo liée (URL Cloudinary uploadée par le promoteur)
    reel_video_url = Column(String(1000), nullable=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relations
    category = relationship("ProductCategory", back_populates="products")
    promotions = relationship("ProductPromotion", back_populates="product")
    sales = relationship("AffiliateSale", back_populates="product")

    @property
    def promoter_user_id(self) -> str:
        """Retourne l'UID du premier promoteur lié à ce produit."""
        if self.promotions:
            return self.promotions[0].promoter_user_id
        return ""

    @property
    def post_uid(self) -> str:
        """Retourne le post_id de la première promotion liée (pour les commentaires)."""
        if self.promotions:
            return self.promotions[0].post_id
        return ""

    @property
    def post_likes_count(self) -> int:
        """Retourne le nombre de likes du post lié à la première promotion."""
        if not self.promotions:
            return 0
        from sqlalchemy.orm import object_session
        session = object_session(self)
        if session is None:
            return 0
        from ..models import Post as PostModel
        post = session.query(PostModel).filter(PostModel.uid == self.promotions[0].post_id).first()
        return (post.likes_count or 0) if post else 0


class ProductPromotion(Base):
    """Relation entre un post/reel et un produit (promotion)."""
    __tablename__ = "product_promotions"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # Références
    post_id = Column(String(100), nullable=False)  # ID du Reel/Post Firebase
    product_id = Column(UUID(as_uuid=True), ForeignKey('marketplace_products.id'), nullable=False)
    promoter_user_id = Column(String(100), nullable=False)  # Firebase UID
    
    # Statistiques de cette promotion
    views_count = Column(Integer, default=0)
    clicks_count = Column(Integer, default=0)
    sales_count = Column(Integer, default=0)
    total_revenue = Column(DECIMAL(12, 2), default=0)
    total_commission_earned = Column(DECIMAL(12, 2), default=0)
    
    # Métadonnées
    promotion_type = Column(String(20), default='reel')  # 'reel', 'broadcast', 'story'
    is_official = Column(Boolean, default=False)  # Post du compte officiel admin (pas de commission)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relations
    product = relationship("MarketplaceProduct", back_populates="promotions")
    sales = relationship("AffiliateSale", back_populates="promotion")


class AffiliateSale(Base):
    """Ventes générées par les affiliés."""
    __tablename__ = "affiliate_sales"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # Références
    order_id = Column(UUID(as_uuid=True), nullable=False)  # Référence à la commande
    product_id = Column(UUID(as_uuid=True), ForeignKey('marketplace_products.id'), nullable=False)
    promotion_id = Column(UUID(as_uuid=True), ForeignKey('product_promotions.id'))
    
    # Acteurs
    buyer_user_id = Column(String(100), nullable=False)
    promoter_user_id = Column(String(100))  # NULL si achat direct sans promoteur
    
    # Montants
    sale_amount = Column(DECIMAL(12, 2), nullable=False)
    product_price = Column(DECIMAL(10, 2), nullable=False)
    quantity = Column(Integer, default=1)
    
    # Commission
    commission_rate = Column(DECIMAL(5, 2))
    commission_amount = Column(DECIMAL(10, 2), nullable=False)
    commission_status = Column(String(20), default='pending')
    # 'pending', 'approved', 'paid', 'cancelled'
    
    # Paiement commission
    paid_at = Column(DateTime(timezone=True))
    payment_reference = Column(String(100))
    payment_method = Column(String(50))
    payment_notes = Column(Text)  # Notes admin sur le paiement
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relations
    product = relationship("MarketplaceProduct", back_populates="sales")
    promotion = relationship("ProductPromotion", back_populates="sales")
    # wallet_transaction relationship removed — sale_id column not in DB schema

# NOTE: PromoterWallet is now defined in app/models.py to avoid duplication
# Importing it here for backward compatibility
from app.models import PromoterWallet

# class PromoterWallet(Base):
#     """Portefeuille des commissions des promoteurs."""
#     __tablename__ = "promoter_wallets"
#     
#     id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
#     user_id = Column(String(100), unique=True, nullable=False)  # Firebase UID
#     
#     # Soldes
#     total_earned = Column(DECIMAL(12, 2), default=0)
#     pending_amount = Column(DECIMAL(12, 2), default=0)  # En attente de validation
#     available_amount = Column(DECIMAL(12, 2), default=0)  # Disponible pour retrait
#     withdrawn_amount = Column(DECIMAL(12, 2), default=0)  # Total retiré
#     
#     # Informations promoteur
#     promoter_level = Column(String(20), default='standard')  # 'standard', 'silver', 'gold'
#     total_sales_count = Column(Integer, default=0)
#     
#     # Informations bancaires (pour virements)
#     bank_name = Column(String(100))
#     bank_account_number = Column(String(100))
#     bank_account_holder = Column(String(100))
#     bank_swift_code = Column(String(50))
#     
#     # Timestamps
#     created_at = Column(DateTime(timezone=True), server_default=func.now())
#     updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
#     
#     # Relations
#     transactions = relationship("WalletTransaction", back_populates="wallet")
#     withdrawal_requests = relationship("WithdrawalRequest", back_populates="wallet")


class WalletTransaction(Base):
    """Historique des transactions du portefeuille."""
    __tablename__ = "wallet_transactions"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    wallet_id = Column(Integer, ForeignKey('promoter_wallets.id'), nullable=False)
    
    # Transaction
    type = Column(String(20), nullable=False)  # 'commission', 'withdrawal', 'adjustment'
    amount = Column(DECIMAL(12, 2), nullable=False)
    balance_after = Column(DECIMAL(12, 2), nullable=False)
    
    # Référence
    reference_type = Column(String(50))  # 'sale', 'withdrawal_request', 'admin_adjustment'
    reference_id = Column(UUID(as_uuid=True))
    # NOTE: sale_id column removed — not present in actual DB schema
    
    # Description
    description = Column(Text)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())

# NOTE: WithdrawalRequest is now defined in app/models.py to avoid duplication
# Importing it here for backward compatibility
from app.models import WithdrawalRequest

# class WithdrawalRequest(Base):
#     """Demandes de retrait des commissions (hebdomadaire)."""
#     __tablename__ = "withdrawal_requests"
#     
#     id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
#     wallet_id = Column(UUID(as_uuid=True), ForeignKey('promoter_wallets.id'), nullable=False)
#     user_id = Column(String(100), nullable=False)
#     
#     # Montant
#     amount = Column(DECIMAL(12, 2), nullable=False)
#     
#     # Méthode de paiement
#     payment_method = Column(String(50), nullable=False, default='bank_transfer')
#     payment_details = Column(JSONB)  # Détails bancaires
#     
#     # Statut
#     status = Column(String(20), default='pending')  # 'pending', 'processing', 'completed', 'rejected'
#     processed_by = Column(String(100))  # Admin qui a traité
#     processed_at = Column(DateTime(timezone=True))
#     rejection_reason = Column(Text)
#     
#     # Période de la semaine
#     week_start_date = Column(DateTime(timezone=True))
#     week_end_date = Column(DateTime(timezone=True))
#     
#     # Timestamps
#     created_at = Column(DateTime(timezone=True), server_default=func.now())
#     updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
#     
#     # Relation
#     wallet = relationship("PromoterWallet", back_populates="withdrawal_requests")
