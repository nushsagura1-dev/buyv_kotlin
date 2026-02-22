from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List
from pydantic import BaseModel
from .database import get_db
from . import models
from .schemas import UserOut, UserUpdate, UserStats
from .auth import get_current_user
import json

router = APIRouter(prefix="/users", tags=["users"])


class FCMTokenUpdate(BaseModel):
    """Schema for updating FCM token"""
    fcm_token: str


def user_to_out(user: models.User) -> UserOut:
    try:
        interests = json.loads(user.interests) if user.interests else []
    except Exception:
        interests = []
    try:
        settings = json.loads(user.settings) if user.settings else None
    except Exception:
        settings = None

    # Pass in fields by name (snake_case) or alias (CamelModel handles both if populate_by_name=True)
    # Since UserOut fields are snake_case (display_name), we can pass display_name=...
    return UserOut(
        id=user.uid,
        email=user.email,
        username=user.username,
        display_name=user.display_name,
        profile_image_url=user.profile_image_url,
        bio=user.bio,
        followers_count=user.followers_count,
        following_count=user.following_count,
        reels_count=user.reels_count,
        is_verified=user.is_verified,
        created_at=user.created_at,
        updated_at=user.updated_at,
        interests=interests,
        settings=settings,
    )

@router.get("/search", response_model=List[UserOut])
def search_users(
    q: str = Query(..., min_length=1, description="Search query"),
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db)
):
    """Search users by username or display name with pagination"""
    search_pattern = f"%{q}%"
    
    users = (
        db.query(models.User)
        .filter(
            (models.User.username.ilike(search_pattern)) |
            (models.User.display_name.ilike(search_pattern))
        )
        .offset(offset)
        .limit(limit)
        .all()
    )
    
    return [user_to_out(user) for user in users]


@router.get("/{uid}", response_model=UserOut)
def get_user(uid: str, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user_to_out(user)

@router.get("/{uid}/stats", response_model=UserStats)
def get_user_stats(uid: str, db: Session = Depends(get_db)):
    """Get summarized user statistics in ONE call"""
    user = db.query(models.User).filter(models.User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Efficiently count reels and products
    reels_count = db.query(models.Post).filter(
        models.Post.user_id == user.id, 
        models.Post.type == "reel"
    ).count()
    
    products_count = db.query(models.Post).filter(
        models.Post.user_id == user.id, 
        models.Post.type == "product"
    ).count()

    # Calculate total likes (sum of likes on all user's posts)
    from sqlalchemy import func
    total_likes = db.query(func.sum(models.Post.likes_count)).filter(
        models.Post.user_id == user.id
    ).scalar() or 0

    # Count bookmarked posts
    saved_posts_count = db.query(models.PostBookmark).filter(
        models.PostBookmark.user_id == user.id
    ).count()

    return UserStats(
        followers_count=user.followers_count,
        following_count=user.following_count,
        reels_count=reels_count,
        products_count=products_count,
        total_likes=total_likes,
        saved_posts_count=saved_posts_count
    )

@router.put("/{uid}", response_model=UserOut)
def update_user(uid: str, payload: UserUpdate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    # Security: only allow users to update their own profile
    if current_user.uid != uid:
        raise HTTPException(status_code=403, detail="You can only update your own profile")
    
    user = db.query(models.User).filter(models.User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Payload fields are snake_case in struct (display_name), but input was CamelCase (displayName)
    if payload.display_name is not None:
        user.display_name = payload.display_name
    if payload.profile_image_url is not None:
        user.profile_image_url = payload.profile_image_url
    if payload.bio is not None:
        user.bio = payload.bio
    if payload.interests is not None:
        user.interests = json.dumps(payload.interests)
    if payload.settings is not None:
        user.settings = json.dumps(payload.settings)

    db.add(user)
    db.commit()
    db.refresh(user)

    return user_to_out(user)


@router.post("/me/fcm-token")
def update_fcm_token(
    payload: FCMTokenUpdate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Update user's FCM (Firebase Cloud Messaging) token for push notifications.
    This token is used to send push notifications to the user's device.
    """
    current_user.fcm_token = payload.fcm_token
    db.commit()
    return {
        "message": "FCM token updated successfully",
        "user_id": current_user.uid
    }


@router.get("/me/promoter-status")
def get_promoter_status(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Check if the current user has promoter status (dynamic, not a role).
    A user becomes a promoter when they promote their first product,
    which creates a PromoterWallet entry in the database.
    
    Returns:
        - is_promoter (bool): True if PromoterWallet exists
        - wallet (dict | None): Wallet details if promoter, None otherwise
    """
    # Check if user has a PromoterWallet (using uid not id)
    wallet = db.query(models.PromoterWallet).filter(
        models.PromoterWallet.user_id == current_user.uid
    ).first()
    
    if wallet:
        return {
            "is_promoter": True,
            "wallet": {
                "id": wallet.id,
                "user_id": current_user.uid,
                "total_earned": wallet.total_earned,
                "available_balance": wallet.available_amount,
                "pending_amount": wallet.pending_amount,
                "total_withdrawn": wallet.withdrawn_amount,
                "created_at": wallet.created_at,
                "updated_at": wallet.updated_at
            }
        }
    else:
        return {
            "is_promoter": False,
            "wallet": None
        }


@router.delete("/me")
def delete_account(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Delete the authenticated user's account permanently.
    This will cascade delete all associated data:
    - Posts (reels, products, photos)
    - Comments
    - Likes
    - Follows (both as follower and followed)
    - Orders and order items
    - Commissions
    - Notifications
    
    Required for Apple Store and Google Play Store compliance.
    """
    user_id = current_user.id
    
    # The cascade deletes are handled by SQLAlchemy relationships
    # defined in models.py with cascade="all, delete-orphan"
    # Additional manual cleanup for relationships without cascade:
    
    # Delete follows where user is follower or followed
    db.query(models.Follow).filter(
        (models.Follow.follower_id == user_id) | 
        (models.Follow.followed_id == user_id)
    ).delete(synchronize_session=False)
    
    # Delete post likes
    db.query(models.PostLike).filter(
        models.PostLike.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Delete commissions
    db.query(models.Commission).filter(
        models.Commission.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Delete notifications for this user
    db.query(models.Notification).filter(
        models.Notification.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Delete all comments by this user
    db.query(models.Comment).filter(
        models.Comment.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Delete all posts by this user (will cascade delete likes and comments on those posts)
    db.query(models.Post).filter(
        models.Post.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Delete all orders by this user (will cascade delete order items)
    db.query(models.Order).filter(
        models.Order.user_id == user_id
    ).delete(synchronize_session=False)
    
    # Finally, delete the user
    db.delete(current_user)
    db.commit()
    
    return {
        "message": "Account successfully deleted",
        "deleted_user_id": current_user.uid
    }