"""
Script to reset admin users - delete all admins and create a single new one
"""
from sqlalchemy import create_engine, text
from passlib.context import CryptContext
from app.config import DATABASE_URL

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Admin credentials
ADMIN_EMAIL = "admin@buyv.com"
ADMIN_PASSWORD = "adminbuyv2025"
ADMIN_USERNAME = "admin_buyv"

def reset_admins():
    """Remove admin role from all users and create a single new admin"""
    engine = create_engine(DATABASE_URL)
    
    try:
        with engine.connect() as conn:
            # Change all existing admins to regular users
            print("🔄 Removing admin role from all existing admin users...")
            result = conn.execute(text("UPDATE users SET role = 'user' WHERE role = 'admin'"))
            updated_count = result.rowcount
            print(f"   Changed {updated_count} admin users to regular users")
            
            # Check if admin@buyv.com already exists
            result = conn.execute(
                text("SELECT id, role FROM users WHERE email = :email"),
                {"email": ADMIN_EMAIL}
            )
            existing_user = result.fetchone()
            
            if existing_user:
                # Update existing user to admin
                print(f"\n🔄 User {ADMIN_EMAIL} exists, promoting to admin...")
                password_hash = pwd_context.hash(ADMIN_PASSWORD)
                conn.execute(
                    text("""
                        UPDATE users 
                        SET role = 'admin', 
                            password_hash = :password_hash,
                            username = :username,
                            display_name = :display_name,
                            is_verified = TRUE
                        WHERE email = :email
                    """),
                    {
                        "email": ADMIN_EMAIL,
                        "password_hash": password_hash,
                        "username": ADMIN_USERNAME,
                        "display_name": "Admin BUYV"
                    }
                )
            else:
                # Hash the password
                password_hash = pwd_context.hash(ADMIN_PASSWORD)
                
                # Create new admin user
                print(f"\n✨ Creating new admin user: {ADMIN_EMAIL}")
                conn.execute(
                    text("""
                        INSERT INTO users (
                            uid, email, username, display_name, password_hash, role,
                            followers_count, following_count, reels_count, is_verified,
                            created_at, updated_at
                        )
                        VALUES (
                            :uid, :email, :username, :display_name, :password_hash, 'admin',
                            0, 0, 0, TRUE,
                            NOW(), NOW()
                        )
                    """),
                    {
                        "uid": "admin_primary",
                        "email": ADMIN_EMAIL,
                        "username": ADMIN_USERNAME,
                        "display_name": "Admin BUYV",
                        "password_hash": password_hash
                    }
                )
            
            # Commit transaction
            conn.commit()
            
            print(f"\n✅ Admin user created successfully!")
            print(f"   Email: {ADMIN_EMAIL}")
            print(f"   Password: {ADMIN_PASSWORD}")
            print(f"   Username: {ADMIN_USERNAME}")
            print(f"\n🔐 Use these credentials to login via /auth/admin/login")
        
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    reset_admins()
