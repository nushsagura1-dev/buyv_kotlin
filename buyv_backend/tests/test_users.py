"""
BuyV Backend — User & Social Endpoint Tests

Covers:
  - GET  /users/me/stats
  - PUT  /users/me (update profile)
  - DELETE /users/me (delete account)
  - POST /users/me/fcm-token
  - GET  /users/search?query=...
  - GET  /users/{uid}
"""
import pytest
import uuid


# ════════════════════════════════════════════════
# USER PROFILE
# ════════════════════════════════════════════════

class TestUserProfile:
    def test_get_user_profile_by_uid(self, client, registered_user, auth_headers):
        _, reg_data = registered_user
        uid = reg_data["user"]["id"]
        resp = client.get(f"/users/{uid}", headers=auth_headers)
        assert resp.status_code == 200
        assert resp.json()["email"] == reg_data["user"]["email"]

    def test_get_nonexistent_user(self, client, auth_headers):
        resp = client.get(f"/users/nonexistent-uid-{uuid.uuid4().hex[:8]}", headers=auth_headers)
        assert resp.status_code == 404

    def test_update_profile(self, client, registered_user, auth_headers):
        _, reg_data = registered_user
        uid = reg_data["user"]["id"]
        resp = client.put(f"/users/{uid}", headers=auth_headers, json={
            "displayName": "Updated Name",
            "bio": "New bio text",
        })
        assert resp.status_code == 200

        # Verify update persisted
        me = client.get("/auth/me", headers=auth_headers)
        assert me.status_code == 200
        assert me.json()["displayName"] == "Updated Name"

    def test_update_profile_unauthenticated(self, client):
        resp = client.put("/users/some-uid", json={"displayName": "Hack"})
        assert resp.status_code == 401


# ════════════════════════════════════════════════
# USER STATS
# ════════════════════════════════════════════════

class TestUserStats:
    def test_get_my_stats(self, client, registered_user, auth_headers):
        _, reg_data = registered_user
        uid = reg_data["user"]["id"]
        resp = client.get(f"/users/{uid}/stats", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        # Should have follower/following counts
        assert "followersCount" in data or "followers_count" in data

    def test_get_stats_nonexistent_user(self, client):
        resp = client.get("/users/nonexistent-uid-xyz/stats")
        assert resp.status_code == 404


# ════════════════════════════════════════════════
# USER SEARCH
# ════════════════════════════════════════════════

class TestUserSearch:
    def test_search_users(self, client, auth_headers, registered_user):
        user_data, _ = registered_user
        username = user_data["username"]
        resp = client.get(f"/users/search?q={username}", headers=auth_headers)
        assert resp.status_code == 200
        results = resp.json()
        assert isinstance(results, list)

    def test_search_empty_query(self, client, auth_headers):
        resp = client.get("/users/search?q=zzz_nonexistent_user_zzz", headers=auth_headers)
        # Should return empty list
        assert resp.status_code == 200
        assert resp.json() == []


# ════════════════════════════════════════════════
# FCM TOKEN
# ════════════════════════════════════════════════

class TestFCMToken:
    def test_update_fcm_token(self, client, registered_user, auth_headers):
        resp = client.post("/users/me/fcm-token", headers=auth_headers, json={
            "fcm_token": f"test-fcm-token-{uuid.uuid4().hex}",
        })
        assert resp.status_code == 200


# ════════════════════════════════════════════════
# DELETE ACCOUNT
# ════════════════════════════════════════════════

class TestDeleteAccount:
    def test_delete_account(self, client):
        """Register, then delete — should succeed and token should be invalid."""
        payload = {
            "email": f"delme_{uuid.uuid4().hex[:6]}@test.com",
            "password": "DeleteMe123!",
            "username": f"delme_{uuid.uuid4().hex[:6]}",
            "displayName": "Delete Me",
        }
        reg = client.post("/auth/register", json=payload)
        assert reg.status_code == 200
        token = reg.json()["access_token"]

        headers = {"Authorization": f"Bearer {token}"}
        resp = client.delete("/users/me", headers=headers)
        assert resp.status_code == 200

        # Token should no longer work
        me = client.get("/auth/me", headers=headers)
        assert me.status_code in (401, 404)

    def test_delete_account_unauthenticated(self, client):
        resp = client.delete("/users/me")
        assert resp.status_code == 401
