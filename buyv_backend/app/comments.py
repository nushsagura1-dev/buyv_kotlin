from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import and_
from typing import List, Optional
from datetime import datetime

from .database import get_db
from .models import User, Post, Comment, CommentLike
from .auth import get_current_user, get_current_user_optional
from .schemas import CommentCreate, CommentOut

router = APIRouter(prefix="/comments", tags=["comments"])


def _map_comment_out(comment: Comment, user: User, post_uid: str, is_liked: bool = False) -> CommentOut:
    """Map Comment model to CommentOut schema"""
    return CommentOut(
        id=comment.id,
        user_id=user.uid,
        username=user.username,
        display_name=user.display_name,
        user_profile_image=user.profile_image_url,
        post_id=post_uid,
        content=comment.content,
        likes_count=comment.likes_count or 0,
        is_liked=is_liked,
        created_at=comment.created_at,
        updated_at=comment.updated_at,
    )


@router.post("/{post_uid}", response_model=CommentOut)
def add_comment(
    post_uid: str,
    payload: CommentCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Add a comment to a post"""
    # Find the post
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    # Create the comment
    comment = Comment(
        user_id=current_user.id,
        post_id=post.id,
        content=payload.content,
    )
    db.add(comment)
    
    # Increment the post's comments count
    post.comments_count = (post.comments_count or 0) + 1
    
    db.commit()
    db.refresh(comment)
    
    return _map_comment_out(comment, current_user, post_uid)


@router.get("/{post_uid}", response_model=List[CommentOut])
def get_comments(
    post_uid: str,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional),
):
    """Fetch comments for a post with pagination"""
    # Find the post
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    # Fetch comments with pagination, ordered by newest first
    comments = (
        db.query(Comment)
        .filter(Comment.post_id == post.id)
        .order_by(Comment.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    
    if not comments:
        return []
    
    # Fetch all users who created these comments
    user_ids = list({c.user_id for c in comments})
    users = db.query(User).filter(User.id.in_(user_ids)).all()
    user_map = {u.id: u for u in users}
    
    # Check which comments the current user has liked
    liked_comment_ids = set()
    if current_user:
        comment_ids = [c.id for c in comments]
        liked = db.query(CommentLike.comment_id).filter(
            and_(
                CommentLike.comment_id.in_(comment_ids),
                CommentLike.user_id == current_user.id
            )
        ).all()
        liked_comment_ids = {row[0] for row in liked}
    
    # Map comments to output schema
    result = []
    for comment in comments:
        user = user_map.get(comment.user_id)
        if user:
            result.append(_map_comment_out(
                comment, user, post_uid,
                is_liked=comment.id in liked_comment_ids
            ))
    
    return result


@router.delete("/{post_uid}/{comment_id}")
def delete_comment(
    post_uid: str,
    comment_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete a comment (only the comment author can delete it)"""
    # Find the post
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    # Find the comment
    comment = db.query(Comment).filter(
        Comment.id == comment_id,
        Comment.post_id == post.id
    ).first()
    
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")
    
    # Check ownership
    if comment.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to delete this comment")
    
    # Delete the comment
    db.delete(comment)
    
    # Decrement the post's comments count
    if post.comments_count > 0:
        post.comments_count -= 1
    
    db.commit()
    
    return {"status": "deleted", "comment_id": comment_id}


@router.post("/{post_uid}/{comment_id}/like")
def toggle_like_comment(
    post_uid: str,
    comment_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Toggle like on a comment. If already liked, unlike it; otherwise, like it."""
    # Find the post
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")

    # Find the comment
    comment = db.query(Comment).filter(
        Comment.id == comment_id,
        Comment.post_id == post.id
    ).first()
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")

    existing_like = db.query(CommentLike).filter(
        and_(
            CommentLike.comment_id == comment.id,
            CommentLike.user_id == current_user.id
        )
    ).first()

    if existing_like:
        # Unlike
        db.delete(existing_like)
        comment.likes_count = max((comment.likes_count or 0) - 1, 0)
        is_liked = False
    else:
        # Like
        like = CommentLike(comment_id=comment.id, user_id=current_user.id)
        db.add(like)
        comment.likes_count = (comment.likes_count or 0) + 1
        is_liked = True

    db.commit()

    return {
        "is_liked": is_liked,
        "likes_count": comment.likes_count or 0
    }
