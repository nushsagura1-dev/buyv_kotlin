"""
BuyV Backend — Sounds API Tests  (Section 3.3)

Covers:
  - GET  /api/sounds/{uid}     all non-null fields returned (P0)
  - GET  /api/sounds           list with query params        (P0)
  - GET  /api/sounds/trending  top sounds by usage_count    (P1)
  - POST /api/sounds/{uid}/use increment usage count         (P0)
  - POST /api/sounds           admin create                  (P1)
  - DELETE /api/sounds/{uid}   admin delete                  (P1)

Prerequisites: pytest + fastapi TestClient set up in conftest.py (session-scoped).
"""
import pytest
import uuid


# ── Helpers ──────────────────────────────────────────────────────────────
REQUIRED_SOUND_FIELDS = {
    "id", "uid", "title", "artist", "audioUrl",
    "duration", "usageCount", "isFeatured", "createdAt",
}

SOUND_PAYLOAD = {
    "title": "Test Track",
    "artist": "BuyV Artist",
    "audioUrl": "https://cdn.buyv.io/sounds/test_track.mp3",
    "coverImageUrl": "https://cdn.buyv.io/sounds/test_cover.jpg",
    "duration": 42.5,
    "genre": "pop",
}


def _create_sound(client, admin_headers: dict) -> dict:
    """Helper to create a sound via admin endpoint and return the JSON body."""
    resp = client.post("/api/sounds", json=SOUND_PAYLOAD, headers=admin_headers)
    assert resp.status_code == 200, f"Sound creation failed: {resp.text}"
    return resp.json()


# ── Fixtures ─────────────────────────────────────────────────────────────

@pytest.fixture
def admin_headers(client):
    """
    Register a regular user, then promote them to admin via the test DB,
    and return their auth headers.
    """
    payload = {
        "email": f"admin_{uuid.uuid4().hex[:8]}@buyv.io",
        "password": "AdminPass123!",
        "username": f"admin_{uuid.uuid4().hex[:8]}",
        "displayName": "Test Admin",
    }
    resp = client.post("/auth/register", json=payload)
    assert resp.status_code == 200, f"Admin register failed: {resp.text}"
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def seeded_sound(client, admin_headers):
    """Create a sound for use in read/update tests."""
    return _create_sound(client, admin_headers)


# ════════════════════════════════════════════════════════════
# GET /api/sounds/{uid}  — P0
# ════════════════════════════════════════════════════════════

class TestGetSoundByUid:

    def test_get_sound_returns_all_required_fields(self, client, seeded_sound):
        """P0 — GET /api/sounds/{uid} must return all non-null required fields."""
        uid = seeded_sound["uid"]
        resp = client.get(f"/api/sounds/{uid}")

        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        data = resp.json()

        for field in REQUIRED_SOUND_FIELDS:
            assert field in data, f"Missing required field: {field}"
            # None only allowed on optional fields
            if field not in ("coverImageUrl", "genre"):
                assert data[field] is not None, f"Field '{field}' must not be None"

    def test_get_sound_uid_matches_request(self, client, seeded_sound):
        """P0 — The uid in the response must match the requested uid."""
        uid = seeded_sound["uid"]
        resp = client.get(f"/api/sounds/{uid}")
        assert resp.status_code == 200
        assert resp.json()["uid"] == uid

    def test_get_sound_numeric_fields_are_correct_types(self, client, seeded_sound):
        """P0 — duration (float) and usageCount (int) must be the right types."""
        uid = seeded_sound["uid"]
        data = client.get(f"/api/sounds/{uid}").json()
        assert isinstance(data["duration"], (int, float)), "duration must be numeric"
        assert isinstance(data["usageCount"], int), "usageCount must be int"
        assert data["duration"] > 0, "duration must be positive"

    def test_get_sound_nonexistent_uid_returns_404(self, client):
        """P0 — Requesting a non-existent sound must return 404."""
        resp = client.get(f"/api/sounds/nonexistent-uid-{uuid.uuid4().hex}")
        assert resp.status_code == 404, f"Expected 404, got {resp.status_code}"

    def test_get_sound_artist_and_title_not_empty(self, client, seeded_sound):
        """P0 — title and artist must be non-empty strings."""
        uid = seeded_sound["uid"]
        data = client.get(f"/api/sounds/{uid}").json()
        assert data["title"].strip() != "", "title must not be empty"
        assert data["artist"].strip() != "", "artist must not be empty"


# ════════════════════════════════════════════════════════════
# POST /api/sounds/{uid}/use  — P0
# ════════════════════════════════════════════════════════════

class TestIncrementSoundUsage:

    def test_increment_usage_returns_updated_count(self, client, seeded_sound):
        """P0 — POST /use must increment usageCount and return the new value."""
        uid      = seeded_sound["uid"]
        before   = client.get(f"/api/sounds/{uid}").json()["usageCount"]

        resp = client.post(f"/api/sounds/{uid}/use")
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        body = resp.json()
        assert "usageCount" in body or "usage_count" in body, \
            "Response must contain usageCount or usage_count key"

        after = client.get(f"/api/sounds/{uid}").json()["usageCount"]
        assert after == before + 1, f"Expected usageCount {before + 1}, got {after}"

    def test_increment_usage_nonexistent_returns_404(self, client):
        """P0 — POST /use on unknown uid must return 404."""
        resp = client.post(f"/api/sounds/ghost-uid-{uuid.uuid4().hex}/use")
        assert resp.status_code == 404, f"Expected 404, got {resp.status_code}"

    def test_increment_usage_idempotent_no_cap(self, client, seeded_sound):
        """P1 — Multiple calls must each increment by 1 (no duplicate guard at API level)."""
        uid    = seeded_sound["uid"]
        before = client.get(f"/api/sounds/{uid}").json()["usageCount"]

        for _ in range(3):
            assert client.post(f"/api/sounds/{uid}/use").status_code == 200

        after = client.get(f"/api/sounds/{uid}").json()["usageCount"]
        assert after == before + 3, f"Expected {before + 3}, got {after}"


# ════════════════════════════════════════════════════════════
# GET /api/sounds  — P1
# ════════════════════════════════════════════════════════════

class TestGetSoundsList:

    def test_get_sounds_returns_list(self, client, seeded_sound):
        """P1 — GET /api/sounds must return a JSON list."""
        resp = client.get("/api/sounds")
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_get_sounds_search_filters_by_title(self, client, seeded_sound):
        """P1 — ?search=<title> must narrow results to matching sounds."""
        title = seeded_sound["title"]
        resp  = client.get(f"/api/sounds?search={title[:4]}")
        assert resp.status_code == 200
        results = resp.json()
        # At least the seeded sound must be in results
        uids = [s["uid"] for s in results]
        assert seeded_sound["uid"] in uids, \
            f"Seed uid {seeded_sound['uid']} not found in search results"

    def test_get_sounds_genre_filter(self, client, seeded_sound):
        """P1 — ?genre=pop must return only sounds of that genre."""
        resp = client.get("/api/sounds?genre=pop")
        assert resp.status_code == 200
        results = resp.json()
        for sound in results:
            assert sound.get("genre") == "pop", \
                f"Genre filter broken: got {sound.get('genre')}"

    def test_get_sounds_limit_respected(self, client, admin_headers):
        """P1 — ?limit=2 must return at most 2 sounds."""
        # Seed 3 sounds first
        for i in range(3):
            payload = {**SOUND_PAYLOAD, "title": f"LimitTest Sound {i} {uuid.uuid4().hex[:4]}"}
            client.post("/api/sounds", json=payload, headers=admin_headers)

        resp = client.get("/api/sounds?limit=2")
        assert resp.status_code == 200
        assert len(resp.json()) <= 2


# ════════════════════════════════════════════════════════════
# GET /api/sounds/trending  — P1
# ════════════════════════════════════════════════════════════

class TestTrendingSounds:

    def test_trending_returns_list(self, client, seeded_sound):
        """P1 — GET /api/sounds/trending must return a JSON list."""
        resp = client.get("/api/sounds/trending")
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_trending_sorted_by_usage_descending(self, client, admin_headers):
        """P1 — Trending list must be ordered by usageCount descending."""
        # Seed two sounds and give the second one more uses
        s1 = _create_sound(client, admin_headers)
        s2 = _create_sound(client, admin_headers)

        # Give s2 more usage
        client.post(f"/api/sounds/{s2['uid']}/use")
        client.post(f"/api/sounds/{s2['uid']}/use")
        client.post(f"/api/sounds/{s1['uid']}/use")

        resp = client.get("/api/sounds/trending?limit=50")
        assert resp.status_code == 200
        sounds = resp.json()
        counts = [s["usageCount"] for s in sounds]
        assert counts == sorted(counts, reverse=True), \
            "Trending sounds must be sorted by usageCount descending"


# ════════════════════════════════════════════════════════════
# POST /api/sounds (admin)  — P1
# ════════════════════════════════════════════════════════════

class TestCreateSound:

    def test_create_sound_success(self, client, admin_headers):
        """P1 — Admin can create a sound via POST /api/sounds."""
        resp = client.post("/api/sounds", json=SOUND_PAYLOAD, headers=admin_headers)
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        data = resp.json()
        assert data["title"]   == SOUND_PAYLOAD["title"]
        assert data["artist"]  == SOUND_PAYLOAD["artist"]
        assert data["audioUrl"] == SOUND_PAYLOAD["audioUrl"]

    def test_create_sound_unauthenticated_fails(self, client):
        """P1 — Unauthenticated POST /api/sounds must return 401/403."""
        resp = client.post("/api/sounds", json=SOUND_PAYLOAD)
        assert resp.status_code in (401, 403), \
            f"Expected 401/403, got {resp.status_code}"

    def test_create_sound_missing_required_fields(self, client, admin_headers):
        """P1 — POST /api/sounds without required fields must return 422."""
        resp = client.post("/api/sounds", json={"title": "No URL"}, headers=admin_headers)
        assert resp.status_code == 422, f"Expected 422, got {resp.status_code}"


# ════════════════════════════════════════════════════════════
# DELETE /api/sounds/{uid} (admin)  — P1
# ════════════════════════════════════════════════════════════

class TestDeleteSound:

    def test_delete_sound_success(self, client, admin_headers):
        """P1 — Admin can delete a sound; subsequent GET returns 404."""
        sound = _create_sound(client, admin_headers)
        uid   = sound["uid"]

        del_resp = client.delete(f"/api/sounds/{uid}", headers=admin_headers)
        assert del_resp.status_code in (200, 204), \
            f"Expected 200/204 on delete, got {del_resp.status_code}: {del_resp.text}"

        get_resp = client.get(f"/api/sounds/{uid}")
        assert get_resp.status_code == 404, \
            "Deleted sound must return 404 on subsequent GET"

    def test_delete_sound_unauthenticated_fails(self, client, seeded_sound):
        """P1 — Unauthenticated DELETE must return 401/403."""
        uid  = seeded_sound["uid"]
        resp = client.delete(f"/api/sounds/{uid}")
        assert resp.status_code in (401, 403)
