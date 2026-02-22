"""
Vues Admin pour le Marketplace.
"""
import sys
import os

# Ajouter le chemin du backend
backend_path = os.path.join(os.path.dirname(__file__), '..', 'buyv_backend')
sys.path.append(backend_path)

from flask_admin.contrib.sqla import ModelView
from flask_admin import expose, BaseView
from flask import redirect, url_for, flash, request, jsonify
from flask_login import current_user
from markupsafe import Markup
from wtforms import SelectField, DecimalField, TextAreaField, StringField
from wtforms.validators import DataRequired, NumberRange
import asyncio
from decimal import Decimal

# Import des models marketplace
from app.marketplace.models import (
    ProductCategory, MarketplaceProduct, ProductPromotion,
    AffiliateSale, PromoterWallet, WithdrawalRequest
)
from app.marketplace.cj_service import CJDropshippingService


class SecureModelView(ModelView):
    """Base ModelView avec authentification."""
    def is_accessible(self):
        return current_user.is_authenticated
    
    def inaccessible_callback(self, name, **kwargs):
        return redirect(url_for('admin.login'))
    
    can_export = True
    can_view_details = True
    page_size = 50


class ProductCategoryAdminView(SecureModelView):
    """Admin pour les catégories de produits."""
    column_searchable_list = ['name', 'name_ar', 'slug']
    column_filters = ['is_active', 'created_at']
    column_list = [
        'name', 'name_ar', 'slug', 'parent', 'display_order',
        'is_active', 'created_at'
    ]
    column_sortable_list = ['name', 'display_order', 'created_at']
    column_default_sort = ('display_order', False)
    
    form_columns = [
        'name', 'name_ar', 'slug', 'parent_id',
        'display_order', 'is_active'
    ]
    
    def __init__(self, session, **kwargs):
        super(ProductCategoryAdminView, self).__init__(ProductCategory, session, **kwargs)


class MarketplaceProductAdminView(SecureModelView):
    """Admin pour les produits du marketplace."""
    column_searchable_list = ['name', 'cj_product_id']
    column_filters = ['status', 'category', 'is_featured', 'is_choice', 'created_at']
    column_list = [
        'name', 'category', 'selling_price', 'commission_rate',
        'status', 'is_featured', 'total_sales', 'total_promotions',
        'created_at'
    ]
    column_sortable_list = [
        'name', 'selling_price', 'commission_rate',
        'total_sales', 'total_promotions', 'created_at'
    ]
    column_default_sort = ('created_at', True)
    
    # Formatage des colonnes
    column_formatters = {
        'selling_price': lambda v, c, m, p: f"${m.selling_price:.2f}" if m.selling_price else "-",
        'commission_rate': lambda v, c, m, p: f"{m.commission_rate}%" if m.commission_rate else "-",
        'status': lambda v, c, m, p: Markup(
            f'<span class="badge badge-{"success" if m.status == "active" else "warning"}">{m.status}</span>'
        ),
        'is_featured': lambda v, c, m, p: Markup(
            '<span class="glyphicon glyphicon-star text-warning"></span>' if m.is_featured else ''
        ),
    }
    
    form_columns = [
        'name', 'description', 'category_id', 'main_image_url',
        'original_price', 'selling_price', 'commission_rate',
        'commission_type', 'cj_product_id', 'status',
        'is_featured', 'is_choice', 'tags'
    ]
    
    form_overrides = {
        'description': TextAreaField,
        'commission_rate': DecimalField,
        'original_price': DecimalField,
        'selling_price': DecimalField,
    }
    
    form_args = {
        'name': {'validators': [DataRequired()]},
        'selling_price': {'validators': [DataRequired(), NumberRange(min=0)]},
        'commission_rate': {'validators': [NumberRange(min=0, max=100)]},
    }
    
    def __init__(self, session, **kwargs):
        super(MarketplaceProductAdminView, self).__init__(MarketplaceProduct, session, **kwargs)


class AffiliateSaleAdminView(SecureModelView):
    """Admin pour les ventes affiliées."""
    can_create = False
    can_edit = True
    can_delete = False
    
    column_searchable_list = ['order_id', 'promoter_user_id', 'buyer_user_id']
    column_filters = ['commission_status', 'created_at', 'paid_at']
    column_list = [
        'order_id', 'product', 'promoter_user_id', 'sale_amount',
        'commission_amount', 'commission_status', 'created_at'
    ]
    column_sortable_list = ['order_id', 'sale_amount', 'commission_amount', 'created_at']
    column_default_sort = ('created_at', True)
    
    # Formatage
    column_formatters = {
        'sale_amount': lambda v, c, m, p: f"${m.sale_amount:.2f}",
        'commission_amount': lambda v, c, m, p: f"${m.commission_amount:.2f}",
        'commission_status': lambda v, c, m, p: Markup(
            f'<span class="badge badge-{_get_status_color(m.commission_status)}">{m.commission_status}</span>'
        ),
    }
    
    form_columns = [
        'commission_status', 'payment_reference',
        'payment_method', 'payment_notes'
    ]
    
    form_overrides = {
        'commission_status': SelectField,
        'payment_notes': TextAreaField,
    }
    
    form_args = {
        'commission_status': {
            'choices': [
                ('pending', 'Pending'),
                ('approved', 'Approved'),
                ('paid', 'Paid'),
                ('cancelled', 'Cancelled')
            ]
        }
    }
    
    def __init__(self, session, **kwargs):
        super(AffiliateSaleAdminView, self).__init__(AffiliateSale, session, **kwargs)


class PromoterWalletAdminView(SecureModelView):
    """Admin pour les portefeuilles des promoteurs."""
    can_create = False
    can_edit = False
    can_delete = False
    
    column_searchable_list = ['user_id']
    column_filters = ['created_at', 'updated_at']
    column_list = [
        'user_id', 'pending_amount', 'available_amount',
        'withdrawn_amount', 'total_earned', 'total_sales_count',
        'created_at'
    ]
    column_sortable_list = [
        'total_earned', 'available_amount', 'total_sales_count', 'created_at'
    ]
    column_default_sort = ('total_earned', True)
    
    # Formatage
    column_formatters = {
        'pending_amount': lambda v, c, m, p: f"${m.pending_amount:.2f}",
        'available_amount': lambda v, c, m, p: f"${m.available_amount:.2f}",
        'withdrawn_amount': lambda v, c, m, p: f"${m.withdrawn_amount:.2f}",
        'total_earned': lambda v, c, m, p: f"${m.total_earned:.2f}",
    }
    
    def __init__(self, session, **kwargs):
        super(PromoterWalletAdminView, self).__init__(PromoterWallet, session, **kwargs)


class WithdrawalRequestAdminView(SecureModelView):
    """Admin pour les demandes de retrait."""
    can_create = False
    can_edit = True
    can_delete = False
    
    column_searchable_list = ['user_id']
    column_filters = ['status', 'payment_method', 'created_at', 'processed_at']
    column_list = [
        'user_id', 'amount', 'payment_method', 'status',
        'created_at', 'processed_at', 'processed_by'
    ]
    column_sortable_list = ['amount', 'status', 'created_at', 'processed_at']
    column_default_sort = ('created_at', True)
    
    # Formatage
    column_formatters = {
        'amount': lambda v, c, m, p: f"${m.amount:.2f}",
        'status': lambda v, c, m, p: Markup(
            f'<span class="badge badge-{_get_status_color(m.status)}">{m.status}</span>'
        ),
    }
    
    form_columns = [
        'status', 'processed_by', 'rejection_reason', 'payment_details'
    ]
    
    form_overrides = {
        'status': SelectField,
        'rejection_reason': TextAreaField,
        'payment_details': TextAreaField,
    }
    
    form_args = {
        'status': {
            'choices': [
                ('pending', 'Pending'),
                ('completed', 'Completed'),
                ('rejected', 'Rejected')
            ]
        }
    }
    
    def __init__(self, session, **kwargs):
        super(WithdrawalRequestAdminView, self).__init__(WithdrawalRequest, session, **kwargs)


class CJImportView(BaseView):
    """Vue personnalisée pour importer des produits depuis CJ Dropshipping."""
    
    @expose('/', methods=['GET', 'POST'])
    def index(self):
        if not current_user.is_authenticated:
            return redirect(url_for('admin.login'))
        
        if request.method == 'POST':
            query = request.form.get('search_query', '')
            if query:
                try:
                    cj_service = CJDropshippingService()
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    results = loop.run_until_complete(cj_service.search_products(query, page=1))
                    loop.close()
                    
                    # Transformer les résultats CJ en format attendu par le template
                    products_list = results.get('list', [])
                    
                    def parse_price(price_str):
                        """Parse CJ price - handle range format like '109.70 -- 152.80'"""
                        if not price_str:
                            return 0.0
                        price_str = str(price_str).strip()
                        # Si c'est un range, prendre le premier prix
                        if '--' in price_str:
                            price_str = price_str.split('--')[0].strip()
                        try:
                            return float(price_str)
                        except ValueError:
                            return 0.0
                    
                    formatted_results = {
                        'products': [
                            {
                                'cj_product_id': p.get('pid'),
                                'name': p.get('productNameEn', ''),
                                'image_url': p.get('productImage'),
                                'selling_price': parse_price(p.get('sellPrice', 0)),
                                'original_price': parse_price(p.get('sellPrice', 0))
                            }
                            for p in products_list
                        ],
                        'total': results.get('total', 0)
                    }
                    
                    # Récupérer toutes les catégories pour le select
                    from database import SessionLocal
                    db = SessionLocal()
                    categories = db.query(ProductCategory).filter_by(is_active=True).all()
                    db.close()
                    
                    return self.render('admin/cj_import_results.html', 
                                     results=formatted_results, 
                                     query=query,
                                     categories=categories)
                except Exception as e:
                    flash(f'Erreur lors de la recherche: {str(e)}', 'error')
        
        return self.render('admin/cj_import.html')
    
    @expose('/import/<cj_product_id>', methods=['POST'])
    def import_product(self, cj_product_id):
        if not current_user.is_authenticated:
            return redirect(url_for('admin.login'))
        
        try:
            # Récupérer les paramètres du formulaire
            commission_rate = Decimal(request.form.get('commission_rate', 10))
            category_id = request.form.get('category_id', None)
            custom_price = request.form.get('custom_price', None)
            
            # Valider que la catégorie est fournie
            if not category_id:
                flash('Erreur: La catégorie est obligatoire!', 'error')
                return redirect(url_for('cjimport.index'))
            
            cj_service = CJDropshippingService()
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            
            # Récupérer détails
            product_data = loop.run_until_complete(cj_service.get_product_details(cj_product_id))
            parsed_data = cj_service.parse_product_data(product_data)
            loop.close()
            
            # Si un prix personnalisé est fourni, l'utiliser
            if custom_price:
                try:
                    parsed_data['selling_price'] = Decimal(custom_price)
                    parsed_data['original_price'] = Decimal(custom_price)
                except:
                    flash('Avertissement: Prix personnalisé invalide, prix CJ utilisé.', 'warning')
            
            # Créer produit
            from database import SessionLocal
            db = SessionLocal()
            
            product = MarketplaceProduct(
                **parsed_data,
                commission_rate=commission_rate,
                category_id=category_id
            )
            
            db.add(product)
            db.commit()
            db.refresh(product)
            db.close()
            
            flash(f'Produit "{product.name}" importé avec succès!', 'success')
            return redirect(url_for('marketplaceproduct.index_view'))
            
        except Exception as e:
            flash(f'Erreur lors de l\'importation: {str(e)}', 'error')
            return redirect(url_for('cjimport.index'))


class MarketplaceDashboardView(BaseView):
    """Dashboard marketplace avec statistiques."""
    
    @expose('/')
    def index(self):
        if not current_user.is_authenticated:
            return redirect(url_for('admin.login'))
        
        from database import SessionLocal
        from sqlalchemy import func
        
        db = SessionLocal()
        
        try:
            # Statistiques globales
            total_products = db.query(func.count(MarketplaceProduct.id)).scalar()
            active_products = db.query(func.count(MarketplaceProduct.id)).filter(
                MarketplaceProduct.status == 'active'
            ).scalar()
            
            total_sales = db.query(func.count(AffiliateSale.id)).scalar()
            total_revenue = db.query(func.sum(AffiliateSale.sale_amount)).scalar() or 0
            total_commissions = db.query(func.sum(AffiliateSale.commission_amount)).filter(
                AffiliateSale.commission_status.in_(['approved', 'paid'])
            ).scalar() or 0
            
            pending_commissions = db.query(func.sum(AffiliateSale.commission_amount)).filter(
                AffiliateSale.commission_status == 'pending'
            ).scalar() or 0
            
            pending_withdrawals = db.query(func.count(WithdrawalRequest.id)).filter(
                WithdrawalRequest.status == 'pending'
            ).scalar()
            
            # Top produits
            top_products = db.query(MarketplaceProduct).order_by(
                MarketplaceProduct.total_sales.desc()
            ).limit(10).all()
            
            # Top promoteurs
            top_promoters = db.query(
                PromoterWallet.user_id,
                PromoterWallet.total_earned,
                PromoterWallet.total_sales_count
            ).order_by(PromoterWallet.total_earned.desc()).limit(10).all()
            
            # Ventes récentes
            recent_sales = db.query(AffiliateSale).order_by(
                AffiliateSale.created_at.desc()
            ).limit(10).all()
            
            stats = {
                'total_products': total_products,
                'active_products': active_products,
                'total_sales': total_sales,
                'total_revenue': float(total_revenue),
                'total_commissions': float(total_commissions),
                'pending_commissions': float(pending_commissions),
                'pending_withdrawals': pending_withdrawals,
                'top_products': top_products,
                'top_promoters': top_promoters,
                'recent_sales': recent_sales,
            }
            
            return self.render('admin/marketplace_dashboard.html', stats=stats)
            
        except Exception as e:
            flash(f'Erreur lors du chargement du dashboard: {str(e)}', 'error')
            return self.render('admin/marketplace_dashboard.html', stats={})
        finally:
            db.close()


def _get_status_color(status):
    """Retourne la couleur Bootstrap pour un statut."""
    colors = {
        'pending': 'warning',
        'approved': 'info',
        'paid': 'success',
        'completed': 'success',
        'cancelled': 'danger',
        'rejected': 'danger',
        'active': 'success',
        'inactive': 'secondary',
    }
    return colors.get(status, 'secondary')
