import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env from the same directory as this config.py file
env_path = Path(__file__).parent / '.env'
load_dotenv(dotenv_path=env_path)

# MySQL connection via PyMySQL
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "")
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_DB = os.getenv("MYSQL_DB", "Buyv")

SECRET_KEY = os.getenv("SECRET_KEY", "your-super-secret-key-change-it")
ALGORITHM = os.getenv("ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "30"))

# Database URL - supports PostgreSQL (production) and SQLite (local dev)
# Railway will automatically set DATABASE_URL with PostgreSQL connection
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./buyv.db")

# Fix for Railway PostgreSQL URL (postgres:// -> postgresql://)
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

# CJ Dropshipping Keys
CJ_API_KEY = os.getenv("CJ_API_KEY", "")
CJ_ACCOUNT_ID = os.getenv("CJ_ACCOUNT_ID", "")
CJ_EMAIL = os.getenv("CJ_EMAIL", "")
STRIPE_SECRET_KEY = os.getenv("STRIPE_SECRET_KEY", "")