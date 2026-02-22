"""
Service métier pour le Marketplace.
"""
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, desc, func
from typing import List, Optional, Dict, Any
from uuid import UUID
from decimal import Decimal
import logging

from app.marketplace.models import (
    MarketplaceProduct, ProductCategory, ProductPromotion,
    AffiliateSale, PromoterWallet, WalletTransaction, WithdrawalRequest
)
from app.marketplace.schemas import (
    ProductCreate, ProductUpdate, CategoryCreate, CategoryUpdate,
    PromotionCreate, AffiliateSaleCreate, WithdrawalRequest as WithdrawalRequestSchema
)
from app.marketplace.cj_service import CJDropshippingService
from app.models import Post  # For reel_video_url update on promotion creation

logger = logging.getLogger(__name__)


class MarketplaceService:
    """Service pour gérer le marketplace."""
    
    def __init__(self, db: Session):
        self.db = db
        self.cj_service = CJDropshippingService()
    
    # ============================================
    # PRODUCTS
    # ============================================
    
    def get_products(
        self,
        category_id: Optional[UUID] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        min_commission: Optional[float] = None,
        search: Optional[str] = None,
        status: str = "active",
        sort_by: str = "relevance",
        page: int = 1,
        limit: int = 20
    ) -> Dict[str, Any]:
        """Liste des produits avec filtres."""
        query = self.db.query(MarketplaceProduct).filter(
            MarketplaceProduct.status == status
        )
        
        # Filtres
        if category_id:
            query = query.filter(MarketplaceProduct.category_id == category_id)
        
        if min_price:
            query = query.filter(MarketplaceProduct.selling_price >= min_price)
        
        if max_price:
            query = query.filter(MarketplaceProduct.selling_price <= max_price)
        
        if min_commission:
            query = query.filter(MarketplaceProduct.commission_rate >= min_commission)
        
        if search:
            search_term = f"%{search}%"
            query = query.filter(
                or_(
                    MarketplaceProduct.name.ilike(search_term),
                    MarketplaceProduct.description.ilike(search_term),
                    MarketplaceProduct.tags.contains([search])
                )
            )
        
        # Tri
        if sort_by == "price_asc":
            query = query.order_by(MarketplaceProduct.selling_price.asc())
        elif sort_by == "price_desc":
            query = query.order_by(MarketplaceProduct.selling_price.desc())
        elif sort_by == "commission":
            query = query.order_by(MarketplaceProduct.commission_rate.desc())
        elif sort_by == "rating":
            query = query.order_by(MarketplaceProduct.average_rating.desc())
        elif sort_by == "sales":
            query = query.order_by(MarketplaceProduct.total_sales.desc())
        elif sort_by == "recent":
            query = query.order_by(MarketplaceProduct.created_at.desc())
        elif sort_by == "popular":
            query = query.order_by(
                MarketplaceProduct.total_sales.desc(),
                MarketplaceProduct.average_rating.desc()
            )
        else:  # relevance
            query = query.order_by(
                MarketplaceProduct.is_featured.desc(),
                MarketplaceProduct.is_choice.desc(),
                MarketplaceProduct.total_sales.desc()
            )
        
        # Pagination
        total = query.count()
        products = query.offset((page - 1) * limit).limit(limit).all()
        
        return {
            "items": products,
            "total": total,
            "page": page,
            "limit": limit,
            "total_pages": (total + limit - 1) // limit
        }
    
    def get_product(self, product_id: UUID) -> Optional[MarketplaceProduct]:
        """Obtenir un produit par ID."""
        product = self.db.query(MarketplaceProduct).filter(
            MarketplaceProduct.id == product_id
        ).first()
        
        if product:
            # Incrémenter vues
            product.total_views += 1
            self.db.commit()
        
        return product
    
    def create_product(self, product_data: ProductCreate) -> MarketplaceProduct:
        """Créer un nouveau produit."""
        product = MarketplaceProduct(**product_data.dict())
        self.db.add(product)
        self.db.commit()
        self.db.refresh(product)
        return product
    
    def update_product(self, product_id: UUID, product_data: ProductUpdate) -> Optional[MarketplaceProduct]:
        """Mettre à jour un produit."""
        product = self.get_product(product_id)
        if not product:
            return None
        
        update_data = product_data.dict(exclude_unset=True)
        for field, value in update_data.items():
            setattr(product, field, value)
        
        self.db.commit()
        self.db.refresh(product)
        return product
    
    def delete_product(self, product_id: UUID) -> bool:
        """Supprimer un produit (soft delete)."""
        product = self.get_product(product_id)
        if not product:
            return False
        
        product.status = "inactive"
        self.db.commit()
        return True
    
    def get_featured_products(self, limit: int = 10) -> List[MarketplaceProduct]:
        """Produits mis en avant."""
        return self.db.query(MarketplaceProduct).filter(
            MarketplaceProduct.status == "active"
        ).order_by(
            desc(MarketplaceProduct.is_featured),
            MarketplaceProduct.total_sales.desc()
        ).limit(limit).all()
    
    # ============================================
    # CJ DROPSHIPPING INTEGRATION
    # ============================================
    
    async def search_cj_products(
        self,
        query: str,
        category: Optional[str] = None,
        page: int = 1
    ) -> Dict[str, Any]:
        """Rechercher des produits sur CJ."""
        raw_result = await self.cj_service.search_products(query, category, page)
        
        # Mapping Backend -> Android API contract
        # Android expects: products (list), total, page, page_size
        # CJ returns: list (list), total, pageNum, pageSize
        
        products = []
        cj_list = raw_result.get("list", [])
        if cj_list:
            for item in cj_list:
                # Price parsing (handle ranges like "10.00-20.00" or "--")
                sell_price_str = str(item.get("sellPrice", "0"))
                if '--' in sell_price_str:
                    sell_price_str = sell_price_str.split('--')[0].strip()
                elif '-' in sell_price_str: # Sometimes range uses simple dash
                     parts = sell_price_str.split('-')
                     if len(parts) > 1:
                         sell_price_str = parts[0].strip()

                try:
                    sell_price = float(sell_price_str)
                except (ValueError, TypeError):
                    sell_price = 0.0

                # Image parsing
                image_url = item.get("productImage", "")
                if isinstance(image_url, list) and image_url:
                     image_url = image_url[0]
                elif isinstance(image_url, str) and image_url.startswith('['):
                     try:
                         import json
                         images = json.loads(image_url)
                         image_url = images[0] if images else ""
                     except:
                         pass

                products.append({
                    "product_id": item.get("pid"),
                    "product_name": item.get("productNameEn") or item.get("productName", ""),
                    "product_image": image_url,
                    "sell_price": sell_price,
                    "original_price": None, # Not always available in list
                    "product_url": None, # detail only
                    "category_name": item.get("categoryName"),
                    "description": None, # detail only
                    "variants": []
                })
            
        return {
            "products": products,
            "total": raw_result.get("total", 0),
            "page": raw_result.get("pageNum", page),
            "page_size": raw_result.get("pageSize", 20)
        }
    
    async def import_cj_product(
        self,
        cj_product_id: str,
        commission_rate: Decimal,
        category_id: Optional[UUID] = None,
        custom_description: Optional[str] = None,
        selling_price: Optional[Decimal] = None
    ) -> MarketplaceProduct:
        """Importer un produit de CJ."""
        # Récupérer les détails du produit CJ
        cj_data = await self.cj_service.get_product_details(cj_product_id)
        parsed_data = self.cj_service.parse_product_data(cj_data)
        
        # Créer le produit dans notre DB
        product_data = ProductCreate(
            **parsed_data,
            commission_rate=commission_rate,
            category_id=category_id,
            is_featured=True
        )
        
        if custom_description:
            product_data.description = custom_description
            
        if selling_price is not None:
            product_data.selling_price = selling_price
            # Do NOT overwrite original_price - preserve the CJ retail/suggested price
        
        return self.create_product(product_data)
    
    async def sync_product_with_cj(self, product_id: UUID) -> bool:
        """Synchroniser prix/stock avec CJ."""
        product = self.get_product(product_id)
        if not product or not product.cj_product_id:
            return False
        
        try:
            cj_data = await self.cj_service.get_product_details(product.cj_product_id)
            parsed = self.cj_service.parse_product_data(cj_data)
            
            # Mettre à jour prix
            product.original_price = parsed["original_price"]
            product.selling_price = parsed["selling_price"]
            
            # Vérifier stock
            inventory = await self.cj_service.check_inventory(product.cj_product_id)
            if not inventory.get("in_stock", False):
                product.status = "out_of_stock"
            else:
                product.status = "active"
            
            self.db.commit()
            return True
        except Exception as e:
            logger.error(f"Failed to sync product {product_id}: {str(e)}")
            return False
    
    # ============================================
    # CATEGORIES
    # ============================================
    
    def get_categories(self, parent_id: Optional[UUID] = None) -> List[ProductCategory]:
        """Liste des catégories."""
        query = self.db.query(ProductCategory).filter(
            ProductCategory.is_active == True
        )
        
        if parent_id:
            query = query.filter(ProductCategory.parent_id == parent_id)
        else:
            query = query.filter(ProductCategory.parent_id.is_(None))
        
        return query.order_by(ProductCategory.display_order).all()
    
    def create_category(self, category_data: CategoryCreate) -> ProductCategory:
        """Créer une catégorie."""
        category = ProductCategory(**category_data.dict())
        self.db.add(category)
        self.db.commit()
        self.db.refresh(category)
        return category
    
    # ============================================
    # PROMOTIONS
    # ============================================
    
    def create_promotion(self, promotion_data: PromotionCreate, user_id: str) -> ProductPromotion:
        """Créer une promotion (lier vidéo à produit)."""
        # Vérifier si promotion existe déjà
        existing = self.db.query(ProductPromotion).filter(
            ProductPromotion.post_id == promotion_data.post_id,
            ProductPromotion.product_id == promotion_data.product_id
        ).first()
        
        if existing:
            return existing
        
        promotion = ProductPromotion(
            **promotion_data.dict(),
            promoter_user_id=user_id
        )
        
        self.db.add(promotion)
        
        # Incrémenter compteur de promotions du produit
        product = self.get_product(promotion_data.product_id)
        if product:
            product.total_promotions += 1
            # Lier la vidéo du post au produit pour l'affichage dans le feed reels
            try:
                post = self.db.query(Post).filter(Post.uid == str(promotion_data.post_id)).first()
                if post and post.media_url:
                    product.reel_video_url = post.media_url
                    logger.info(f"✅ reel_video_url set on product {product.id}: {post.media_url[:60]}...")
            except Exception as e:
                logger.warning(f"⚠️ Could not update reel_video_url: {e}")
        
        self.db.commit()
        self.db.refresh(promotion)
        return promotion
    
    def get_promotion_by_post(self, post_id: str) -> Optional[ProductPromotion]:
        """Obtenir la promotion liée à un post."""
        return self.db.query(ProductPromotion).filter(
            ProductPromotion.post_id == post_id
        ).first()
    
    def get_promotions_by_product(self, product_id: UUID) -> List[ProductPromotion]:
        """Toutes les promotions d'un produit."""
        return self.db.query(ProductPromotion).filter(
            ProductPromotion.product_id == product_id
        ).order_by(desc(ProductPromotion.created_at)).all()
    
    def get_user_promotions(self, user_id: str) -> List[ProductPromotion]:
        """Promotions d'un utilisateur."""
        return self.db.query(ProductPromotion).filter(
            ProductPromotion.promoter_user_id == user_id
        ).order_by(desc(ProductPromotion.created_at)).all()
    
    def increment_promotion_view(self, promotion_id: UUID):
        """Incrémenter vues d'une promotion."""
        promotion = self.db.query(ProductPromotion).filter(
            ProductPromotion.id == promotion_id
        ).first()
        if promotion:
            promotion.views_count += 1
            self.db.commit()
    
    def increment_promotion_click(self, promotion_id: UUID):
        """Incrémenter clics d'une promotion."""
        promotion = self.db.query(ProductPromotion).filter(
            ProductPromotion.id == promotion_id
        ).first()
        if promotion:
            promotion.clicks_count += 1
            self.db.commit()
    
    # ============================================
    # AFFILIATE SALES
    # ============================================
    
    def create_affiliate_sale(self, sale_data: AffiliateSaleCreate) -> AffiliateSale:
        """Enregistrer une vente affiliée."""
        sale = AffiliateSale(**sale_data.dict())
        self.db.add(sale)
        
        # Mettre à jour stats produit
        product = self.get_product(sale_data.product_id)
        if product:
            product.total_sales += sale_data.quantity
        
        # Mettre à jour stats promotion
        if sale_data.promotion_id:
            promotion = self.db.query(ProductPromotion).filter(
                ProductPromotion.id == sale_data.promotion_id
            ).first()
            if promotion:
                promotion.sales_count += 1
                promotion.total_revenue += sale_data.sale_amount
                promotion.total_commission_earned += sale_data.commission_amount
        
        # Créer/mettre à jour wallet si promoteur
        if sale_data.promoter_user_id:
            wallet = self._get_or_create_wallet(sale_data.promoter_user_id)
            wallet.pending_amount += sale_data.commission_amount
            wallet.total_earned += sale_data.commission_amount
            wallet.total_sales_count += 1
        
        self.db.commit()
        self.db.refresh(sale)
        return sale
    
    def get_promoter_sales(
        self,
        promoter_user_id: str,
        status: Optional[str] = None
    ) -> List[AffiliateSale]:
        """Ventes d'un promoteur."""
        query = self.db.query(AffiliateSale).filter(
            AffiliateSale.promoter_user_id == promoter_user_id
        )
        
        if status:
            query = query.filter(AffiliateSale.commission_status == status)
        
        return query.order_by(desc(AffiliateSale.created_at)).all()
    
    def approve_sale_commission(
        self,
        sale_id: UUID,
        admin_user_id: str,
        payment_reference: Optional[str] = None,
        payment_notes: Optional[str] = None
    ) -> Optional[AffiliateSale]:
        """Approuver la commission d'une vente."""
        sale = self.db.query(AffiliateSale).filter(
            AffiliateSale.id == sale_id
        ).first()
        
        if not sale or sale.commission_status != "pending":
            return None
        
        sale.commission_status = "approved"
        
        # Transférer de pending à available dans le wallet
        if sale.promoter_user_id:
            wallet = self._get_or_create_wallet(sale.promoter_user_id)
            wallet.pending_amount -= sale.commission_amount
            wallet.available_amount += sale.commission_amount
            
            # Créer transaction
            transaction = WalletTransaction(
                wallet_id=wallet.id,
                type="commission",
                amount=sale.commission_amount,
                balance_after=wallet.available_amount,
                reference_type="sale",
                reference_id=sale_id,
                description=f"Commission approuvée pour vente #{sale.order_id}"
            )
            self.db.add(transaction)
        
        self.db.commit()
        self.db.refresh(sale)
        return sale
    
    def mark_sale_paid(
        self,
        sale_id: UUID,
        payment_reference: str,
        payment_method: str = "bank_transfer",
        payment_notes: Optional[str] = None
    ) -> Optional[AffiliateSale]:
        """Marquer une vente comme payée."""
        sale = self.db.query(AffiliateSale).filter(
            AffiliateSale.id == sale_id
        ).first()
        
        if not sale or sale.commission_status not in ["pending", "approved"]:
            return None
        
        sale.commission_status = "paid"
        sale.payment_reference = payment_reference
        sale.payment_method = payment_method
        sale.payment_notes = payment_notes
        sale.paid_at = func.now()
        
        self.db.commit()
        self.db.refresh(sale)
        return sale
    
    # ============================================
    # WALLETS
    # ============================================
    
    def _get_or_create_wallet(self, user_id: str) -> PromoterWallet:
        """Obtenir ou créer le wallet d'un utilisateur."""
        wallet = self.db.query(PromoterWallet).filter(
            PromoterWallet.user_id == user_id
        ).first()
        
        if not wallet:
            wallet = PromoterWallet(user_id=user_id)
            self.db.add(wallet)
            self.db.commit()
            self.db.refresh(wallet)
        
        return wallet
    
    def get_wallet(self, user_id: str) -> PromoterWallet:
        """Obtenir le wallet d'un utilisateur."""
        return self._get_or_create_wallet(user_id)
    
    def get_wallet_transactions(self, user_id: str, limit: int = 50) -> List[WalletTransaction]:
        """Historique des transactions."""
        wallet = self.get_wallet(user_id)
        return self.db.query(WalletTransaction).filter(
            WalletTransaction.wallet_id == wallet.id
        ).order_by(desc(WalletTransaction.created_at)).limit(limit).all()
    
    # ============================================
    # WITHDRAWALS
    # ============================================
    
    def create_withdrawal_request(
        self,
        user_id: str,
        withdrawal_data: WithdrawalRequestSchema
    ) -> WithdrawalRequest:
        """Créer une demande de retrait."""
        wallet = self.get_wallet(user_id)
        
        # Vérifier solde disponible
        if wallet.available_amount < withdrawal_data.amount:
            raise ValueError("Insufficient balance")
        
        # Créer la demande
        withdrawal = WithdrawalRequest(
            wallet_id=wallet.id,
            user_id=user_id,
            amount=withdrawal_data.amount,
            payment_method=withdrawal_data.payment_method,
            payment_details={
                "bank_name": withdrawal_data.bank_name,
                "account_number": withdrawal_data.bank_account_number,
                "account_holder": withdrawal_data.bank_account_holder,
                "swift_code": withdrawal_data.bank_swift_code
            }
        )
        
        self.db.add(withdrawal)
        
        # Déduire du solde disponible
        wallet.available_amount -= withdrawal_data.amount
        
        self.db.commit()
        self.db.refresh(withdrawal)
        return withdrawal
    
    def get_user_withdrawals(self, user_id: str) -> List[WithdrawalRequest]:
        """Demandes de retrait d'un utilisateur."""
        wallet = self.get_wallet(user_id)
        return self.db.query(WithdrawalRequest).filter(
            WithdrawalRequest.wallet_id == wallet.id
        ).order_by(desc(WithdrawalRequest.created_at)).all()
    
    def process_withdrawal(
        self,
        withdrawal_id: UUID,
        admin_user_id: str,
        status: str,
        payment_reference: Optional[str] = None,
        rejection_reason: Optional[str] = None
    ) -> Optional[WithdrawalRequest]:
        """Traiter une demande de retrait."""
        withdrawal = self.db.query(WithdrawalRequest).filter(
            WithdrawalRequest.id == withdrawal_id
        ).first()
        
        if not withdrawal or withdrawal.status != "pending":
            return None
        
        withdrawal.status = status
        withdrawal.processed_by = admin_user_id
        withdrawal.processed_at = func.now()
        
        wallet = self.db.query(PromoterWallet).filter(
            PromoterWallet.id == withdrawal.wallet_id
        ).first()
        
        if status == "completed":
            # Marquer comme retiré
            wallet.withdrawn_amount += withdrawal.amount
            
            # Créer transaction
            transaction = WalletTransaction(
                wallet_id=wallet.id,
                type="withdrawal",
                amount=-withdrawal.amount,
                balance_after=wallet.available_amount,
                reference_type="withdrawal_request",
                reference_id=withdrawal_id,
                description=f"Retrait effectué - Ref: {payment_reference or 'N/A'}"
            )
            self.db.add(transaction)
            
        elif status == "rejected":
            # Remettre le montant dans le solde disponible
            wallet.available_amount += withdrawal.amount
            withdrawal.rejection_reason = rejection_reason
        
        self.db.commit()
        self.db.refresh(withdrawal)
        return withdrawal
