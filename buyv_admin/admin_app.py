"""
Buyv Admin Panel - Flask-Admin Application
Provides web-based administration interface for the Buyv e-commerce platform
"""

from flask import Flask, redirect, url_for, request, flash
from flask_admin import Admin, AdminIndexView, expose
from flask_admin.contrib.sqla import ModelView
from flask_login import LoginManager, UserMixin, login_user, logout_user, login_required, current_user
from flask_babel import Babel
from passlib.context import CryptContext
import sys
import os
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Use same password context as backend (bcrypt)
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Add parent directory to path to import backend modules
backend_path = os.path.join(os.path.dirname(__file__), '..', 'buyv_backend')
sys.path.append(backend_path)

# Import DATABASE_URL from config.py (loads .env automatically)
from config import DATABASE_URL

# Create database engine
if DATABASE_URL.startswith('sqlite'):
    engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
else:
    # PostgreSQL doesn't need check_same_thread
    engine = create_engine(DATABASE_URL, pool_pre_ping=True)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

import models
from models import User as UserModel
from views import (
    UserAdminView, PostAdminView, OrderAdminView, CommissionAdminView,
    CommentAdminView, NotificationAdminView, FollowAdminView, PostLikeAdminView,
    PaymentAdminView
)
from marketplace_views import (
    ProductCategoryAdminView, MarketplaceProductAdminView,
    AffiliateSaleAdminView, PromoterWalletAdminView,
    WithdrawalRequestAdminView, CJImportView, MarketplaceDashboardView
)

# Flask app configuration
app = Flask(__name__)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'your-super-secret-key-change-in-production')
app.config['FLASK_ADMIN_SWATCH'] = 'cerulean'
app.config['BABEL_DEFAULT_LOCALE'] = 'en'
app.config['BABEL_TRANSLATION_DIRECTORIES'] = 'translations'
# Fix for serving static files in production
app.config['SEND_FILE_MAX_AGE_DEFAULT'] = 0
app.config['TEMPLATES_AUTO_RELOAD'] = True

# Initialize Babel for internationalization
babel = Babel(app)

# Create scoped session factory for thread-safe database access
from sqlalchemy.orm import scoped_session
db_session_factory = scoped_session(SessionLocal)

# Add teardown to handle transaction rollback on errors
@app.teardown_appcontext
def shutdown_session(exception=None):
    """Ensure database sessions are properly closed and rolled back on errors"""
    db_session_factory.remove()

# Login manager setup
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'admin.login'

# Remove root redirect - Flask-Admin handles it
# @app.route('/')
# def index():
#     if current_user.is_authenticated:
#         return redirect('/admin')
#     return redirect('/login')


class AdminUser(UserMixin):
    """Admin user model for Flask-Login"""
    def __init__(self, id, username, role='moderator'):
        self.id = id
        self.username = username
        self.role = role


@login_manager.user_loader
def load_user(user_id):
    """Load user for Flask-Login from database"""
    try:
        db = SessionLocal()
        admin = db.query(UserModel).filter(
            UserModel.id == int(user_id),
            UserModel.role == 'admin'
        ).first()
        db.close()
        
        if admin:
            return AdminUser(admin.id, admin.username, admin.role)
        return None
    except Exception as e:
        print(f"Error loading admin user: {e}")
        return None


class SecureAdminIndexView(AdminIndexView):
    """Custom admin index view with authentication and dashboard"""
    
    @expose('/')
    def index(self):
        if not current_user.is_authenticated:
            return redirect(url_for('.login'))
        
        # Get database session
        db = SessionLocal()
        
        try:
            # Calculate statistics
            total_users = db.query(models.User).count()
            verified_users = db.query(models.User).filter_by(is_verified=True).count()
            total_posts = db.query(models.Post).count()
            total_reels = db.query(models.Post).filter_by(type='reel').count()
            total_products = db.query(models.Post).filter_by(type='product').count()
            total_orders = db.query(models.Order).count()
            pending_orders = db.query(models.Order).filter_by(status='pending').count()
            total_commissions = db.query(models.Commission).count()
            pending_commissions = db.query(models.Commission).filter_by(status='pending').count()
            total_comments = db.query(models.Comment).count()
            total_likes = db.query(models.PostLike).count()
            total_follows = db.query(models.Follow).count()
            
            # Calculate revenue (sum of paid commissions)
            paid_commissions = db.query(models.Commission).filter_by(status='paid').all()
            total_revenue = sum(c.amount for c in paid_commissions if c.amount)
            
            # Recent activity
            recent_users = db.query(models.User).order_by(models.User.created_at.desc()).limit(5).all()
            recent_orders = db.query(models.Order).order_by(models.Order.created_at.desc()).limit(5).all()
            
            stats = {
                'users': {
                    'total': total_users,
                    'verified': verified_users,
                    'unverified': total_users - verified_users
                },
                'content': {
                    'total_posts': total_posts,
                    'reels': total_reels,
                    'products': total_products,
                    'comments': total_comments,
                    'likes': total_likes
                },
                'social': {
                    'follows': total_follows
                },
                'commerce': {
                    'total_orders': total_orders,
                    'pending_orders': pending_orders,
                    'total_commissions': total_commissions,
                    'pending_commissions': pending_commissions,
                    'total_revenue': total_revenue
                },
                'recent_users': recent_users,
                'recent_orders': recent_orders
            }
            
            return self.render('admin/index.html', stats=stats)
            
        finally:
            db.close()
    
    @expose('/login', methods=['GET', 'POST'])
    def login(self):
        if current_user.is_authenticated:
            return redirect(url_for('.index'))
        
        if request.method == 'POST':
            email = request.form.get('email')  # Changed from username to email
            password = request.form.get('password')
            
            # Get database session
            db = SessionLocal()
            
            try:
                # Find admin by email (must have role='admin')
                admin_user = db.query(UserModel).filter(
                    UserModel.email == email,
                    UserModel.role == 'admin'
                ).first()
                
                if admin_user and pwd_context.verify(password, admin_user.password_hash):
                    # No last_login field in User model
                    # db.commit() not needed
                    
                    # Create Flask-Login user object
                    user = AdminUser(admin_user.id, admin_user.username, admin_user.role)
                    login_user(user)
                    
                    flash(f'Welcome {admin_user.username}! ({admin_user.role})', 'success')
                    return redirect(url_for('.index'))
                else:
                    flash('Invalid email or password', 'error')
            except Exception as e:
                flash(f'Login error: {str(e)}', 'error')
            finally:
                db.close()
        
        return self.render('admin/login.html')
    
    @expose('/logout')
    @login_required
    def logout(self):
        logout_user()
        flash('You have been logged out.', 'info')
        return redirect(url_for('.login'))


class SecureModelView(ModelView):
    """Base ModelView with authentication"""
    def is_accessible(self):
        return current_user.is_authenticated
    
    def inaccessible_callback(self, name, **kwargs):
        return redirect(url_for('admin.login'))


# Initialize Flask-Admin at root URL
admin = Admin(
    app,
    name='Buyv Admin',
    index_view=SecureAdminIndexView(name='Dashboard', url='/'),
    url='/'
)

# Use the scoped session factory created earlier
db_session = db_session_factory

# Add model views to admin
admin.add_view(UserAdminView(models.User, db_session, name='Users', category='User Management'))
admin.add_view(FollowAdminView(models.Follow, db_session, name='Follows', category='User Management'))

admin.add_view(PostAdminView(models.Post, db_session, name='Posts', category='Content'))
admin.add_view(CommentAdminView(models.Comment, db_session, name='Comments', category='Content'))
admin.add_view(PostLikeAdminView(models.PostLike, db_session, name='Likes', category='Content'))

admin.add_view(SecureModelView(models.Order, db_session, name='Orders', category='Commerce'))
admin.add_view(SecureModelView(models.Commission, db_session, name='Commissions', category='Commerce'))

# Marketplace views
admin.add_view(MarketplaceDashboardView(name='Marketplace Dashboard', endpoint='marketplace_dashboard', category='Marketplace'))
admin.add_view(ProductCategoryAdminView(db_session, name='Categories', endpoint='productcategory', category='Marketplace'))
admin.add_view(MarketplaceProductAdminView(db_session, name='Products', endpoint='marketplaceproduct', category='Marketplace'))
admin.add_view(CJImportView(name='Import from CJ', endpoint='cjimport', category='Marketplace'))
admin.add_view(AffiliateSaleAdminView(db_session, name='Affiliate Sales', endpoint='affiliatesale', category='Marketplace'))
admin.add_view(PromoterWalletAdminView(db_session, name='Promoter Wallets', endpoint='promoterwallet', category='Marketplace'))
admin.add_view(WithdrawalRequestAdminView(db_session, name='Withdrawal Requests', endpoint='withdrawalrequest', category='Marketplace'))

admin.add_view(NotificationAdminView(models.Notification, db_session, name='Notifications', category='System'))


if __name__ == '__main__':
    print("=" * 60)
    print("🚀 Buyv Admin Panel Starting...")
    print("=" * 60)
    print("📊 Dashboard: http://localhost:5000/admin/")
    print("🔐 Login credentials:")
    print("   Username: admin")
    print("   Password: admin123")
    print("=" * 60)
    print("⚠️  IMPORTANT: Change default passwords in production!")
    print("=" * 60)
    
    # use_reloader=False pour éviter les loops de reload quand le backend change
    app.run(debug=True, host='0.0.0.0', port=5000, use_reloader=False)
