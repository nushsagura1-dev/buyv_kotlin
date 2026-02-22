# Create Admin User - Workaround
# ================================
# Creates admin user directly without model relationships issues

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))

from sqlalchemy import create_engine, text
from passlib.context import CryptContext
from app.config import DATABASE_URL

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Create engine
if DATABASE_URL.startswith('sqlite'):
    engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
else:
    engine = create_engine(DATABASE_URL, pool_pre_ping=True)

print("🔗 Connected to database")

# Create admin_users table if not exists
with engine.connect() as conn:
    conn.execute(text("""
        CREATE TABLE IF NOT EXISTS admin_users (
            id SERIAL PRIMARY KEY,
            username VARCHAR(255) UNIQUE NOT NULL,
            email VARCHAR(255) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            role VARCHAR(50) NOT NULL DEFAULT 'moderator',
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """))
    conn.commit()
    print("✅ Table admin_users created/verified")

    # Check if admin exists
    result = conn.execute(text("SELECT COUNT(*) FROM admin_users WHERE email = 'admin@buyv.com'"))
    count = result.scalar()
    
    if count > 0:
        print("⚠️  Admin already exists")
        result = conn.execute(text("SELECT username, email, role FROM admin_users WHERE email = 'admin@buyv.com'"))
        admin = result.fetchone()
        print(f"  Username: {admin[0]}")
        print(f"  Email: {admin[1]}")
        print(f"  Role: {admin[2]}")
    else:
        # Create admin
        password_hash = pwd_context.hash("Buyv2024Admin!")
        conn.execute(text("""
            INSERT INTO admin_users (username, email, password_hash, role, is_active)
            VALUES (:username, :email, :password_hash, :role, :is_active)
        """), {
            "username": "buyv_admin",
            "email": "admin@buyv.com",
            "password_hash": password_hash,
            "role": "super_admin",
            "is_active": True
        })
        conn.commit()
        print("✅ Admin created successfully!")
        print("  Username: buyv_admin")
        print("  Email: admin@buyv.com")
        print("  Password: Buyv2024Admin!")
        print("  Role: super_admin")

print("\n✅ Ready to test!")
