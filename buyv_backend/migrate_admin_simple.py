"""
Simplified migration script to consolidate admin_users table into users table.
This script copies admin users from admin_users table to users table with role='admin'.
Uses direct SQL to avoid model loading issues.
"""
import sys
from sqlalchemy import create_engine, text
from app.config import DATABASE_URL

def migrate_admin_users():
    """Migrate admin_users data to users table using raw SQL"""
    
    # Create database engine
    engine = create_engine(DATABASE_URL)
    
    try:
        with engine.connect() as conn:
            print("Starting migration of admin_users to users table...")
            
            # Check if admin_users table exists
            result = conn.execute(text("""
                SELECT COUNT(*) 
                FROM information_schema.tables 
                WHERE table_name = 'admin_users'
            """))
            
            if result.scalar() == 0:
                print("⚠️  admin_users table not found. Already migrated or doesn't exist.")
                return
            
            # Fetch all admin users
            result = conn.execute(text("SELECT id, username, email, password_hash FROM admin_users"))
            admin_users = result.fetchall()
            print(f"Found {len(admin_users)} admin users to migrate")
            
            migrated_count = 0
            skipped_count = 0
            
            for admin in admin_users:
                admin_id, username, email, password_hash = admin
                
                try:
                    # Check if user with this email already exists
                    result = conn.execute(
                        text("SELECT id, role FROM users WHERE email = :email"),
                        {"email": email}
                    )
                    existing_user = result.fetchone()
                    
                    if existing_user:
                        # Update existing user to admin role
                        print(f"User {email} already exists, updating to admin role...")
                        conn.execute(
                            text("UPDATE users SET role = 'admin', display_name = :username WHERE email = :email"),
                            {"username": username, "email": email}
                        )
                        migrated_count += 1
                    else:
                        # Check if username already exists
                        result = conn.execute(
                            text("SELECT id FROM users WHERE username = :username"),
                            {"username": username}
                        )
                        if result.fetchone():
                            print(f"⚠️  Username '{username}' already exists, using alternative username...")
                            username = f"{username}_admin"
                        
                        # Create new user with admin role
                        print(f"Creating new admin user: {email}")
                        conn.execute(
                            text("""
                                INSERT INTO users (
                                    uid, email, username, display_name, password_hash, role,
                                    followers_count, following_count, reels_count, is_verified,
                                    created_at, updated_at
                                )
                                VALUES (
                                    :uid, :email, :username, :display_name, :password_hash, 'admin',
                                    0, 0, 0, FALSE,
                                    NOW(), NOW()
                                )
                            """),
                            {
                                "uid": f"admin_{admin_id}",
                                "email": email,
                                "username": username,
                                "display_name": username,
                                "password_hash": password_hash
                            }
                        )
                        migrated_count += 1
                except Exception as e:
                    print(f"⚠️  Skipping {email}: {str(e)}")
                    skipped_count += 1
                    continue
            
            # Commit transaction
            conn.commit()
            
            print(f"\n✅ Migration completed successfully!")
            print(f"   - Migrated: {migrated_count}")
            print(f"   - Skipped: {skipped_count}")
            print("\nYou can now safely drop the admin_users table after verifying the migration:")
            print("   DROP TABLE IF EXISTS admin_users;")
        
    except Exception as e:
        print(f"❌ Error during migration: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    migrate_admin_users()
