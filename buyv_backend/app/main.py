import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from .database import engine, Base
from .auth import router as auth_router
from .users import router as users_router
from .follows import router as follows_router
from .notifications import router as notifications_router
from .orders import router as orders_router
from .commissions import router as commissions_router
from .posts import router as posts_router
from .comments import router as comments_router
from .payments import router as payments_router
from .cleanup import router as cleanup_router
from .firebase_service import FirebaseService
from .marketplace.router import router as marketplace_router
from .tracking import router as tracking_router  # Phase 6
from .withdrawal import router as withdrawal_router  # Phase 8
from .admin_dashboard import router as admin_dashboard_router  # Phase 10 - Admin Mobile
from .admin_dashboard import orders_admin_router, commissions_admin_router  # Admin order/commission routes
from .blocked_users import router as blocked_users_router
from .reports import router as reports_router
from .sounds import router as sounds_router
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create tables if not exist
Base.metadata.create_all(bind=engine)

# Initialize Firebase on startup (will skip if credentials not found)
try:
    FirebaseService.initialize()
except Exception as e:
    logger.warning(f"Firebase initialization failed: {e}")

app = FastAPI(title="Buyv API", version="0.1.0")

# Rate Limiting Configuration
limiter = Limiter(key_func=get_remote_address, default_limits=["200/minute"])
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS Configuration
# Reads production origins from CORS_ORIGINS env var (comma-separated)
# Falls back to dev origins if not set
_dev_origins = [
    "http://localhost",
    "http://localhost:3000",
    "http://localhost:5173",
    "http://localhost:8080",
    "http://localhost:5500",
    "http://127.0.0.1",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5500",
    "http://10.0.2.2",      # Android Emulator default
    "http://10.0.2.2:8000",
    "http://10.0.3.2",      # Genymotion
    "http://10.0.3.2:8000",
]

# Production origins from environment (comma-separated)
# Example: CORS_ORIGINS=https://buyv.app,https://admin.buyv.app
_prod_origins_str = os.getenv("CORS_ORIGINS", "")
_prod_origins = [o.strip() for o in _prod_origins_str.split(",") if o.strip()] if _prod_origins_str else []

# Merge: production origins + dev origins (dev origins are harmless in prod)
origins = _prod_origins + _dev_origins

if _prod_origins:
    logger.info(f"CORS: {len(_prod_origins)} production origin(s) configured: {_prod_origins}")
else:
    logger.warning("CORS: No CORS_ORIGINS env var set — only dev localhost origins are active. "
                    "Set CORS_ORIGINS for production deployment.")

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,  # Restricted to specific origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Health & Version ──────────────────────────────────
APP_VERSION = "1.0.0"
IS_PRODUCTION = bool(os.getenv("RAILWAY_ENVIRONMENT") or os.getenv("RENDER_SERVICE_ID"))

@app.get("/health")
def health():
    return {
        "status": "ok",
        "version": APP_VERSION,
        "environment": "production" if IS_PRODUCTION else "development"
    }

app.include_router(auth_router)
app.include_router(users_router)
app.include_router(follows_router)
app.include_router(notifications_router)
app.include_router(orders_router)
app.include_router(commissions_router)
app.include_router(posts_router)
app.include_router(comments_router)
app.include_router(payments_router)
app.include_router(cleanup_router)
app.include_router(marketplace_router)
app.include_router(tracking_router)  # Phase 6: Tracking & Analytics
app.include_router(withdrawal_router)  # Phase 8: Withdrawal Management
app.include_router(admin_dashboard_router)  # Phase 10: Admin Mobile Dashboard
app.include_router(orders_admin_router)       # Admin order management (/api/orders/admin/...)
app.include_router(commissions_admin_router)  # Admin commission management (/api/commissions/admin/...)
app.include_router(blocked_users_router)
app.include_router(reports_router)
app.include_router(sounds_router)