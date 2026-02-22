"""
Script to run SQL migrations on the database
"""
from app.database import SessionLocal, engine
from sqlalchemy import text
import sys

def run_migration(sql_file: str):
    """Execute SQL migration file"""
    print(f"Running migration: {sql_file}")
    
    with open(sql_file, 'r', encoding='utf-8') as f:
        sql = f.read()
    
    try:
        with engine.connect() as conn:
            # Execute migration
            conn.execute(text(sql))
            conn.commit()
            print("✅ Migration completed successfully!")
            return True
    except Exception as e:
        print(f"❌ Migration failed: {str(e)}")
        return False

if __name__ == "__main__":
    if len(sys.argv) > 1:
        sql_file = sys.argv[1]
    else:
        sql_file = "migrations/add_thumbnail_url_to_posts.sql"
    
    success = run_migration(sql_file)
    sys.exit(0 if success else 1)
