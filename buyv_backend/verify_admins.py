"""Quick script to verify admin users"""
from sqlalchemy import create_engine, text
from app.config import DATABASE_URL

engine = create_engine(DATABASE_URL)

with engine.connect() as conn:
    result = conn.execute(text("SELECT username, email, role FROM users WHERE role = 'admin'"))
    admins = result.fetchall()
    
    print(f"\n📋 Admin Users ({len(admins)}):")
    print("-" * 60)
    for username, email, role in admins:
        print(f"  • {email}")
        print(f"    Username: {username}")
        print(f"    Role: {role}")
        print()
