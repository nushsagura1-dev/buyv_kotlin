"""
Migration pour créer les tables du Marketplace.

Revision ID: marketplace_tables
Create Date: 2026-02-03
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID, JSONB, DECIMAL

# revision identifiers
revision = 'marketplace_tables'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    # Activer l'extension UUID si pas déjà fait
    op.execute('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"')
    
    # 1. ProductCategory
    op.create_table(
        'product_categories',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('name_ar', sa.String(100)),
        sa.Column('slug', sa.String(120), unique=True, nullable=False, index=True),
        sa.Column('description', sa.Text),
        sa.Column('parent_id', UUID(as_uuid=True), sa.ForeignKey('product_categories.id', ondelete='CASCADE'), nullable=True),
        sa.Column('display_order', sa.Integer, default=0),
        sa.Column('is_active', sa.Boolean, default=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # 2. MarketplaceProduct
    op.create_table(
        'marketplace_products',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('category_id', UUID(as_uuid=True), sa.ForeignKey('product_categories.id', ondelete='SET NULL'), nullable=True, index=True),
        sa.Column('name', sa.String(255), nullable=False),
        sa.Column('description', sa.Text, nullable=False),
        sa.Column('short_description', sa.String(500)),
        sa.Column('slug', sa.String(280), unique=True, nullable=False, index=True),
        sa.Column('sku', sa.String(100), unique=True, index=True),
        sa.Column('status', sa.String(20), default='active', index=True),
        
        # Prix
        sa.Column('original_price', DECIMAL(10, 2), nullable=False),
        sa.Column('selling_price', DECIMAL(10, 2), nullable=False),
        sa.Column('currency', sa.String(3), default='USD'),
        
        # Commission
        sa.Column('commission_rate', DECIMAL(5, 2), nullable=False),
        sa.Column('commission_amount', DECIMAL(10, 2)),
        sa.Column('commission_type', sa.String(20), default='percentage'),
        
        # Images
        sa.Column('main_image', sa.String(500)),
        sa.Column('images', sa.ARRAY(sa.String(500))),
        sa.Column('video_url', sa.String(500)),
        
        # CJ Dropshipping
        sa.Column('cj_product_id', sa.String(100), unique=True, index=True),
        sa.Column('cj_variant_id', sa.String(100)),
        sa.Column('cj_category_id', sa.String(100)),
        sa.Column('cj_data', JSONB),
        
        # Métadonnées
        sa.Column('tags', sa.ARRAY(sa.String(50))),
        sa.Column('metadata', JSONB),
        
        # Statistiques
        sa.Column('total_views', sa.Integer, default=0),
        sa.Column('total_promotions', sa.Integer, default=0),
        sa.Column('total_sales', sa.Integer, default=0),
        sa.Column('average_rating', DECIMAL(3, 2), default=0),
        sa.Column('review_count', sa.Integer, default=0),
        
        # Flags
        sa.Column('is_featured', sa.Boolean, default=False, index=True),
        sa.Column('is_choice', sa.Boolean, default=False),
        sa.Column('is_available_for_promotion', sa.Boolean, default=True),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # Index composites
    op.create_index('idx_product_status_featured', 'marketplace_products', ['status', 'is_featured'])
    op.create_index('idx_product_category_status', 'marketplace_products', ['category_id', 'status'])
    
    # 3. ProductPromotion
    op.create_table(
        'product_promotions',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('post_id', sa.String(50), nullable=False, index=True),
        sa.Column('product_id', UUID(as_uuid=True), sa.ForeignKey('marketplace_products.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('promoter_user_id', sa.String(50), nullable=False, index=True),
        sa.Column('is_official', sa.Boolean, default=False),
        
        # Statistiques
        sa.Column('views_count', sa.Integer, default=0),
        sa.Column('clicks_count', sa.Integer, default=0),
        sa.Column('sales_count', sa.Integer, default=0),
        sa.Column('total_revenue', DECIMAL(10, 2), default=0),
        sa.Column('total_commission_earned', DECIMAL(10, 2), default=0),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # Contrainte unique
    op.create_unique_constraint('uq_post_product', 'product_promotions', ['post_id', 'product_id'])
    
    # 4. AffiliateSale
    op.create_table(
        'affiliate_sales',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('order_id', sa.String(100), unique=True, nullable=False, index=True),
        sa.Column('product_id', UUID(as_uuid=True), sa.ForeignKey('marketplace_products.id', ondelete='RESTRICT'), nullable=False, index=True),
        sa.Column('promotion_id', UUID(as_uuid=True), sa.ForeignKey('product_promotions.id', ondelete='SET NULL'), nullable=True, index=True),
        sa.Column('promoter_user_id', sa.String(50), nullable=False, index=True),
        sa.Column('buyer_user_id', sa.String(50), nullable=False, index=True),
        
        # Montants
        sa.Column('quantity', sa.Integer, nullable=False),
        sa.Column('unit_price', DECIMAL(10, 2), nullable=False),
        sa.Column('sale_amount', DECIMAL(10, 2), nullable=False),
        sa.Column('commission_amount', DECIMAL(10, 2), nullable=False),
        sa.Column('currency', sa.String(3), default='USD'),
        
        # Statut commission
        sa.Column('commission_status', sa.String(20), default='pending', index=True),
        sa.Column('payment_method', sa.String(50)),
        sa.Column('payment_reference', sa.String(200)),
        sa.Column('payment_notes', sa.Text),
        sa.Column('paid_at', sa.DateTime(timezone=True)),
        
        # Anti-fraude
        sa.Column('buyer_ip', sa.String(45)),
        sa.Column('verification_data', JSONB),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # Index composites
    op.create_index('idx_sale_promoter_status', 'affiliate_sales', ['promoter_user_id', 'commission_status'])
    
    # 5. PromoterWallet
    op.create_table(
        'promoter_wallets',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('user_id', sa.String(50), unique=True, nullable=False, index=True),
        
        # Montants
        sa.Column('pending_amount', DECIMAL(10, 2), default=0, nullable=False),
        sa.Column('available_amount', DECIMAL(10, 2), default=0, nullable=False),
        sa.Column('withdrawn_amount', DECIMAL(10, 2), default=0, nullable=False),
        sa.Column('total_earned', DECIMAL(10, 2), default=0, nullable=False),
        sa.Column('currency', sa.String(3), default='USD'),
        
        # Info bancaire
        sa.Column('bank_name', sa.String(100)),
        sa.Column('bank_account_number', sa.String(100)),
        sa.Column('bank_account_holder', sa.String(150)),
        sa.Column('bank_swift_code', sa.String(50)),
        
        # Statistiques
        sa.Column('total_sales_count', sa.Integer, default=0),
        sa.Column('last_withdrawal_at', sa.DateTime(timezone=True)),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # 6. WalletTransaction
    op.create_table(
        'wallet_transactions',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('wallet_id', UUID(as_uuid=True), sa.ForeignKey('promoter_wallets.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('type', sa.String(30), nullable=False, index=True),
        sa.Column('amount', DECIMAL(10, 2), nullable=False),
        sa.Column('balance_after', DECIMAL(10, 2), nullable=False),
        sa.Column('currency', sa.String(3), default='USD'),
        sa.Column('description', sa.String(255)),
        
        # Référence
        sa.Column('reference_type', sa.String(50)),
        sa.Column('reference_id', UUID(as_uuid=True)),
        sa.Column('sale_id', UUID(as_uuid=True), sa.ForeignKey('affiliate_sales.id', ondelete='SET NULL'), nullable=True),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )
    
    # 7. WithdrawalRequest
    op.create_table(
        'withdrawal_requests',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('wallet_id', UUID(as_uuid=True), sa.ForeignKey('promoter_wallets.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('user_id', sa.String(50), nullable=False, index=True),
        sa.Column('amount', DECIMAL(10, 2), nullable=False),
        sa.Column('currency', sa.String(3), default='USD'),
        sa.Column('payment_method', sa.String(50), default='bank_transfer', nullable=False),
        sa.Column('payment_details', JSONB),
        
        sa.Column('status', sa.String(20), default='pending', nullable=False, index=True),
        sa.Column('processed_by', sa.String(50)),
        sa.Column('processed_at', sa.DateTime(timezone=True)),
        sa.Column('rejection_reason', sa.Text),
        
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now())
    )
    
    # Index composite
    op.create_index('idx_withdrawal_user_status', 'withdrawal_requests', ['user_id', 'status'])


def downgrade():
    op.drop_table('withdrawal_requests')
    op.drop_table('wallet_transactions')
    op.drop_table('promoter_wallets')
    op.drop_table('affiliate_sales')
    op.drop_table('product_promotions')
    op.drop_table('marketplace_products')
    op.drop_table('product_categories')
