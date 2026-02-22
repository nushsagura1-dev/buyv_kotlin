from fastapi import APIRouter, HTTPException, Depends
import stripe
import uuid
from pydantic import BaseModel
from .config import STRIPE_SECRET_KEY, MOCK_PAYMENTS
from .auth import get_current_user
from .models import User
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/payments", tags=["payments"])

stripe.api_key = STRIPE_SECRET_KEY

if MOCK_PAYMENTS:
    logger.warning(
        "⚠️  MOCK_PAYMENTS mode is ACTIVE — Stripe calls are bypassed. "
        "Set MOCK_PAYMENTS=false for production."
    )

class PaymentIntentRequest(BaseModel):
    amount: int  # Amount in cents
    currency: str = "usd"

class PaymentIntentResponse(BaseModel):
    clientSecret: str
    ephemeralKey: str
    customer: str
    publishableKey: str = ""  # Optional, client might need it
    paymentIntentId: str = ""  # PaymentIntent ID for order verification

@router.get("/mock-status")
def get_mock_status():
    """Check if mock payment mode is active."""
    return {"mock_payments": MOCK_PAYMENTS}


@router.post("/create-payment-intent", response_model=PaymentIntentResponse)
def create_payment_intent(
    payload: PaymentIntentRequest,
    current_user: User = Depends(get_current_user)
):
    # ── MOCK MODE ──────────────────────────────────────────
    # Returns fake credentials so the full order/commission flow can be tested
    # without a real Stripe account or card details.
    if MOCK_PAYMENTS:
        mock_pi_id = f"pi_mock_{uuid.uuid4().hex[:16]}"
        mock_customer_id = f"cus_mock_{uuid.uuid4().hex[:12]}"
        logger.info(
            f"[MOCK] Returning fake PaymentIntent {mock_pi_id} "
            f"for user {current_user.uid} (amount={payload.amount} {payload.currency})"
        )
        return PaymentIntentResponse(
            clientSecret=f"{mock_pi_id}_secret_mock",
            ephemeralKey=f"ek_mock_{uuid.uuid4().hex[:16]}",
            customer=mock_customer_id,
            paymentIntentId=mock_pi_id
        )

    # ── REAL STRIPE MODE ───────────────────────────────────
    try:
        # 1. Search for existing customer by email
        customers = stripe.Customer.list(email=current_user.email, limit=1).data
        if customers:
            customer = customers[0]
        else:
            # 2. Create new customer if not exists
            customer = stripe.Customer.create(
                email=current_user.email,
                name=current_user.display_name,
                metadata={"uid": current_user.uid}
            )

        # 3. Create Ephemeral Key (required for Stripe Payment Sheet)
        ephemeral_key = stripe.EphemeralKey.create(
            customer=customer.id,
            stripe_version="2023-10-16"
        )

        # 4. Create Payment Intent
        payment_intent = stripe.PaymentIntent.create(
            amount=payload.amount,
            currency=payload.currency,
            customer=customer.id,
            automatic_payment_methods={"enabled": True},
        )

        return PaymentIntentResponse(
            clientSecret=payment_intent.client_secret,
            ephemeralKey=ephemeral_key.secret,
            customer=customer.id,
            paymentIntentId=payment_intent.id
        )

    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
