"""
Cleanup script: Remove all test users except admin accounts and anasjafir@gmail.com
Uses raw SQL to avoid SQLAlchemy model/schema mismatches.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.database import engine
from sqlalchemy import text

KEEP_EMAILS = [
    "buyv@admin.com",
    "anasjafir@gmail.com",
    "admin@buyv.com",
    "finance@buyv.com",
    "moderator@buyv.com",
]

def cleanup():
    with engine.connect() as conn:
        # Get users to delete
        result = conn.execute(text(
            "SELECT id, uid, email, username FROM users WHERE email NOT IN :emails"
        ), {"emails": tuple(KEEP_EMAILS)})
        users = result.fetchall()
        print(f"Found {len(users)} users to delete")
        
        for user in users:
            user_id, user_uid, email, username = user
            print(f"  Deleting: {email} (uid={user_uid}, id={user_id})")
            
            # Get post IDs for this user
            post_ids_result = conn.execute(text("SELECT id FROM posts WHERE user_id = :uid"), {"uid": user_id})
            post_ids = [r[0] for r in post_ids_result.fetchall()]
            
            # Get order IDs for this user
            order_ids_result = conn.execute(text("SELECT id FROM orders WHERE user_id = :uid"), {"uid": user_id})
            order_ids = [r[0] for r in order_ids_result.fetchall()]
            
            # Delete order items
            if order_ids:
                conn.execute(text("DELETE FROM order_items WHERE order_id IN :ids"), {"ids": tuple(order_ids)})
            
            # Delete orders
            conn.execute(text("DELETE FROM orders WHERE user_id = :uid"), {"uid": user_id})
            
            # Delete commissions
            conn.execute(text("DELETE FROM commissions WHERE user_id = :uid"), {"uid": user_id})
            
            # Delete withdrawal requests
            try:
                conn.execute(text("DELETE FROM withdrawal_requests WHERE user_id = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete promoter wallets
            try:
                conn.execute(text("DELETE FROM promoter_wallets WHERE user_id = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete affiliate clicks
            try:
                conn.execute(text("DELETE FROM affiliate_clicks WHERE viewer_uid = :uid OR promoter_uid = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete reel views
            try:
                conn.execute(text("DELETE FROM reel_views WHERE viewer_uid = :uid OR promoter_uid = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete follows
            conn.execute(text("DELETE FROM follows WHERE follower_id = :uid OR followed_id = :uid"), {"uid": user_id})
            
            # Delete notifications
            conn.execute(text("DELETE FROM notifications WHERE user_id = :uid"), {"uid": user_id})
            
            # Delete blocked users
            try:
                conn.execute(text("DELETE FROM blocked_users WHERE blocker_uid = :uid OR blocked_uid = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete reports
            try:
                conn.execute(text("DELETE FROM reports WHERE reporter_uid = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete post-related data
            if post_ids:
                # Delete comment likes for comments on user's posts
                try:
                    conn.execute(text(
                        "DELETE FROM comment_likes WHERE comment_id IN (SELECT id FROM comments WHERE post_id IN :ids)"
                    ), {"ids": tuple(post_ids)})
                except: pass
                
                conn.execute(text("DELETE FROM post_likes WHERE post_id IN :ids"), {"ids": tuple(post_ids)})
                try:
                    conn.execute(text("DELETE FROM post_bookmarks WHERE post_id IN :ids"), {"ids": tuple(post_ids)})
                except: pass
                conn.execute(text("DELETE FROM comments WHERE post_id IN :ids"), {"ids": tuple(post_ids)})
                conn.execute(text("DELETE FROM posts WHERE id IN :ids"), {"ids": tuple(post_ids)})
            
            # Delete user's likes/bookmarks/comments on other posts
            conn.execute(text("DELETE FROM post_likes WHERE user_id = :uid"), {"uid": user_id})
            try:
                conn.execute(text("DELETE FROM post_bookmarks WHERE user_id = :uid"), {"uid": user_id})
            except: pass
            try:
                conn.execute(text("DELETE FROM comment_likes WHERE user_id = :uid"), {"uid": user_id})
            except: pass
            conn.execute(text("DELETE FROM comments WHERE user_id = :uid"), {"uid": user_id})
            
            # Revoked tokens
            try:
                conn.execute(text("DELETE FROM revoked_tokens WHERE user_uid = :uid"), {"uid": user_uid})
            except: pass
            
            # Delete the user
            conn.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": user_id})
        
        conn.commit()
        print(f"\nSuccessfully deleted {len(users)} users")
        
        # Verify
        result = conn.execute(text("SELECT uid, email FROM users"))
        remaining = result.fetchall()
        print(f"\nRemaining users ({len(remaining)}):")
        for u in remaining:
            print(f"  {u[1]} (uid={u[0]})")

if __name__ == "__main__":
    cleanup()
