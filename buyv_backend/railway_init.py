"""
Railway deployment initialization script.
Runs in the 'release' phase (before web dyno starts).

1. Creates all database tables (idempotent - CREATE TABLE IF NOT EXISTS)
2. Resets PostgreSQL sequences to maximum IDs (avoids duplicate key errors)

Usage (Procfile release phase):
    release: python railway_init.py
"""
import os
import sys
import logging

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

# ── Resolve DATABASE_URL ──────────────────────────────────────────────────────
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    logger.error("DATABASE_URL is not set. Skipping init (will use SQLite fallback at runtime).")
    sys.exit(0)  # Non-fatal: exit 0 so release phase doesn't block the deploy

# Fix legacy postgres:// → postgresql://
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

logger.info(f"Database backend: {DATABASE_URL.split('@')[-1] if '@' in DATABASE_URL else DATABASE_URL}")

# ── Import models BEFORE create_all so SQLAlchemy knows all tables ─────────────
try:
    from sqlalchemy import create_engine, text

    # Import Base and ALL models to register them
    from app.database import Base
    import app.models          # noqa: F401  – registers User, Reel, Order …
    import app.marketplace.models  # noqa: F401  – registers ProductCategory, MarketplaceProduct …

    engine = create_engine(
        DATABASE_URL,
        pool_pre_ping=True,
        pool_recycle=3600,
        echo=False,
        future=True,
    )
except Exception as e:
    logger.error(f"Import / engine creation failed: {e}")
    sys.exit(1)

# ── Step 1: Create tables ──────────────────────────────────────────────────────
logger.info("Step 1/2 – Creating tables (IF NOT EXISTS) …")
try:
    # Enable uuid-ossp extension for PostgreSQL UUID columns
    with engine.connect() as conn:
        conn.execute(text('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"'))
        conn.commit()

    Base.metadata.create_all(bind=engine)
    logger.info("✓ Tables created / verified.")
except Exception as e:
    logger.error(f"create_all failed: {e}")
    sys.exit(1)

# ── Step 2: Fix sequences ─────────────────────────────────────────────────────
logger.info("Step 2/2 – Resetting PostgreSQL sequences …")

integer_id_tables = [
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
    "wishlist_items",
    "sounds",
    "reports",
    "blocked_users",
]

fixed = 0
skipped = 0
with engine.connect() as conn:
    for table in integer_id_tables:
        try:
            conn.execute(text(f"""
                SELECT setval(
                    pg_get_serial_sequence('{table}', 'id'),
                    COALESCE((SELECT MAX(id) FROM {table}), 0) + 1,
                    false
                )
            """))
            conn.commit()
            logger.info(f"  ✓ Sequence reset: {table}")
            fixed += 1
        except Exception as e:
            logger.warning(f"  ⚠ Skipped {table}: {str(e)[:80]}")
            try:
                conn.rollback()
            except Exception:
                pass
            skipped += 1

logger.info(f"Sequences: {fixed} reset, {skipped} skipped.")
logger.info("─" * 50)
logger.info("✓ Railway init complete. App is ready to start.")
