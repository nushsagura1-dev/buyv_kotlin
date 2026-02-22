"""
Migration script to add post_bookmarks table
Run this once after updating models.py
"""
from sqlalchemy import create_engine, text
import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./buyv.db")

# Create engine
engine = create_engine(DATABASE_URL)

# SQL to create post_bookmarks table
CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS post_bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_post_bookmark UNIQUE (post_id, user_id)
);
"""

CREATE_INDEX_USER_SQL = """
CREATE INDEX IF NOT EXISTS idx_post_bookmarks_user_id ON post_bookmarks(user_id);
"""

CREATE_INDEX_POST_SQL = """
CREATE INDEX IF NOT EXISTS idx_post_bookmarks_post_id ON post_bookmarks(post_id);
"""

def main():
    print("🔧 Adding post_bookmarks table...")
    
    try:
        with engine.connect() as conn:
            # Create table
            conn.execute(text(CREATE_TABLE_SQL))
            conn.commit()
            print("✅ Table post_bookmarks created")
            
            # Create indexes (separately)
            conn.execute(text(CREATE_INDEX_USER_SQL))
            conn.commit()
            conn.execute(text(CREATE_INDEX_POST_SQL))
            conn.commit()
            print("✅ Indexes created")
            
        print("\n🎉 Migration completed successfully!")
        print("\nYou can now:")
        print("1. Deploy backend to Railway")
        print("2. Rebuild Flutter app")
        print("3. Test bookmark functionality")
        
    except Exception as e:
        print(f"❌ Migration failed: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())
