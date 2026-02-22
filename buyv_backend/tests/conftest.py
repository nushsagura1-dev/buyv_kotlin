"""
BuyV Backend — Shared Test Fixtures (conftest.py)

Provides:
  - test_db: In-memory SQLite DB per test session
  - client: FastAPI TestClient bound to test DB
  - auth_headers: Helper to register + get Bearer token
  - admin_headers: Helper to get admin Bearer token
"""
import os
import pytest
import uuid
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, JSON
from sqlalchemy.orm import sessionmaker

# Force SQLite for tests BEFORE importing app modules
os.environ["DATABASE_URL"] = "sqlite:///./test_buyv.db"

# ── JSONB → JSON mapping for SQLite ────────────────────
# Marketplace models use PostgreSQL-specific JSONB; map it to JSON for SQLite.
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.compiler import compiles

@compiles(JSONB, "sqlite")
def compile_jsonb_sqlite(type_, compiler, **kw):
    return "JSON"

from app.database import Base, get_db
from app.main import app

# ── Disable rate limiting for tests ─────────────────────
app.state.limiter.enabled = False
app.state.limiter.reset()
# Also disable the limiter instance used in auth.py decorators
from app.auth import limiter as auth_limiter
auth_limiter.enabled = False
auth_limiter.reset()

# ── Test DB Engine ──────────────────────────────────────
TEST_DATABASE_URL = "sqlite:///./test_buyv.db"

test_engine = create_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
)
TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)


def override_get_db():
    db = TestSessionLocal()
    try:
        yield db
    finally:
        db.close()


# Override the dependency globally
app.dependency_overrides[get_db] = override_get_db


# ── DB lifecycle ────────────────────────────────────────
@pytest.fixture(scope="session", autouse=True)
def setup_database():
    """Create all tables once for the entire test session."""
    Base.metadata.create_all(bind=test_engine)
    yield
    Base.metadata.drop_all(bind=test_engine)
    # Clean up test DB file (ignore errors on Windows file locks)
    import pathlib
    db_file = pathlib.Path("./test_buyv.db")
    if db_file.exists():
        try:
            db_file.unlink()
        except PermissionError:
            pass  # Windows file locks — will be cleaned on next run


@pytest.fixture(scope="session")
def client():
    """FastAPI TestClient — session-scoped for performance."""
    with TestClient(app) as c:
        yield c


# ── Auth helpers ────────────────────────────────────────
def _unique_email():
    return f"test_{uuid.uuid4().hex[:8]}@test.com"


def _unique_username():
    return f"user_{uuid.uuid4().hex[:8]}"


@pytest.fixture
def test_user_data():
    """Generate unique user registration payload."""
    return {
        "email": _unique_email(),
        "password": "TestPass123!",
        "username": _unique_username(),
        "displayName": "Test User",
    }


@pytest.fixture
def registered_user(client, test_user_data):
    """Register a user and return (user_data, response_json)."""
    resp = client.post("/auth/register", json=test_user_data)
    assert resp.status_code == 200, f"Registration failed: {resp.text}"
    return test_user_data, resp.json()


@pytest.fixture
def auth_headers(registered_user):
    """Return Authorization headers for a newly registered user."""
    _, data = registered_user
    token = data["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def second_user_headers(client):
    """Register and authenticate a second user — useful for isolation tests."""
    payload = {
        "email": _unique_email(),
        "password": "SecondPass123!",
        "username": _unique_username(),
        "displayName": "Second User",
    }
    resp = client.post("/auth/register", json=payload)
    assert resp.status_code == 200
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
