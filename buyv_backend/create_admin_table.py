"""
Script de migration : Crée la table admin_users et ajoute des comptes admin par défaut
Usage: python create_admin_table.py
"""
import sys
import os
from pathlib import Path

# Add parent directory to path
backend_path = Path(__file__).parent
sys.path.insert(0, str(backend_path))

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from passlib.context import CryptContext

# Import models to create tables
from app.database import Base
from app.config import DATABASE_URL
from app.models import AdminUser

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def create_admin_table():
    """Create admin_users table and seed default admins"""
    
    # Get database URL
    print(f"🔗 Connecting to database...")
    
    # Create engine
    if DATABASE_URL.startswith('sqlite'):
        engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
    else:
        engine = create_engine(DATABASE_URL, pool_pre_ping=True)
    
    # Create all tables
    print("📋 Creating admin_users table...")
    Base.metadata.create_all(bind=engine, tables=[AdminUser.__table__])
    
    # Create session
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db = SessionLocal()
    
    try:
        # Check if any admin exists
        existing_admins = db.query(AdminUser).count()
        
        if existing_admins > 0:
            print(f"⚠️  Found {existing_admins} existing admin(s). Skipping seed.")
            print("\nExisting admins:")
            for admin in db.query(AdminUser).all():
                print(f"  - {admin.username} ({admin.email}) - Role: {admin.role}")
            return
        
        # Seed default admins
        print("🌱 Seeding default admin accounts...")
        
        default_admins = [
            {
                "username": "buyv_admin",
                "email": "admin@buyv.com",
                "password": "Buyv2024Admin!",
                "role": "super_admin"
            },
            {
                "username": "finance_admin",
                "email": "finance@buyv.com",
                "password": "Finance2024!",
                "role": "finance"
            },
            {
                "username": "moderator",
                "email": "moderator@buyv.com",
                "password": "Moderator2024!",
                "role": "moderator"
            }
        ]
        
        for admin_data in default_admins:
            admin = AdminUser(
                username=admin_data["username"],
                email=admin_data["email"],
                password_hash=pwd_context.hash(admin_data["password"]),
                role=admin_data["role"],
                is_active=True
            )
            db.add(admin)
            print(f"  ✅ Created: {admin_data['username']} ({admin_data['role']})")
        
        db.commit()
        
        print("\n" + "="*60)
        print("✅ Admin accounts created successfully!")
        print("="*60)
        print("\n📋 Default Admin Credentials:")
        print("-" * 60)
        for admin_data in default_admins:
            print(f"\n{admin_data['role'].upper()}:")
            print(f"  Username: {admin_data['username']}")
            print(f"  Email:    {admin_data['email']}")
            print(f"  Password: {admin_data['password']}")
        
        print("\n" + "="*60)
        print("⚠️  IMPORTANT SECURITY NOTES:")
        print("="*60)
        print("1. Change these passwords immediately in production!")
        print("2. Use /auth/admin/login endpoint to get admin tokens")
        print("3. Admin tokens are separate from user tokens")
        print("4. Role permissions:")
        print("   - super_admin: Full access to everything")
        print("   - finance: Withdrawals, commissions, payments, sales")
        print("   - moderator: Users, content, orders (no financial data)")
        print("="*60 + "\n")
        
    except Exception as e:
        db.rollback()
        print(f"❌ Error: {e}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    print("="*60)
    print("🔐 Buyv Admin User Table Migration")
    print("="*60)
    print()
    
    try:
        create_admin_table()
        print("✅ Migration completed successfully!")
    except Exception as e:
        print(f"\n❌ Migration failed: {e}")
        sys.exit(1)
