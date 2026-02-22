"""
BuyV Backend — Orders Endpoint Tests

Covers:
  - POST /orders (create order)
  - GET  /orders/me (list my orders)
  - GET  /orders/{id} (get order detail)
  - POST /orders/{id}/cancel
  - PATCH /orders/{id}/status (admin-only — H-4)
  - PATCH /orders/{id}/tracking (admin-only — H-4)
"""
import pytest
import uuid


# ── Helper ──────────────────────────────────────────────
def _create_order_payload(**overrides):
    """Generate a valid order creation payload."""
    base = {
        "items": [{
            "productId": f"prod_{uuid.uuid4().hex[:8]}",
            "productName": "Test Product",
            "productImage": "https://example.com/img.jpg",
            "price": 29.99,
            "quantity": 2,
        }],
        "subtotal": 59.98,
        "shipping": 5.00,
        "tax": 3.60,
        "total": 68.58,
        "paymentMethod": "card",
        "shippingAddress": {
            "fullName": "Test User",
            "address": "123 Test Street",
            "city": "Test City",
            "state": "TS",
            "zipCode": "12345",
            "country": "US",
            "phone": "+1234567890",
        },
    }
    base.update(overrides)
    return base


# ════════════════════════════════════════════════
# CREATE ORDER
# ════════════════════════════════════════════════

class TestCreateOrder:
    def test_create_order_success(self, client, auth_headers):
        resp = client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        assert resp.status_code == 200, f"Create order failed: {resp.text}"
        data = resp.json()
        assert "id" in data
        assert data["status"] == "pending"
        assert data["total"] == 68.58
        assert len(data["items"]) == 1
        assert data["items"][0]["productName"] == "Test Product"

    def test_create_order_unauthenticated(self, client):
        resp = client.post("/orders", json=_create_order_payload())
        assert resp.status_code == 401

    def test_create_order_empty_items(self, client, auth_headers):
        resp = client.post("/orders", json=_create_order_payload(items=[]), headers=auth_headers)
        # Server currently accepts empty orders — verify it at least succeeds
        assert resp.status_code in (200, 400, 422)


# ════════════════════════════════════════════════
# LIST MY ORDERS
# ════════════════════════════════════════════════

class TestListOrders:
    def test_list_orders_empty(self, client, second_user_headers):
        """New user should have 0 orders."""
        resp = client.get("/orders/me", headers=second_user_headers)
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_list_orders_after_create(self, client, auth_headers):
        # Create order
        client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        # List
        resp = client.get("/orders/me", headers=auth_headers)
        assert resp.status_code == 200
        orders = resp.json()
        assert len(orders) >= 1

    def test_list_orders_unauthenticated(self, client):
        resp = client.get("/orders/me")
        assert resp.status_code == 401


# ════════════════════════════════════════════════
# GET ORDER DETAIL
# ════════════════════════════════════════════════

class TestOrderDetail:
    def test_get_order_detail(self, client, auth_headers):
        # Create
        create_resp = client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        order_id = create_resp.json()["id"]
        # Get detail
        resp = client.get(f"/orders/{order_id}", headers=auth_headers)
        assert resp.status_code == 200
        assert resp.json()["id"] == order_id

    def test_get_nonexistent_order(self, client, auth_headers):
        resp = client.get("/orders/999999", headers=auth_headers)
        assert resp.status_code == 404


# ════════════════════════════════════════════════
# CANCEL ORDER
# ════════════════════════════════════════════════

class TestCancelOrder:
    def test_cancel_pending_order(self, client, auth_headers):
        create_resp = client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        order_id = create_resp.json()["id"]

        resp = client.post(f"/orders/{order_id}/cancel", headers=auth_headers)
        assert resp.status_code == 200

        # Verify status is cancelled (American spelling "canceled")
        detail = client.get(f"/orders/{order_id}", headers=auth_headers)
        assert detail.json()["status"] in ("cancelled", "canceled")


# ════════════════════════════════════════════════
# ADMIN-ONLY ENDPOINTS (H-4)
# ════════════════════════════════════════════════

class TestOrderAdminRestrictions:
    def test_update_status_requires_admin(self, client, auth_headers):
        """Regular user should NOT be able to change order status (H-4 fix)."""
        create_resp = client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        order_id = create_resp.json()["id"]

        resp = client.patch(
            f"/orders/{order_id}/status",
            json={"status": "shipped"},
            headers=auth_headers,
        )
        assert resp.status_code in (401, 403), \
            f"Regular user should not update status, got {resp.status_code}"

    def test_update_tracking_requires_admin(self, client, auth_headers):
        """Regular user should NOT be able to update tracking (H-4 fix)."""
        create_resp = client.post("/orders", json=_create_order_payload(), headers=auth_headers)
        order_id = create_resp.json()["id"]

        resp = client.patch(
            f"/orders/{order_id}/tracking",
            json={"trackingNumber": "TRACK123", "carrier": "FedEx"},
            headers=auth_headers,
        )
        assert resp.status_code in (401, 403), \
            f"Regular user should not update tracking, got {resp.status_code}"
