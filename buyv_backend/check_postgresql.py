"""
PostgreSQL Diagnostic Script
Vérifie si PostgreSQL est installé et accessible
"""

import subprocess
import sys

def check_postgresql_service():
    """Check if PostgreSQL service is running"""
    print("🔍 Checking PostgreSQL service status...")
    try:
        if sys.platform == 'win32':
            result = subprocess.run(
                ['powershell', '-Command', 'Get-Service', 'postgresql*'],
                capture_output=True,
                text=True,
                timeout=5
            )
            print(result.stdout)
            if 'Running' in result.stdout:
                print("✅ PostgreSQL service is running")
                return True
            else:
                print("❌ PostgreSQL service is NOT running")
                print("   Start it with: Start-Service postgresql-x64-15")
                return False
        else:
            result = subprocess.run(
                ['systemctl', 'status', 'postgresql'],
                capture_output=True,
                text=True,
                timeout=5
            )
            if 'active (running)' in result.stdout:
                print("✅ PostgreSQL service is running")
                return True
            else:
                print("❌ PostgreSQL service is NOT running")
                return False
    except Exception as e:
        print(f"⚠️  Could not check service status: {e}")
        return False

def check_postgresql_connection():
    """Try to connect to PostgreSQL"""
    print("\n🔍 Testing PostgreSQL connection...")
    try:
        import psycopg2
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="postgres",  # Default database
            user="postgres",
            password="postgres",
            connect_timeout=3
        )
        conn.close()
        print("✅ Can connect to PostgreSQL with default credentials")
        return True
    except ImportError:
        print("❌ psycopg2 not installed")
        print("   Install it with: pip install psycopg2-binary")
        return False
    except Exception as e:
        print(f"❌ Cannot connect to PostgreSQL: {e}")
        print("   Possible issues:")
        print("   - PostgreSQL not installed")
        print("   - Wrong credentials (default: postgres/postgres)")
        print("   - PostgreSQL not accepting connections")
        return False

def check_database_exists():
    """Check if buyv_db exists"""
    print("\n🔍 Checking if buyv_db exists...")
    try:
        import psycopg2
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="postgres",
            user="postgres",
            password="postgres",
            connect_timeout=3
        )
        cursor = conn.cursor()
        cursor.execute("SELECT 1 FROM pg_database WHERE datname='buyv_db'")
        exists = cursor.fetchone() is not None
        conn.close()
        
        if exists:
            print("✅ Database 'buyv_db' exists")
            return True
        else:
            print("❌ Database 'buyv_db' does NOT exist")
            print("   Create it with:")
            print('   psql -U postgres -c "CREATE DATABASE buyv_db;"')
            return False
    except Exception as e:
        print(f"⚠️  Could not check database: {e}")
        return False

def main():
    print("=" * 60)
    print("🩺 PostgreSQL Diagnostic Tool")
    print("=" * 60)
    print()
    
    # Check 1: Service running
    service_ok = check_postgresql_service()
    
    # Check 2: Connection
    connection_ok = check_postgresql_connection()
    
    # Check 3: Database exists
    database_ok = False
    if connection_ok:
        database_ok = check_database_exists()
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 SUMMARY")
    print("=" * 60)
    print(f"PostgreSQL Service: {'✅ OK' if service_ok else '❌ NOT OK'}")
    print(f"Connection Test:    {'✅ OK' if connection_ok else '❌ NOT OK'}")
    print(f"Database 'buyv_db': {'✅ EXISTS' if database_ok else '❌ MISSING'}")
    
    if service_ok and connection_ok and database_ok:
        print("\n🎉 PostgreSQL is ready for migration!")
        print("   You can now run: python migrate_sqlite_to_postgres.py")
    else:
        print("\n⚠️  PostgreSQL is NOT ready. Follow the steps above to fix issues.")
        
        if not service_ok:
            print("\n📝 To install PostgreSQL on Windows:")
            print("   1. Download: https://www.postgresql.org/download/windows/")
            print("   2. Or use winget: winget install PostgreSQL.PostgreSQL")
            print("   3. Or use chocolatey: choco install postgresql")

if __name__ == "__main__":
    main()
