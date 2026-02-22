"""Add likes_count column to comments table if missing."""
import sys
sys.path.insert(0, '.')
from app.database import engine
from sqlalchemy import text

with engine.connect() as conn:
    result = conn.execute(text(
        "SELECT column_name FROM information_schema.columns "
        "WHERE table_name = 'comments' AND column_name = 'likes_count'"
    ))
    rows = result.fetchall()
    if len(rows) == 0:
        conn.execute(text("ALTER TABLE comments ADD COLUMN likes_count INTEGER DEFAULT 0"))
        conn.commit()
        print("✅ likes_count column added to comments table")
    else:
        print("✅ likes_count column already exists")
