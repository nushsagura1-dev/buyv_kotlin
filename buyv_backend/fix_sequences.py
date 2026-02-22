"""
Fix PostgreSQL sequences after data migration
Run this script once to reset all auto-increment sequences
"""
import os
from sqlalchemy import create_engine, text

# Get database URL from environment
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    print("ERROR: DATABASE_URL environment variable not set")
    exit(1)

# Handle PostgreSQL URL format
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(DATABASE_URL)

# List of tables with auto-increment IDs
tables = [
    "users",
    "reels", 
    "likes",
    "comments",
    "follows",
    "notifications",
    "products",
    "orders",
    "order_items",
    "categories",
    "carts",
    "cart_items",
    "addresses",
    "reviews",
    "payments",
    "wishlists",
    "wishlist_items"
]

print("Fixing PostgreSQL sequences...")
print("-" * 50)

with engine.connect() as conn:
    for table in tables:
        try:
            # Reset the sequence to the maximum ID + 1
            query = text(f"""
                SELECT setval(
                    pg_get_serial_sequence('{table}', 'id'),
                    COALESCE((SELECT MAX(id) FROM {table}), 0) + 1,
                    false
                );
            """)
            result = conn.execute(query)
            conn.commit()
            print(f"✓ Fixed sequence for table: {table}")
        except Exception as e:
            # Table might not exist, skip it
            print(f"⚠ Skipped {table}: {str(e)[:50]}")
            continue

print("-" * 50)
print("✓ All sequences fixed successfully!")
print("\nYou can now register new users without errors.")
