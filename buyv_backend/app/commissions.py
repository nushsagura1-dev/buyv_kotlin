from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
import json
from datetime import datetime

from .database import get_db
from .models import User, Commission
from .auth import get_current_user, get_current_admin_user
from .schemas import CommissionOut, StatusUpdate

router = APIRouter(prefix="/commissions", tags=["commissions"])


def _map_commission_out(row: Commission, db: Session) -> dict:
    # Resolve user UID
    uid = None
    if row.user_id:
        u = db.query(User).filter(User.id == row.user_id).first()
        uid = u.uid if u else None
    if not uid:
        uid = row.user_uid

    try:
        metadata = json.loads(row.metadata_json) if row.metadata_json else None
    except Exception:
        metadata = None

    return {
        "id": row.id,
        "userId": uid,
        "orderId": row.order_id,  # int — matches CommissionOut schema
        "orderItemId": row.order_item_id,  # int | None — matches CommissionOut schema
        "productId": row.product_id,
        "productName": row.product_name,
        "productPrice": row.product_price,
        "commissionRate": row.commission_rate,
        "commissionAmount": row.commission_amount,
        "status": row.status,
        "createdAt": row.created_at,
        "updatedAt": row.updated_at,
        "paidAt": row.paid_at,
        "metadata": metadata,
    }


@router.get("/me", response_model=list[CommissionOut])
@router.get("/user/me", response_model=list[CommissionOut])
def list_my_commissions(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    rows = (
        db.query(Commission)
        .filter((Commission.user_id == current_user.id) | (Commission.user_uid == current_user.uid))
        .order_by(Commission.created_at.desc())
        .all()
    )
    return [_map_commission_out(row, db) for row in rows]


@router.post("/{commission_id}/status")
def update_commission_status(commission_id: int, payload: StatusUpdate, db: Session = Depends(get_db), admin: User = Depends(get_current_admin_user)):
    row = db.query(Commission).filter(Commission.id == commission_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Commission not found")
    row.status = payload.status
    if payload.status.lower() == "paid":
        row.paid_at = datetime.utcnow()
    row.updated_at = datetime.utcnow()
    db.add(row)
    db.commit()
    return {"status": "ok"}