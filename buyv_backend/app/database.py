from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from .config import DATABASE_URL

# ── Driver normalization ───────────────────────────────────────────────────────
# Railway/Render sometimes provide mysql:// URLs → need PyMySQL driver
_db_url = DATABASE_URL
if _db_url.startswith("mysql://"):
    _db_url = _db_url.replace("mysql://", "mysql+pymysql://", 1)

# ── Connection arguments ───────────────────────────────────────────────────────
if _db_url.startswith("sqlite"):
    connect_args = {"check_same_thread": False}
elif _db_url.startswith(("postgresql", "postgres")):
    # Railway Postgres proxy works with SSL; prefer SSL but don't require it
    # so local dev without SSL still works.
    connect_args = {"sslmode": "prefer"}
else:
    connect_args = {}

engine = create_engine(
    _db_url,
    pool_pre_ping=True,      # Reconnect on stale connections
    pool_recycle=1800,       # Recycle connections every 30 min (Railway cuts idle at ~5 min)
    pool_size=5,
    max_overflow=10,
    echo=False,
    future=True,
    connect_args=connect_args,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, future=True)

class Base(DeclarativeBase):
    pass

# Dependency for FastAPI
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

