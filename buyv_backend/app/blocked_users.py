"""
Blocked Users API Endpoints
Allows users to block/unblock other users and retrieve their block list.
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import and_
from datetime import datetime
from typing import List

from .database import get_db
from .models import User, BlockedUser
from .auth import get_current_user
from .schemas import CamelModel

router = APIRouter(prefix="/api/users", tags=["Blocked Users"])


# ============ Response Schemas ============

class BlockedUserOut(CamelModel):
    id: int
    blocked_uid: str
    blocked_username: str
    blocked_display_name: str
    blocked_profile_image: str | None = None
    created_at: datetime


class BlockUserRequest(CamelModel):
    user_id: str  # UID of the user to block


# ============ Endpoints ============

@router.get("/me/blocked", response_model=List[BlockedUserOut])
def get_blocked_users(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get list of users blocked by the current user."""
    blocks = (
        db.query(BlockedUser)
        .filter(BlockedUser.blocker_uid == current_user.uid)
        .order_by(BlockedUser.created_at.desc())
        .all()
    )

    result = []
    for block in blocks:
        blocked_user = db.query(User).filter(User.uid == block.blocked_uid).first()
        if blocked_user:
            result.append(BlockedUserOut(
                id=block.id,
                blocked_uid=blocked_user.uid,
                blocked_username=blocked_user.username,
                blocked_display_name=blocked_user.display_name,
                blocked_profile_image=blocked_user.profile_image_url,
                created_at=block.created_at
            ))
    return result


@router.post("/me/blocked", response_model=BlockedUserOut)
def block_user(
    request: BlockUserRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Block a user. Cannot block yourself."""
    if request.user_id == current_user.uid:
        raise HTTPException(status_code=400, detail="Cannot block yourself")

    # Check if target user exists
    target_user = db.query(User).filter(User.uid == request.user_id).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="User not found")

    # Check if already blocked
    existing = db.query(BlockedUser).filter(
        and_(
            BlockedUser.blocker_uid == current_user.uid,
            BlockedUser.blocked_uid == request.user_id
        )
    ).first()
    if existing:
        raise HTTPException(status_code=409, detail="User already blocked")

    block = BlockedUser(
        blocker_uid=current_user.uid,
        blocked_uid=request.user_id
    )
    db.add(block)
    db.commit()
    db.refresh(block)

    return BlockedUserOut(
        id=block.id,
        blocked_uid=target_user.uid,
        blocked_username=target_user.username,
        blocked_display_name=target_user.display_name,
        blocked_profile_image=target_user.profile_image_url,
        created_at=block.created_at
    )


@router.delete("/me/blocked/{user_uid}")
def unblock_user(
    user_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Unblock a user."""
    block = db.query(BlockedUser).filter(
        and_(
            BlockedUser.blocker_uid == current_user.uid,
            BlockedUser.blocked_uid == user_uid
        )
    ).first()
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")

    db.delete(block)
    db.commit()
    return {"message": "User unblocked successfully"}


@router.get("/me/blocked/{user_uid}/status")
def check_block_status(
    user_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Check if a user is blocked."""
    block = db.query(BlockedUser).filter(
        and_(
            BlockedUser.blocker_uid == current_user.uid,
            BlockedUser.blocked_uid == user_uid
        )
    ).first()
    return {"is_blocked": block is not None}
