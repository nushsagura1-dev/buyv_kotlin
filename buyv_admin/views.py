"""
Custom ModelView classes for Flask-Admin
Provides enhanced CRUD interfaces for each model
"""

from flask_admin.contrib.sqla import ModelView
from flask_admin import expose
from flask import redirect, url_for, flash
from flask_login import current_user
from wtforms import TextAreaField
from wtforms.widgets import TextArea
from markupsafe import Markup


class SecureModelView(ModelView):
    """Base ModelView with authentication"""
    def is_accessible(self):
        return current_user.is_authenticated
    
    def inaccessible_callback(self, name, **kwargs):
        return redirect(url_for('admin.login'))
    
    can_export = True
    can_view_details = True
    page_size = 50


class UserAdminView(SecureModelView):
    """Admin view for User model"""
    column_searchable_list = ['username', 'email', 'display_name']
    column_filters = ['is_verified', 'created_at', 'updated_at']
    column_list = [
        'username', 'email', 'display_name', 'is_verified',
        'followers_count', 'following_count', 'reels_count',
        'created_at'
    ]
    column_sortable_list = [
        'username', 'email', 'created_at', 'followers_count',
        'following_count', 'reels_count'
    ]
    column_default_sort = ('created_at', True)
    
    # Block deletion of users with orders/commissions
    can_delete = True
    
    def delete_model(self, model):
        """Override delete to check for related records"""
        try:
            # Import models to check relationships
            from models import Order, Commission, Post, Comment
            
            # Check if user has orders
            has_orders = self.session.query(Order).filter_by(user_id=model.id).count() > 0
            has_commissions = self.session.query(Commission).filter_by(user_id=model.id).count() > 0
            has_posts = self.session.query(Post).filter_by(user_id=model.id).count() > 0
            has_comments = self.session.query(Comment).filter_by(user_id=model.id).count() > 0
            
            if has_orders or has_commissions:
                flash(f'Cannot delete user "{model.username}" - User has {self.session.query(Order).filter_by(user_id=model.id).count()} orders and {self.session.query(Commission).filter_by(user_id=model.id).count()} commissions. Delete these first or archive the user instead.', 'error')
                return False
            
            if has_posts or has_comments:
                flash(f'Warning: User "{model.username}" has {self.session.query(Post).filter_by(user_id=model.id).count()} posts and {self.session.query(Comment).filter_by(user_id=model.id).count()} comments. These will become orphaned.', 'warning')
            
            # Proceed with deletion
            return super(UserAdminView, self).delete_model(model)
            
        except Exception as e:
            flash(f'Error deleting user: {str(e)}', 'error')
            self.session.rollback()
            return False
    
    # Details view
    column_details_list = [
        'uid', 'username', 'email', 'display_name', 'bio',
        'profile_image_url', 'is_verified', 'followers_count',
        'following_count', 'reels_count', 'fcm_token',
        'created_at', 'updated_at'
    ]
    
    # Form configuration
    form_excluded_columns = ['posts', 'comments', 'likes', 'bookmarks', 
                             'followers', 'following', 'sent_notifications',
                             'received_notifications', 'commissions', 'payments',
                             'orders']
    
    # Custom labels
    column_labels = {
        'uid': 'User ID',
        'display_name': 'Display Name',
        'profile_image_url': 'Profile Image',
        'is_verified': 'Verified',
        'followers_count': 'Followers',
        'following_count': 'Following',
        'reels_count': 'Reels',
        'fcm_token': 'Firebase Token'
    }
    
    # Actions
    def action_verify_users(self, ids):
        """Verify selected users"""
        try:
            count = 0
            for user_id in ids:
                user = self.session.query(self.model).get(user_id)
                if user and not user.is_verified:
                    user.is_verified = True
                    count += 1
            self.session.commit()
            flash(f'Successfully verified {count} users!', 'success')
        except Exception as e:
            flash(f'Error verifying users: {str(e)}', 'error')
            self.session.rollback()
    
    def action_unverify_users(self, ids):
        """Unverify selected users"""
        try:
            count = 0
            for user_id in ids:
                user = self.session.query(self.model).get(user_id)
                if user and user.is_verified:
                    user.is_verified = False
                    count += 1
            self.session.commit()
            flash(f'Successfully unverified {count} users!', 'success')
        except Exception as e:
            flash(f'Error unverifying users: {str(e)}', 'error')
            self.session.rollback()
    
    column_formatters = {
        'is_verified': lambda v, c, m, p: '✅' if m.is_verified else '❌'
    }


class PostAdminView(SecureModelView):
    """Admin view for Post model"""
    column_searchable_list = ['caption']
    column_filters = ['type', 'created_at', 'updated_at']
    column_list = [
        'uid', 'type', 'likes_count',
        'comments_count', 'created_at'
    ]
    column_sortable_list = [
        'type', 'likes_count', 'comments_count', 'created_at'
    ]
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'uid': 'Post ID',
        'likes_count': 'Likes',
        'comments_count': 'Comments',
        'media_url': 'Media URL',
        'caption': 'Caption'
    }
    
    form_excluded_columns = ['likes', 'comments']
    
    column_formatters = {
        'type': lambda v, c, m, p: {'reel': '🎥', 'product': '🛍️', 'photo': '📷'}.get(m.type, m.type)
    }


class OrderAdminView(SecureModelView):
    """Admin view for Order model"""
    column_searchable_list = ['id', 'buyer_email']
    column_filters = ['status', 'created_at', 'updated_at']
    column_list = [
        'id', 'buyer_email', 'total_amount', 'status',
        'created_at', 'updated_at'
    ]
    column_sortable_list = ['created_at', 'total_amount', 'status']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'buyer_email': 'Buyer Email',
        'total_amount': 'Total Amount',
        'stripe_payment_intent_id': 'Stripe Payment ID'
    }
    
    column_formatters = {
        'status': lambda v, c, m, p: {
            'pending': '⏳ Pending',
            'paid': '✅ Paid',
            'failed': '❌ Failed',
            'refunded': '↩️ Refunded'
        }.get(m.status, m.status),
        'total_amount': lambda v, c, m, p: f"${m.total_amount:.2f}" if m.total_amount else 'N/A'
    }


class CommissionAdminView(SecureModelView):
    """Admin view for Commission model"""
    column_searchable_list = ['product_name']
    column_filters = ['status', 'created_at', 'paid_at']
    column_list = [
        'product_name', 'amount',
        'rate', 'status', 'created_at', 'paid_at'
    ]
    column_sortable_list = ['amount', 'rate', 'status', 'created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'product_name': 'Product',
        'paid_at': 'Paid Date',
        'cj_order_id': 'CJ Order ID'
    }
    
    form_excluded_columns = ['post']
    
    column_formatters = {
        'status': lambda v, c, m, p: {
            'pending': '⏳ Pending',
            'paid': '✅ Paid',
            'cancelled': '❌ Cancelled'
        }.get(m.status, m.status),
        'amount': lambda v, c, m, p: f"${m.amount:.2f}" if m.amount else 'N/A',
        'rate': lambda v, c, m, p: f"{m.rate}%" if m.rate else 'N/A'
    }
    
    def action_mark_as_paid(self, ids):
        """Mark selected commissions as paid"""
        try:
            from datetime import datetime
            count = 0
            for comm_id in ids:
                comm = self.session.query(self.model).get(comm_id)
                if comm and comm.status != 'paid':
                    comm.status = 'paid'
                    comm.paid_at = datetime.utcnow()
                    count += 1
            self.session.commit()
            flash(f'Successfully marked {count} commissions as paid!', 'success')
        except Exception as e:
            flash(f'Error updating commissions: {str(e)}', 'error')
            self.session.rollback()


class CommentAdminView(SecureModelView):
    """Admin view for Comment model"""
    column_searchable_list = ['content']
    column_filters = ['created_at']
    column_list = ['content', 'created_at']
    column_sortable_list = ['created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'content': 'Comment Content'
    }
    
    form_excluded_columns = ['replies', 'parent']


class NotificationAdminView(SecureModelView):
    """Admin view for Notification model"""
    column_filters = ['type', 'is_read', 'created_at']
    column_list = [
        'type', 'is_read', 'created_at'
    ]
    column_sortable_list = ['type', 'is_read', 'created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'is_read': 'Read',
        'type': 'Type'
    }
    
    column_formatters = {
        'is_read': lambda v, c, m, p: '✅' if m.is_read else '📬',
        'type': lambda v, c, m, p: {
            'like': '❤️ Like',
            'comment': '💬 Comment',
            'follow': '👤 Follow',
            'mention': '📣 Mention'
        }.get(m.type, m.type)
    }


class FollowAdminView(SecureModelView):
    """Admin view for Follow model"""
    column_list = ['created_at']
    column_sortable_list = ['created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'created_at': 'Follow Date'
    }


class PostLikeAdminView(SecureModelView):
    """Admin view for PostLike model"""
    column_list = ['created_at']
    column_sortable_list = ['created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'created_at': 'Like Date'
    }


class BookmarkAdminView(SecureModelView):
    """Admin view for Bookmark model"""
    column_list = ['created_at']
    column_sortable_list = ['created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'created_at': 'Bookmark Date'
    }


class PaymentAdminView(SecureModelView):
    """Admin view for Payment model"""
    column_searchable_list = ['stripe_payment_id']
    column_filters = ['status', 'created_at']
    column_list = [
        'amount', 'status',
        'stripe_payment_id', 'created_at'
    ]
    column_sortable_list = ['amount', 'status', 'created_at']
    column_default_sort = ('created_at', True)
    
    column_labels = {
        'stripe_payment_id': 'Stripe Payment ID'
    }
    
    column_formatters = {
        'status': lambda v, c, m, p: {
            'pending': '⏳ Pending',
            'completed': '✅ Completed',
            'failed': '❌ Failed'
        }.get(m.status, m.status),
        'amount': lambda v, c, m, p: f"${m.amount:.2f}" if m.amount else 'N/A'
    }
