"""
BuyV Backend — Auth Endpoint Tests

Covers:
  - POST /auth/register (success, duplicate email, duplicate username, weak password)
  - POST /auth/login (success, wrong password, nonexistent user)
  - GET  /auth/me (authenticated, unauthenticated)
  - POST /auth/refresh (valid, invalid, expired)
  - POST /auth/logout + token blacklisting (H-3)
  - POST /auth/change-password
  - POST /auth/admin/login
"""
import pytest
import uuid


# ════════════════════════════════════════════════
# REGISTER
# ════════════════════════════════════════════════

class TestRegister:
    def test_register_success(self, client, test_user_data):
        resp = client.post("/auth/register", json=test_user_data)
        assert resp.status_code == 200
        data = resp.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"
        assert data["expires_in"] > 0
        assert data["user"]["email"] == test_user_data["email"]
        assert data["user"]["displayName"] == test_user_data["displayName"]

    def test_register_duplicate_email(self, client, registered_user):
        user_data, _ = registered_user
        # Try to register same email again with different username
        payload = {**user_data, "username": f"other_{uuid.uuid4().hex[:6]}"}
        resp = client.post("/auth/register", json=payload)
        assert resp.status_code == 400
        assert "email" in resp.json()["detail"].lower() or "already" in resp.json()["detail"].lower()

    def test_register_duplicate_username(self, client, registered_user):
        user_data, _ = registered_user
        payload = {**user_data, "email": f"other_{uuid.uuid4().hex[:6]}@test.com"}
        resp = client.post("/auth/register", json=payload)
        assert resp.status_code == 400
        assert "username" in resp.json()["detail"].lower() or "taken" in resp.json()["detail"].lower()

    def test_register_short_password(self, client):
        payload = {
            "email": f"short_{uuid.uuid4().hex[:6]}@test.com",
            "password": "abc",  # < 8 chars
            "username": f"short_{uuid.uuid4().hex[:6]}",
            "displayName": "Short Password",
        }
        resp = client.post("/auth/register", json=payload)
        # Backend enforces min 8 chars (H-6 fix)
        # Could be 400 or 422 depending on validation layer
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}: {resp.text}"

    def test_register_invalid_email(self, client):
        payload = {
            "email": "not-an-email",
            "password": "ValidPass123!",
            "username": f"invalid_{uuid.uuid4().hex[:6]}",
            "displayName": "Invalid Email",
        }
        resp = client.post("/auth/register", json=payload)
        assert resp.status_code == 422  # Pydantic EmailStr validation


# ════════════════════════════════════════════════
# LOGIN
# ════════════════════════════════════════════════

class TestLogin:
    def test_login_success(self, client, registered_user):
        user_data, _ = registered_user
        resp = client.post("/auth/login", json={
            "email": user_data["email"],
            "password": user_data["password"],
        })
        assert resp.status_code == 200
        data = resp.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["user"]["email"] == user_data["email"]

    def test_login_wrong_password(self, client, registered_user):
        user_data, _ = registered_user
        resp = client.post("/auth/login", json={
            "email": user_data["email"],
            "password": "WrongPassword123!",
        })
        assert resp.status_code == 401
        assert "invalid" in resp.json()["detail"].lower()

    def test_login_nonexistent_user(self, client):
        resp = client.post("/auth/login", json={
            "email": f"noexist_{uuid.uuid4().hex}@test.com",
            "password": "Whatever123!",
        })
        assert resp.status_code == 401


# ════════════════════════════════════════════════
# GET /auth/me
# ════════════════════════════════════════════════

class TestMe:
    def test_me_authenticated(self, client, auth_headers, registered_user):
        user_data, _ = registered_user
        resp = client.get("/auth/me", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["email"] == user_data["email"]

    def test_me_no_token(self, client):
        resp = client.get("/auth/me")
        assert resp.status_code == 401

    def test_me_invalid_token(self, client):
        resp = client.get("/auth/me", headers={"Authorization": "Bearer invalid.token.here"})
        assert resp.status_code == 401


# ════════════════════════════════════════════════
# POST /auth/refresh
# ════════════════════════════════════════════════

class TestRefresh:
    def test_refresh_success(self, client, registered_user):
        _, data = registered_user
        refresh_token = data["refresh_token"]
        resp = client.post("/auth/refresh", json={"refresh_token": refresh_token})
        assert resp.status_code == 200
        new_data = resp.json()
        assert "access_token" in new_data
        assert new_data["access_token"] != data["access_token"]  # New token issued

    def test_refresh_invalid_token(self, client):
        resp = client.post("/auth/refresh", json={"refresh_token": "garbage.token.value"})
        assert resp.status_code == 401

    def test_refresh_with_access_token_fails(self, client, registered_user):
        """Refresh endpoint should reject access tokens (wrong type)."""
        _, data = registered_user
        resp = client.post("/auth/refresh", json={"refresh_token": data["access_token"]})
        assert resp.status_code == 401


# ════════════════════════════════════════════════
# LOGOUT & TOKEN BLACKLISTING (H-3)
# ════════════════════════════════════════════════

class TestLogout:
    def test_logout_success(self, client, auth_headers):
        resp = client.post("/auth/logout", headers=auth_headers)
        assert resp.status_code == 200
        assert "message" in resp.json()

    def test_logout_no_token(self, client):
        resp = client.post("/auth/logout")
        assert resp.status_code == 401

    def test_token_blacklisted_after_logout(self, client):
        """After logout, the same token should be rejected on /auth/me."""
        # Register fresh user
        payload = {
            "email": f"blacklist_{uuid.uuid4().hex[:6]}@test.com",
            "password": "BlacklistTest123!",
            "username": f"bl_{uuid.uuid4().hex[:6]}",
            "displayName": "Blacklist Test",
        }
        reg_resp = client.post("/auth/register", json=payload)
        assert reg_resp.status_code == 200
        token = reg_resp.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Verify it works before logout
        me_resp = client.get("/auth/me", headers=headers)
        assert me_resp.status_code == 200

        # Logout
        logout_resp = client.post("/auth/logout", headers=headers)
        assert logout_resp.status_code == 200

        # Token should now be rejected
        me_after = client.get("/auth/me", headers=headers)
        assert me_after.status_code == 401, "Token should be blacklisted after logout"

    def test_double_logout_idempotent(self, client):
        """Calling logout twice should not error."""
        payload = {
            "email": f"dbl_{uuid.uuid4().hex[:6]}@test.com",
            "password": "DoubleLogout123!",
            "username": f"dbl_{uuid.uuid4().hex[:6]}",
            "displayName": "Double Logout",
        }
        reg = client.post("/auth/register", json=payload)
        token = reg.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        resp1 = client.post("/auth/logout", headers=headers)
        assert resp1.status_code == 200

        resp2 = client.post("/auth/logout", headers=headers)
        assert resp2.status_code == 200  # Idempotent, not error


# ════════════════════════════════════════════════
# CHANGE PASSWORD
# ════════════════════════════════════════════════

class TestChangePassword:
    def test_change_password_success(self, client, registered_user):
        user_data, reg_data = registered_user
        headers = {"Authorization": f"Bearer {reg_data['access_token']}"}
        resp = client.post("/auth/change-password", headers=headers, json={
            "current_password": user_data["password"],
            "new_password": "NewPassword456!",
        })
        assert resp.status_code == 200

        # Old password should no longer work
        login_resp = client.post("/auth/login", json={
            "email": user_data["email"],
            "password": user_data["password"],
        })
        assert login_resp.status_code == 401

        # New password should work
        login_new = client.post("/auth/login", json={
            "email": user_data["email"],
            "password": "NewPassword456!",
        })
        assert login_new.status_code == 200

    def test_change_password_wrong_current(self, client, auth_headers):
        resp = client.post("/auth/change-password", headers=auth_headers, json={
            "current_password": "WrongCurrent123!",
            "new_password": "NewPassword456!",
        })
        assert resp.status_code in (400, 401, 403)


# ════════════════════════════════════════════════
# ADMIN LOGIN
# ════════════════════════════════════════════════

class TestAdminLogin:
    def test_admin_login_as_regular_user_fails(self, client, registered_user):
        """Regular users should not be able to login via admin endpoint."""
        user_data, _ = registered_user
        resp = client.post("/auth/admin/login", json={
            "email": user_data["email"],
            "password": user_data["password"],
        })
        # Should fail — user is not admin
        assert resp.status_code in (401, 403)
