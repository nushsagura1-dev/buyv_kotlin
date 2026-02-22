"""
Script to create or promote users to admin role.
Run this script to easily make users into admins.
"""
import sys
from passlib.context import CryptContext
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.config import DATABASE_URL
from app.models import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def create_engine_session():
    """Create database engine and session"""
    engine = create_engine(DATABASE_URL)
    SessionLocal = sessionmaker(bind=engine)
    return SessionLocal()

def promote_to_admin(email: str):
    """Promote an existing user to admin role"""
    db = create_engine_session()
    try:
        user = db.query(User).filter(User.email == email).first()
        if not user:
            print(f"❌ User with email '{email}' not found")
            return False
        
        if user.role == "admin":
            print(f"ℹ️ User '{email}' is already an admin")
            return True
        
        user.role = "admin"
        db.commit()
        print(f"✅ User '{email}' promoted to admin successfully!")
        print(f"   Username: {user.username}")
        print(f"   Display Name: {user.display_name}")
        return True
    except Exception as e:
        print(f"❌ Error promoting user: {str(e)}")
        db.rollback()
        return False
    finally:
        db.close()

def create_admin_user(email: str, username: str, display_name: str, password: str):
    """Create a new admin user"""
    db = create_engine_session()
    try:
        # Check if user already exists
        existing_user = db.query(User).filter(
            (User.email == email) | (User.username == username)
        ).first()
        
        if existing_user:
            print(f"❌ User with email '{email}' or username '{username}' already exists")
            print(f"   Use promote_to_admin() instead to make them an admin")
            return False
        
        # Create new admin user
        password_hash = pwd_context.hash(password)
        new_admin = User(
            email=email,
            username=username,
            display_name=display_name,
            password_hash=password_hash,
            role="admin",
            uid=f"admin_{username}"
        )
        
        db.add(new_admin)
        db.commit()
        db.refresh(new_admin)
        
        print(f"✅ Admin user created successfully!")
        print(f"   Email: {email}")
        print(f"   Username: {username}")
        print(f"   Display Name: {display_name}")
        print(f"   Role: admin")
        return True
    except Exception as e:
        print(f"❌ Error creating admin user: {str(e)}")
        db.rollback()
        return False
    finally:
        db.close()

def list_admins():
    """List all admin users"""
    db = create_engine_session()
    try:
        admins = db.query(User).filter(User.role == "admin").all()
        if not admins:
            print("ℹ️ No admin users found")
            return
        
        print(f"\n📋 Admin Users ({len(admins)}):")
        print("-" * 80)
        for admin in admins:
            print(f"  • {admin.email}")
            print(f"    Username: {admin.username}")
            print(f"    Display Name: {admin.display_name}")
            print(f"    UID: {admin.uid}")
            print(f"    Verified: {'Yes' if admin.is_verified else 'No'}")
            print(f"    Created: {admin.created_at}")
            print()
    except Exception as e:
        print(f"❌ Error listing admins: {str(e)}")
    finally:
        db.close()

def main():
    """Main interactive menu"""
    print("\n" + "="*80)
    print("  BUYV Admin User Management")
    print("="*80)
    print("\nOptions:")
    print("  1. List all admin users")
    print("  2. Promote existing user to admin")
    print("  3. Create new admin user")
    print("  4. Exit")
    print()
    
    choice = input("Select option (1-4): ").strip()
    
    if choice == "1":
        list_admins()
    
    elif choice == "2":
        email = input("\nEnter user email to promote: ").strip()
        if email:
            promote_to_admin(email)
        else:
            print("❌ Email is required")
    
    elif choice == "3":
        print("\n📝 Create New Admin User")
        email = input("Email: ").strip()
        username = input("Username: ").strip()
        display_name = input("Display Name: ").strip()
        password = input("Password: ").strip()
        
        if email and username and display_name and password:
            create_admin_user(email, username, display_name, password)
        else:
            print("❌ All fields are required")
    
    elif choice == "4":
        print("👋 Goodbye!")
        sys.exit(0)
    
    else:
        print("❌ Invalid option")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n👋 Goodbye!")
        sys.exit(0)
