"""
Setup PostgreSQL pour BuyV (Sans Migration)
Crée simplement la base de données et l'utilisateur
FastAPI créera automatiquement les tables au démarrage
"""

import subprocess
import sys
import time

def run_psql_command(command, description):
    """Execute a PostgreSQL command via psql"""
    print(f"📝 {description}...")
    
    # Try without password first (trust authentication)
    result = subprocess.run(
        ['psql', '-U', 'postgres', '-c', command],
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace'  # Replace invalid UTF-8 chars
    )
    
    if result.returncode == 0:
        print(f"   ✅ Success")
        return True
    else:
        # Extract clean error message
        error = result.stderr.replace('\n', ' ').strip()
        if 'already exists' in error.lower() or 'd\xe9j' in error.lower():
            print(f"   ⚠️  Already exists (OK)")
            return True
        else:
            print(f"   ❌ Failed: {error[:100]}")
            return False

def setup_postgresql():
    """Setup PostgreSQL database and user"""
    print("=" * 60)
    print("🗄️  PostgreSQL Setup for BuyV")
    print("=" * 60)
    print()
    
    print("ℹ️  This script will:")
    print("   1. Create user: buyv_admin")
    print("   2. Create database: buyv_db")
    print("   3. Grant privileges")
    print()
    
    # Step 1: Create user
    success1 = run_psql_command(
        "CREATE USER buyv_admin WITH PASSWORD 'buyv123' SUPERUSER;",
        "Creating user 'buyv_admin'"
    )
    
    # Step 2: Create database
    success2 = run_psql_command(
        "CREATE DATABASE buyv_db OWNER buyv_admin;",
        "Creating database 'buyv_db'"
    )
    
    # Step 3: Grant privileges (in case database already existed)
    success3 = run_psql_command(
        "GRANT ALL PRIVILEGES ON DATABASE buyv_db TO buyv_admin;",
        "Granting privileges"
    )
    
    print()
    print("=" * 60)
    
    if success1 or success2:
        print("✅ PostgreSQL Setup Complete!")
        print("=" * 60)
        print()
        print("📝 Next Steps:")
        print()
        print("1️⃣  Update your .env file:")
        print("   DATABASE_URL=postgresql://buyv_admin:buyv123@localhost:5432/buyv_db")
        print()
        print("2️⃣  Start the FastAPI backend:")
        print("   cd buyv_backend")
        print("   uvicorn app.main:app --reload")
        print()
        print("3️⃣  FastAPI will automatically create all tables!")
        print("   You'll see in the logs: 'Database engine created successfully'")
        print()
        print("💡 Note: You'll start with an empty database.")
        print("   Create test data via the API endpoints or Swagger UI:")
        print("   http://localhost:8000/docs")
        
    else:
        print("❌ Setup Failed")
        print("=" * 60)
        print()
        print("🔧 Manual Setup Required:")
        print()
        print("Open PostgreSQL command line tool and run:")
        print()
        print("   CREATE USER buyv_admin WITH PASSWORD 'buyv123' SUPERUSER;")
        print("   CREATE DATABASE buyv_db OWNER buyv_admin;")
        print("   GRANT ALL PRIVILEGES ON DATABASE buyv_db TO buyv_admin;")
        print()
        print("Or use pgAdmin GUI tool.")

if __name__ == "__main__":
    setup_postgresql()
