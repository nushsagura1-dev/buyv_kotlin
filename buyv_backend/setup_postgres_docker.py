"""
Migration Script: SQLite to PostgreSQL (Version Simple - Sans Encodage)
Alternative qui utilise Docker PostgreSQL pour éviter les problèmes d'encodage
"""

import sqlite3
import subprocess
import sys
import time
from datetime import datetime

def check_docker():
    """Check if Docker is available"""
    try:
        result = subprocess.run(['docker', '--version'], capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ Docker is available")
            return True
        return False
    except:
        return False

def start_postgres_docker():
    """Start PostgreSQL in Docker"""
    print("🐳 Starting PostgreSQL in Docker...")
    
    # Stop and remove existing container if any
    subprocess.run(['docker', 'stop', 'buyv-postgres'], capture_output=True)
    subprocess.run(['docker', 'rm', 'buyv-postgres'], capture_output=True)
    
    # Start new container
    cmd = [
        'docker', 'run', '--name', 'buyv-postgres',
        '-e', 'POSTGRES_USER=buyv_admin',
        '-e', 'POSTGRES_PASSWORD=buyv123',
        '-e', 'POSTGRES_DB=buyv_db',
        '-p', '5432:5432',
        '-d', 'postgres:15'
    ]
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode == 0:
        print("✅ PostgreSQL container started")
        print("⏳ Waiting for PostgreSQL to be ready...")
        time.sleep(10)
        return True
    else:
        print(f"❌ Failed to start container: {result.stderr}")
        return False

def main():
    print("=" * 60)
    print("🚀 SQLite → PostgreSQL Migration (Docker)")
    print("=" * 60)
    print()
    
    if not check_docker():
        print("❌ Docker is not available")
        print("   Please install Docker Desktop: https://www.docker.com/products/docker-desktop")
        return
    
    if not start_postgres_docker():
        print("❌ Could not start PostgreSQL")
        return
    
    print("\n✅ PostgreSQL is ready!")
    print("\n📝 Next steps:")
    print("   1. Update your .env file:")
    print("      DATABASE_URL=postgresql://buyv_admin:buyv123@localhost:5432/buyv_db")
    print()
    print("   2. The database is empty. FastAPI will create tables automatically")
    print("      when you start the backend:")
    print("      uvicorn app.main:app --reload")
    print()
    print("   3. To stop PostgreSQL later:")
    print("      docker stop buyv-postgres")
    print()
    print("   4. To start it again:")
    print("      docker start buyv-postgres")

if __name__ == "__main__":
    main()
