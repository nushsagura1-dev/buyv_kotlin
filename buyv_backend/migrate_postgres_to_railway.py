"""
Migration: PostgreSQL (source) → PostgreSQL Railway (cible).
Copie le schéma (création des tables) et les données.

Variables d'environnement requises:
  SOURCE_DATABASE_URL  - ex: postgresql://buyv_admin:buyv123@localhost:5432/buyv_db
  TARGET_DATABASE_URL  - ex: postgresql://postgres:xxx@switchback.proxy.rlwy.net:25752/railway

Usage (depuis buyv_kotlin/buyv_backend):
  set SOURCE_DATABASE_URL=postgresql://...
  set TARGET_DATABASE_URL=postgresql://...
  python migrate_postgres_to_railway.py
"""

import os
import sys
from urllib.parse import urlparse

# Fix Railway URL format
def _normalize_url(url: str) -> str:
    if url.startswith("postgres://"):
        return url.replace("postgres://", "postgresql://", 1)
    return url

def main():
    source_url = os.getenv("SOURCE_DATABASE_URL")
    target_url = os.getenv("TARGET_DATABASE_URL")
    if not source_url or not target_url:
        print("Usage: set SOURCE_DATABASE_URL and TARGET_DATABASE_URL then run this script.")
        print("Example:")
        print("  set SOURCE_DATABASE_URL=postgresql://buyv_admin:buyv123@localhost:5432/buyv_db")
        print("  set TARGET_DATABASE_URL=postgresql://postgres:PASSWORD@switchback.proxy.rlwy.net:25752/railway")
        sys.exit(1)
    source_url = _normalize_url(source_url)
    target_url = _normalize_url(target_url)

    try:
        import psycopg2
        from psycopg2.extras import execute_values
    except ImportError:
        print("Install psycopg2: pip install psycopg2-binary")
        sys.exit(1)

    # Order of tables to copy (respects foreign keys)
    TABLE_ORDER = [
        "users",
        "notifications",
        "follows",
        "orders",
        "order_items",
        "commissions",
        "posts",
        "post_likes",
        "post_bookmarks",
        "comments",
        "comment_likes",
        "affiliate_clicks",
        "reel_views",
        "promoter_wallets",
        "blocked_users",
        "reports",
        "sounds",
        "withdrawal_requests",
        "revoked_tokens",
        "product_categories",
        "marketplace_products",
        "product_promotions",
        "affiliate_sales",
        "wallet_transactions",
    ]

    def get_tables(conn):
        cur = conn.cursor()
        cur.execute("""
            SELECT tablename FROM pg_tables
            WHERE schemaname = 'public'
            ORDER BY tablename
        """)
        return [r[0] for r in cur.fetchall()]

    def table_columns(conn, table):
        cur = conn.cursor()
        cur.execute("""
            SELECT column_name FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = %s
            ORDER BY ordinal_position
        """, (table,))
        return [r[0] for r in cur.fetchall()]

    def copy_table(src_conn, tgt_conn, table):
        cols = table_columns(src_conn, table)
        if not cols:
            return 0
        cur_src = src_conn.cursor()
        cols_quoted = ",".join(f'"{c}"' for c in cols)
        cur_src.execute(f'SELECT {cols_quoted} FROM "{table}"')
        rows = cur_src.fetchall()
        cur_src.close()
        if not rows:
            return 0
        cur_tgt = tgt_conn.cursor()
        cols_str = ",".join(f'"{c}"' for c in cols)
        placeholders = ",".join(["%s"] * len(cols))
        sql = f'INSERT INTO "{table}" ({cols_str}) VALUES ({placeholders}) ON CONFLICT DO NOTHING'
        try:
            cur_tgt.executemany(sql, rows)
            tgt_conn.commit()
            return len(rows)
        except Exception as e:
            tgt_conn.rollback()
            print(f"  Error copying {table}: {e}")
            return -1
        finally:
            cur_tgt.close()

    print("PostgreSQL → Railway migration")
    print("Source:", urlparse(source_url).hostname, urlparse(source_url).path)
    print("Target:", urlparse(target_url).hostname, urlparse(target_url).path)

    src_conn = psycopg2.connect(source_url)
    tgt_conn = psycopg2.connect(target_url)
    src_conn.autocommit = True

    try:
        # 1) Create schema on target using SQLAlchemy
        print("\n1. Creating tables on target...")
        os.environ["DATABASE_URL"] = target_url
        from app.database import Base, engine
        from app import models  # noqa: F401 - register models
        from app.marketplace import models as _  # noqa: F401 - register marketplace tables
        Base.metadata.create_all(bind=engine)
        engine.dispose()
        print("   Tables created.")

        # 2) Copy data in FK order
        print("\n2. Copying data...")
        existing_src = set(get_tables(src_conn))
        total = 0
        for table in TABLE_ORDER:
            if table not in existing_src:
                continue
            n = copy_table(src_conn, tgt_conn, table)
            if n >= 0:
                print(f"   {table}: {n} rows")
                total += n
        print(f"\nDone. Total rows copied: {total}")
    finally:
        src_conn.close()
        tgt_conn.close()

if __name__ == "__main__":
    main()
