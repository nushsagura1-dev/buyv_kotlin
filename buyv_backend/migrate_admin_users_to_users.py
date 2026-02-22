"""
Migration script to consolidate admin_users table into users table.
This script copies admin users from admin_users table to users table with role='admin'.
"""
import sys
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.config import DATABASE_URL
from app.models import User
from datetime import datetime

# Import AdminUser from old schema for migration
from sqlalchemy import Table, MetaData, Column, Integer, String, Boolean, DateTime

def migrate_admin_users():
    """Migrate admin_users data to users table"""
    
    # Create database engine
    engine = create_engine(DATABASE_URL)
    SessionLocal = sessionmaker(bind=engine)
    db = SessionLocal()
    
    try:
        print("Starting migration of admin_users to users table...")
        
        # Reflect the admin_users table if it exists
        metadata = MetaData()
        try:
            admin_users_table = Table('admin_users', metadata, autoload_with=engine)
        except Exception as e:
            print(f"⚠️  admin_users table not found or already migrated: {str(e)}")
            print("ℹ️  No migration needed.")
            return
        
        # Fetch all admin users using raw SQL
        result = db.execute(admin_users_table.select())
        admin_users = result.fetchall()
        print(f"Found {len(admin_users)} admin users to migrate")
        
        migrated_count = 0
        skipped_count = 0
        
        for admin in admin_users:
            # Access columns by index or name (depends on SQLAlchemy version)
            admin_email = admin.email if hasattr(admin, 'email') else admin[2]
            admin_username = admin.username if hasattr(admin, 'username') else admin[1]
            admin_password_hash = admin.password_hash if hasattr(admin, 'password_hash') else admin[3]
            admin_id = admin.id if hasattr(admin, 'id') else admin[0]
            
            # Check if user with this email already exists
            existing_user = db.query(User).filter(User.email == admin_email).first()
            
            if existing_user:
                # Update existing user to admin role
                print(f"User {admin_email} already exists, updating to admin role...")
                existing_user.role = "admin"
                existing_user.display_name = admin_username
                migrated_count += 1
            else:
                # Create new user with admin role
                print(f"Creating new admin user: {admin_email}")
                new_user = User(
                    email=admin_email,
                    username=admin_username,
                    display_name=admin_username,
                    password_hash=admin_password_hash,
                    role="admin",
                    uid=f"admin_{admin_id}"  # Unique identifier
                )
                db.add(new_user)
                migrated_count += 1
        
        # Commit all changes
        db.commit()
        print(f"\n✅ Migration completed successfully!")
        print(f"   - Migrated: {migrated_count}")
        print(f"   - Skipped: {skipped_count}")
        print("\nYou can now safely drop the admin_users table after verifying the migration.")
        
    except Exception as e:
        print(f"❌ Error during migration: {str(e)}")
        db.rollback()
        sys.exit(1)
    finally:
        db.close()

if __name__ == "__main__":
    migrate_admin_users()
