from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List
from .database import get_db
from .models import User, Follow
from .auth import get_current_user

router = APIRouter(prefix="/users", tags=["follows"])


class UserFollowInfo(BaseModel):
    """Simplified user info for followers/following lists"""
    id: str
    username: str
    displayName: str
    profileImageUrl: str | None = None
    isVerified: bool = False

class FollowingStatusResponse(BaseModel):
    """Response for follow status check"""
    isFollowing: bool
    isFollowedBy: bool


@router.post("/{follower_id}/follow/{followed_id}")
def follow_user(
    follower_id: str, 
    followed_id: str,
    db: Session = Depends(get_db), 
    current_user: User = Depends(get_current_user)
):
    """
    Follow a user. follower_id must match current_user.
    POST /users/{follower_id}/follow/{followed_id}
    """
    # Verify authorization: follower_id must be current user
    if follower_id != current_user.uid:
        raise HTTPException(status_code=403, detail="Cannot follow on behalf of another user")
    
    target = db.query(User).filter(User.uid == followed_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="Target user not found")
    if target.id == current_user.id:
        raise HTTPException(status_code=400, detail="Cannot follow yourself")

    existing = db.query(Follow).filter(Follow.follower_id == current_user.id, Follow.followed_id == target.id).first()
    if existing:
        return {"message": "Already following"}

    follow = Follow(follower_id=current_user.id, followed_id=target.id)
    db.add(follow)
    # update counters
    current_user.following_count = (current_user.following_count or 0) + 1
    target.followers_count = (target.followers_count or 0) + 1
    db.commit()
    return {"message": "Successfully followed user"}


@router.delete("/{follower_id}/unfollow/{followed_id}")
def unfollow_user(
    follower_id: str, 
    followed_id: str,
    db: Session = Depends(get_db), 
    current_user: User = Depends(get_current_user)
):
    """
    Unfollow a user. follower_id must match current_user.
    DELETE /users/{follower_id}/unfollow/{followed_id}
    """
    # Verify authorization
    if follower_id != current_user.uid:
        raise HTTPException(status_code=403, detail="Cannot unfollow on behalf of another user")
    
    target = db.query(User).filter(User.uid == followed_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="Target user not found")
    if target.id == current_user.id:
        raise HTTPException(status_code=400, detail="Cannot unfollow yourself")

    existing = db.query(Follow).filter(Follow.follower_id == current_user.id, Follow.followed_id == target.id).first()
    if not existing:
        return {"message": "Not following"}

    db.delete(existing)
    # update counters
    current_user.following_count = max((current_user.following_count or 0) - 1, 0)
    target.followers_count = max((target.followers_count or 0) - 1, 0)
    db.commit()
    return {"message": "Successfully unfollowed user"}


@router.get("/{current_user_id}/follow-status/{target_user_id}", response_model=FollowingStatusResponse)
def get_following_status(
    current_user_id: str, 
    target_user_id: str, 
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get follow status between current user and target user.
    Returns isFollowing (current follows target) and isFollowedBy (target follows current).
    GET /users/{current_user_id}/follow-status/{target_user_id}
    """
    # Verify authorization: current_user_id must match authenticated user
    if current_user_id != current_user.uid:
        raise HTTPException(status_code=403, detail="Cannot check follow status for another user")
    
    target_user = db.query(User).filter(User.uid == target_user_id).first()
    
    if not target_user:
        raise HTTPException(status_code=404, detail="Target user not found")
    
    # Check if current user follows target
    is_following = db.query(Follow).filter(
        Follow.follower_id == current_user.id, 
        Follow.followed_id == target_user.id
    ).first() is not None
    
    # Check if target user follows current user
    is_followed_by = db.query(Follow).filter(
        Follow.follower_id == target_user.id, 
        Follow.followed_id == current_user.id
    ).first() is not None
    
    return FollowingStatusResponse(
        isFollowing=is_following,
        isFollowedBy=is_followed_by
    )


@router.get("/{user_id}/followers", response_model=List[UserFollowInfo])
def get_followers(user_id: str, db: Session = Depends(get_db)):
    """
    Get list of users who follow the specified user.
    GET /users/{user_id}/followers
    """
    user = db.query(User).filter(User.uid == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Get all follower relationships
    follow_rows = db.query(Follow).filter(Follow.followed_id == user.id).all()
    
    # Fetch all follower users
    follower_ids = [row.follower_id for row in follow_rows]
    if not follower_ids:
        return []
    
    followers = db.query(User).filter(User.id.in_(follower_ids)).all()
    
    return [
        UserFollowInfo(
            id=u.uid,
            username=u.username,
            displayName=u.display_name,
            profileImageUrl=u.profile_image_url,
            isVerified=u.is_verified
        )
        for u in followers
    ]


@router.get("/{user_id}/following", response_model=List[UserFollowInfo])
def get_following(user_id: str, db: Session = Depends(get_db)):
    """
    Get list of users that the specified user follows.
    GET /users/{user_id}/following
    """
    user = db.query(User).filter(User.uid == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Get all following relationships
    follow_rows = db.query(Follow).filter(Follow.follower_id == user.id).all()
    
    # Fetch all followed users
    followed_ids = [row.followed_id for row in follow_rows]
    if not followed_ids:
        return []
    
    following_users = db.query(User).filter(User.id.in_(followed_ids)).all()
    
    return [
        UserFollowInfo(
            id=u.uid,
            username=u.username,
            displayName=u.display_name,
            profileImageUrl=u.profile_image_url,
            isVerified=u.is_verified
        )
        for u in following_users
    ]


@router.get("/{uid}/counts")
def get_counts(uid: str, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    followers = db.query(Follow).filter(Follow.followed_id == user.id).count()
    following = db.query(Follow).filter(Follow.follower_id == user.id).count()
    return {"followers": followers, "following": following}


@router.get("/suggested")
def get_suggested_users(
    limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    # Build exclusion set: self + already-followed users
    followed_rows = db.query(Follow).filter(Follow.follower_id == current_user.id).all()
    exclude_ids = {current_user.id}
    exclude_ids.update(row.followed_id for row in followed_rows)

    # Query top users not followed by current user, ordered by popularity
    query = (
        db.query(User)
        .filter(~User.id.in_(exclude_ids))
        .order_by(User.followers_count.desc(), User.created_at.desc())
        .limit(limit)
    )
    users = query.all()
    return {"suggested": [u.uid for u in users]}