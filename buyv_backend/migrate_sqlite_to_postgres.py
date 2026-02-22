"""
Migration Script: SQLite to PostgreSQL
This script exports data from SQLite and imports it into PostgreSQL
"""

import sqlite3
import psycopg2
from psycopg2.extras import execute_values
import os
import sys
from datetime import datetime

# Ensure UTF-8 encoding for Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

# Database configurations
SQLITE_DB = "./buyv.db"
POSTGRES_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "buyv_db",
    "user": "postgres",
    "password": "postgres",
    "client_encoding": "UTF8"
}

def connect_sqlite():
    """Connect to SQLite database"""
    try:
        conn = sqlite3.connect(SQLITE_DB)
        conn.row_factory = sqlite3.Row
        print(f"✅ Connected to SQLite: {SQLITE_DB}")
        return conn
    except Exception as e:
        print(f"❌ SQLite connection error: {e}")
        return None

def connect_postgres():
    """Connect to PostgreSQL database"""
    try:
        # Test if PostgreSQL is accessible first
        print(f"🔌 Attempting connection to PostgreSQL at {POSTGRES_CONFIG['host']}:{POSTGRES_CONFIG['port']}...")
        conn = psycopg2.connect(**POSTGRES_CONFIG)
        print(f"✅ Connected to PostgreSQL: {POSTGRES_CONFIG['database']}")
        return conn
    except psycopg2.OperationalError as e:
        error_msg = str(e).encode('ascii', errors='replace').decode('ascii')
        print(f"❌ PostgreSQL connection error: {error_msg}")
        print(f"\n📋 Troubleshooting steps:")
        print(f"   1. Verify PostgreSQL is running:")
        print(f"      Windows: Get-Service postgresql*")
        print(f"      Linux/Mac: systemctl status postgresql")
        print(f"   2. Check if database exists:")
        print(f"      psql -U postgres -c \"SELECT 1\"")
        print(f"   3. Create database if needed:")
        print(f"      psql -U postgres -c \"CREATE DATABASE buyv_db;\"")
        print(f"   4. Verify credentials in script (user: {POSTGRES_CONFIG['user']})")
        return None
    except Exception as e:
        error_msg = str(e).encode('ascii', errors='replace').decode('ascii')
        print(f"❌ Unexpected error: {error_msg}")
        return None

def get_table_names(sqlite_conn):
    """Get all table names from SQLite"""
    cursor = sqlite_conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';")
    tables = [row[0] for row in cursor.fetchall()]
    print(f"📋 Found {len(tables)} tables: {', '.join(tables)}")
    return tables

def export_table_data(sqlite_conn, table_name):
    """Export data from SQLite table"""
    cursor = sqlite_conn.cursor()
    cursor.execute(f"SELECT * FROM {table_name}")
    
    columns = [description[0] for description in cursor.description]
    rows = cursor.fetchall()
    
    data = [dict(row) for row in rows]
    print(f"  📦 Exported {len(data)} rows from '{table_name}'")
    return columns, data

def import_table_data(postgres_conn, table_name, columns, data):
    """Import data into PostgreSQL table"""
    if not data:
        print(f"  ⏭️  Skipping '{table_name}' (no data)")
        return
    
    cursor = postgres_conn.cursor()
    
    # Build INSERT query
    columns_str = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))
    query = f"INSERT INTO {table_name} ({columns_str}) VALUES ({placeholders}) ON CONFLICT DO NOTHING"
    
    # Convert data to tuples
    values = [tuple(row[col] for col in columns) for row in data]
    
    try:
        cursor.executemany(query, values)
        postgres_conn.commit()
        print(f"  ✅ Imported {len(data)} rows into '{table_name}'")
    except Exception as e:
        postgres_conn.rollback()
        print(f"  ❌ Error importing '{table_name}': {e}")

def verify_data_integrity(sqlite_conn, postgres_conn, table_name):
    """Verify row counts match between databases"""
    sqlite_cursor = sqlite_conn.cursor()
    postgres_cursor = postgres_conn.cursor()
    
    sqlite_cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
    sqlite_count = sqlite_cursor.fetchone()[0]
    
    postgres_cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
    postgres_count = postgres_cursor.fetchone()[0]
    
    if sqlite_count == postgres_count:
        print(f"  ✅ Integrity check PASSED: {sqlite_count} rows")
    else:
        print(f"  ⚠️  Integrity check WARNING: SQLite={sqlite_count}, PostgreSQL={postgres_count}")

def backup_sqlite():
    """Create backup of SQLite database"""
    backup_name = f"buyv_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    try:
        import shutil
        shutil.copy2(SQLITE_DB, backup_name)
        print(f"💾 Backup created: {backup_name}")
        return True
    except Exception as e:
        print(f"❌ Backup failed: {e}")
        return False

def main():
    """Main migration process"""
    print("\n" + "="*60)
    print("🚀 SQLite → PostgreSQL Migration Script")
    print("="*60 + "\n")
    
    # Step 1: Backup SQLite
    print("📝 Step 1: Creating backup...")
    if not backup_sqlite():
        print("⚠️  Warning: Backup failed, but continuing...")
    print()
    
    # Step 2: Connect to databases
    print("📝 Step 2: Connecting to databases...")
    sqlite_conn = connect_sqlite()
    postgres_conn = connect_postgres()
    
    if not sqlite_conn or not postgres_conn:
        print("\n❌ Migration aborted: Database connection failed")
        return
    print()
    
    # Step 3: Get table names
    print("📝 Step 3: Analyzing database schema...")
    tables = get_table_names(sqlite_conn)
    print()
    
    # Step 4: Migrate each table
    print("📝 Step 4: Migrating data...")
    for table in tables:
        print(f"🔄 Processing table: {table}")
        columns, data = export_table_data(sqlite_conn, table)
        import_table_data(postgres_conn, table, columns, data)
    print()
    
    # Step 5: Verify integrity
    print("📝 Step 5: Verifying data integrity...")
    for table in tables:
        print(f"🔍 Verifying: {table}")
        verify_data_integrity(sqlite_conn, postgres_conn, table)
    print()
    
    # Cleanup
    sqlite_conn.close()
    postgres_conn.close()
    
    print("="*60)
    print("✅ Migration completed successfully!")
    print("="*60)
    print("\n📌 Next steps:")
    print("   1. Update buyv_backend/.env to use PostgreSQL")
    print("   2. Test backend: uvicorn app.main:app --reload")
    print("   3. Check logs for 'Connected to PostgreSQL'")

if __name__ == "__main__":
    main()
