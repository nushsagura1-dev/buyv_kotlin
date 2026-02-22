import os
import secrets
from dotenv import load_dotenv

load_dotenv()

# MySQL connection via PyMySQL
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "")
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_DB = os.getenv("MYSQL_DB", "Buyv")

# SECRET_KEY - CRITICAL SECURITY
# In production, SECRET_KEY MUST be set in environment variables
SECRET_KEY = os.getenv("SECRET_KEY")

# Detect environment
IS_PRODUCTION = os.getenv("RAILWAY_ENVIRONMENT") or os.getenv("RENDER_SERVICE_ID") or os.getenv("FLY_APP_NAME")

if not SECRET_KEY:
    if IS_PRODUCTION:
        raise ValueError(
            "🚨 CRITICAL: SECRET_KEY environment variable is NOT set!\n"
            "This is REQUIRED in production.\n"
            "Generate one with: python -c 'import secrets; print(secrets.token_urlsafe(32))'\n"
            "Then set it in your deployment environment variables."
        )
    # Development mode only: generate random key (NEVER print it)
    SECRET_KEY = secrets.token_urlsafe(32)
    print("⚠️  WARNING: SECRET_KEY not set — using ephemeral random key (DEV ONLY)")
    print("❗ Set SECRET_KEY in .env for persistent sessions across restarts")
elif SECRET_KEY == "your-super-secret-key-change-it":
    raise ValueError(
        "🚨 SECURITY ERROR: Default SECRET_KEY detected!\n"
        "Generate a secure key with: python -c 'import secrets; print(secrets.token_urlsafe(32))'\n"
        "Then set it in .env: SECRET_KEY=<your-generated-key>"
    )
elif len(SECRET_KEY) < 32:
    print("⚠️  WARNING: SECRET_KEY is shorter than 32 characters — consider using a longer key")

ALGORITHM = os.getenv("ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "30"))

# Database URL - supports PostgreSQL (production) and SQLite (local dev)
# Railway will automatically set DATABASE_URL with PostgreSQL connection
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./buyv.db")

# Fix for Railway PostgreSQL URL (postgres:// -> postgresql://)
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME = os.getenv("CLOUDINARY_CLOUD_NAME", "")
CLOUDINARY_UPLOAD_PRESET = os.getenv("CLOUDINARY_UPLOAD_PRESET", "")

# CJ Dropshipping Keys
CJ_API_KEY = os.getenv("CJ_API_KEY", "")
CJ_ACCOUNT_ID = os.getenv("CJ_ACCOUNT_ID", "")
CJ_EMAIL = os.getenv("CJ_EMAIL", "")

# Stripe Configuration
STRIPE_SECRET_KEY = os.getenv("STRIPE_SECRET_KEY", "")
STRIPE_PUBLISHABLE_KEY = os.getenv("STRIPE_PUBLISHABLE_KEY", "")

# Payment Mock Mode
# Set MOCK_PAYMENTS=true in .env to bypass real Stripe calls (for testing commissions)
# Defaults to True when no Stripe key is configured, False in production
_mock_env = os.getenv("MOCK_PAYMENTS", "").lower()
if _mock_env in ("true", "1", "yes"):
    MOCK_PAYMENTS = True
elif _mock_env in ("false", "0", "no"):
    MOCK_PAYMENTS = False
else:
    # Auto-detect: mock if no Stripe key is configured
    MOCK_PAYMENTS = not bool(STRIPE_SECRET_KEY)