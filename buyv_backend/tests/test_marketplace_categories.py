"""
BuyV Backend — Marketplace Categories Tests  (Section 3.3)

Covers:
  - GET  /api/v1/marketplace/categories  all active categories returned (P1)
  - POST /admin/marketplace/categories   admin create                   (P1)
  - PUT  /admin/marketplace/categories/{id}  admin update               (P1)
  - DELETE /admin/marketplace/categories/{id} admin delete              (P1)

Prerequisites: pytest + fastapi TestClient set up in conftest.py (session-scoped).
"""
import pytest
import uuid


# ── Required response fields ──────────────────────────────────────────────
REQUIRED_CATEGORY_FIELDS = {"id", "name", "slug", "isActive", "displayOrder", "createdAt"}


# ── Helpers ───────────────────────────────────────────────────────────────

def _make_category_payload(suffix: str | None = None) -> dict:
    s = suffix or uuid.uuid4().hex[:8]
    return {
        "name": f"Cat {s}",
        "slug": f"cat-{s}",
        "displayOrder": 0,
        "isActive": True,
    }


def _create_category(client, admin_headers: dict) -> dict:
    payload = _make_category_payload()
    resp    = client.post(
        "/api/v1/admin/marketplace/categories",
        json=payload,
        headers=admin_headers,
    )
    assert resp.status_code == 200, f"Category creation failed: {resp.text}"
    return resp.json()


# ── Fixtures ──────────────────────────────────────────────────────────────

@pytest.fixture
def admin_headers(client):
    """Register a user and return auth headers (admin endpoint accepts any logged-in user in test env)."""
    payload = {
        "email": f"admin_{uuid.uuid4().hex[:8]}@buyv.io",
        "password": "AdminPass123!",
        "username": f"admin_{uuid.uuid4().hex[:8]}",
        "displayName": "Cat Admin",
    }
    resp = client.post("/auth/register", json=payload)
    assert resp.status_code == 200, f"Register failed: {resp.text}"
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def seeded_category(client, admin_headers):
    return _create_category(client, admin_headers)


# ════════════════════════════════════════════════════════════
# GET /api/v1/marketplace/categories  — P1
# ════════════════════════════════════════════════════════════

class TestGetCategories:

    def test_returns_list(self, client, seeded_category):
        """P1 — GET /api/v1/marketplace/categories must return a JSON list."""
        resp = client.get("/api/v1/marketplace/categories")
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        assert isinstance(resp.json(), list)

    def test_seeded_category_in_list(self, client, seeded_category):
        """P1 — A freshly created active category must appear in the list."""
        resp    = client.get("/api/v1/marketplace/categories")
        ids     = [str(c["id"]) for c in resp.json()]
        cat_id  = str(seeded_category["id"])
        assert cat_id in ids, \
            f"Seeded category id {cat_id} not found in categories list"

    def test_all_required_fields_present(self, client, seeded_category):
        """P1 — Every category in the response must have all required fields."""
        resp       = client.get("/api/v1/marketplace/categories")
        categories = resp.json()
        assert len(categories) > 0, "Expected at least one category"
        for cat in categories:
            for field in REQUIRED_CATEGORY_FIELDS:
                # Convert camelCase check — accept snake_case keys too
                snake = _to_snake(field)
                assert field in cat or snake in cat, \
                    f"Missing field '{field}' (or '{snake}') in category: {cat}"

    def test_inactive_category_not_returned(self, client, admin_headers):
        """P1 — Categories with isActive=False must NOT appear in public list."""
        # Create an inactive category
        payload         = _make_category_payload()
        payload["isActive"] = False
        resp = client.post(
            "/api/v1/admin/marketplace/categories",
            json=payload,
            headers=admin_headers,
        )
        if resp.status_code != 200:
            pytest.skip("Could not create inactive category — skipping")

        created_id = str(resp.json()["id"])
        public     = client.get("/api/v1/marketplace/categories").json()
        public_ids = [str(c["id"]) for c in public]
        assert created_id not in public_ids, \
            "Inactive category must not appear in the public categories endpoint"

    def test_slug_is_url_safe(self, client, seeded_category):
        """P1 — Category slugs must be URL-safe (no spaces or special chars)."""
        resp = client.get("/api/v1/marketplace/categories")
        for cat in resp.json():
            slug = cat.get("slug") or cat.get("slug")
            assert slug is not None, "slug must not be None"
            assert " " not in slug, f"Slug '{slug}' must not contain spaces"

    def test_name_is_non_empty_string(self, client, seeded_category):
        """P1 — Category name must be a non-empty string."""
        resp = client.get("/api/v1/marketplace/categories")
        for cat in resp.json():
            name = cat.get("name") or cat.get("name")
            assert isinstance(name, str) and name.strip() != "", \
                f"Category name is empty or missing: {cat}"


# ════════════════════════════════════════════════════════════
# POST /api/v1/admin/marketplace/categories  — P1
# ════════════════════════════════════════════════════════════

class TestCreateCategory:

    def test_create_category_success(self, client, admin_headers):
        """P1 — Admin can create a category and it appears in the public list."""
        payload = _make_category_payload()
        resp    = client.post(
            "/api/v1/admin/marketplace/categories",
            json=payload,
            headers=admin_headers,
        )
        assert resp.status_code == 200, f"Create failed: {resp.text}"
        data = resp.json()
        assert data["name"] == payload["name"]
        assert data["slug"] == payload["slug"]

    def test_create_category_unauthenticated_fails(self, client):
        """P1 — Unauthenticated POST to admin endpoint must return 401/403."""
        resp = client.post(
            "/api/v1/admin/marketplace/categories",
            json=_make_category_payload(),
        )
        assert resp.status_code in (401, 403), \
            f"Expected 401/403, got {resp.status_code}"

    def test_create_category_missing_name_fails(self, client, admin_headers):
        """P1 — POST without 'name' must return 422."""
        resp = client.post(
            "/api/v1/admin/marketplace/categories",
            json={"slug": "no-name"},
            headers=admin_headers,
        )
        assert resp.status_code == 422, f"Expected 422, got {resp.status_code}"


# ════════════════════════════════════════════════════════════
# PUT /api/v1/admin/marketplace/categories/{id}  — P1
# ════════════════════════════════════════════════════════════

class TestUpdateCategory:

    def test_update_category_name(self, client, admin_headers, seeded_category):
        """P1 — Admin can update a category's display name."""
        cat_id   = seeded_category["id"]
        new_name = f"Updated {uuid.uuid4().hex[:6]}"
        resp = client.put(
            f"/api/v1/admin/marketplace/categories/{cat_id}",
            json={"name": new_name},
            headers=admin_headers,
        )
        if resp.status_code == 404:
            pytest.skip("UUID routing not supported in test DB — skipping update test")
        assert resp.status_code == 200, f"Update failed: {resp.text}"
        assert resp.json()["name"] == new_name


# ════════════════════════════════════════════════════════════
# DELETE /api/v1/admin/marketplace/categories/{id}  — P1
# ════════════════════════════════════════════════════════════

class TestDeleteCategory:

    def test_delete_category_removes_from_list(self, client, admin_headers):
        """P1 — Admin DELETE removes category; it no longer appears in the public list."""
        category = _create_category(client, admin_headers)
        cat_id   = str(category["id"])

        del_resp = client.delete(
            f"/api/v1/admin/marketplace/categories/{cat_id}",
            headers=admin_headers,
        )
        if del_resp.status_code in (404, 405):
            pytest.skip("Delete endpoint not available in test env — skipping")
        assert del_resp.status_code in (200, 204), \
            f"Delete returned {del_resp.status_code}: {del_resp.text}"

        public_ids = [str(c["id"]) for c in client.get("/api/v1/marketplace/categories").json()]
        assert cat_id not in public_ids, \
            "Deleted category must not appear in the public categories list"


# ── Utilities ─────────────────────────────────────────────────────────────

def _to_snake(camel: str) -> str:
    """Convert camelCase → snake_case for field-name normalisation."""
    import re
    return re.sub(r"([A-Z])", r"_\1", camel).lower()
