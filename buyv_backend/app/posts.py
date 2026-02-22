from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime

from .database import get_db
from .models import User, Post, PostLike, PostBookmark
from .marketplace.models import MarketplaceProduct, ProductPromotion
from .auth import get_current_user, get_current_user_optional
from .schemas import PostOut, CountResponse, PostCreate

router = APIRouter(prefix="/posts", tags=["posts"])



def _map_post_out(row: Post, user: User, liked: bool = False, bookmarked: bool = False) -> PostOut:
    # PostOut uses alias 'id' for validation but we pass kwargs. 
    # db row 'uid' -> id.
    # db row 'media_url' -> video_url (aliased to videoUrl).
    return PostOut(
        id=row.uid, # Field(alias="id")
        user_id=user.uid, # Pass UID string (aliased to userId)
        username=user.username,
        display_name=user.display_name, 
        user_profile_image=user.profile_image_url,
        is_user_verified=user.is_verified,
        
        type=row.type,
        video_url=row.media_url, # Aliased to videoUrl.
        thumbnail_url=row.thumbnail_url,
        caption=row.caption,
        likes_count=row.likes_count or 0,
        comments_count=row.comments_count or 0,  # Use actual DB value
        shares_count=0,   # Placeholder
        views_count=0,    # Placeholder
        
        created_at=row.created_at,
        updated_at=row.updated_at,
        is_liked=liked,
        is_bookmarked=bookmarked,
        marketplace_product_uid=getattr(row, 'marketplace_product_uid', None),
    )

@router.post("/", response_model=PostOut)
def create_post(
    payload: PostCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post_type = payload.type
    if post_type not in {"reel", "product", "photo"}:
        raise HTTPException(status_code=400, detail="Invalid post type")

    row = Post(
        user_id=current_user.id,
        type=post_type,
        media_url=payload.media_url, # Input is PostCreate(CamelModel), alias mediaUrl -> media_url
        caption=payload.caption or None,
        likes_count=0,
    )
    db.add(row)
    # Update counters for reels
    if post_type == "reel":
        current_user.reels_count = (current_user.reels_count or 0) + 1
    db.commit()
    db.refresh(row)
    return _map_post_out(row, current_user, liked=False)


@router.get("/feed", response_model=List[PostOut])
def get_feed(
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional),
):
    # Global feed for now
    rows = (
        db.query(Post)
        .order_by(Post.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    
    if not rows:
        return []

    # Fetch authors
    user_ids = list({p.user_id for p in rows})
    users = db.query(User).filter(User.id.in_(user_ids)).all()
    user_map = {u.id: u for u in users}

    # Fetch my likes and bookmarks if authenticated
    liked_post_ids = set()
    bookmarked_post_ids = set()
    
    if current_user:
        post_ids = [r.id for r in rows]
        my_likes = db.query(PostLike).filter(PostLike.user_id == current_user.id, PostLike.post_id.in_(post_ids)).all()
        liked_post_ids = {l.post_id for l in my_likes}

        my_bookmarks = db.query(PostBookmark).filter(PostBookmark.user_id == current_user.id, PostBookmark.post_id.in_(post_ids)).all()
        bookmarked_post_ids = {b.post_id for b in my_bookmarks}

    out = []
    for r in rows:
        author = user_map.get(r.user_id)
        if author:
            is_liked = r.id in liked_post_ids
            is_bookmarked = r.id in bookmarked_post_ids
            out.append(_map_post_out(r, author, liked=is_liked, bookmarked=is_bookmarked))
    
    return out


@router.get("/bookmarks", response_model=List[PostOut])
def get_my_bookmarked_posts(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get bookmarked posts for the currently authenticated user."""
    bookmark_rows = (
        db.query(PostBookmark)
        .filter(PostBookmark.user_id == current_user.id)
        .order_by(PostBookmark.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    post_ids = [r.post_id for r in bookmark_rows]
    if not post_ids:
        return []

    posts = db.query(Post).filter(Post.id.in_(post_ids)).all()
    post_map = {p.id: p for p in posts}

    # Fetch authors
    author_ids = list({p.user_id for p in posts})
    authors = db.query(User).filter(User.id.in_(author_ids)).all()
    author_map = {a.id: a for a in authors}

    # Fetch likes for current user
    my_likes = db.query(PostLike).filter(
        PostLike.user_id == current_user.id,
        PostLike.post_id.in_(post_ids)
    ).all()
    liked_post_ids = {l.post_id for l in my_likes}

    out: List[PostOut] = []
    for br in bookmark_rows:
        p = post_map.get(br.post_id)
        if p is None:
            continue
        author = author_map.get(p.user_id)
        if author:
            out.append(_map_post_out(p, author, liked=p.id in liked_post_ids, bookmarked=True))
    return out


@router.get("/{post_uid}", response_model=PostOut)
def get_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get a single post by its UID"""
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    # Get the author
    author = db.query(User).filter(User.id == post.user_id).first()
    if not author:
        raise HTTPException(status_code=404, detail="Post author not found")
    
    # Check if current user liked this post
    liked = db.query(PostLike).filter(
        PostLike.user_id == current_user.id,
        PostLike.post_id == post.id
    ).first() is not None
    
    return _map_post_out(post, author, liked=liked)


@router.get("/user/{uid}", response_model=List[PostOut])
def list_user_posts(
    uid: str,
    type: Optional[str] = Query(default=None),
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    q = db.query(Post).filter(Post.user_id == user.id)
    if type:
        q = q.filter(Post.type == type)
    rows = (
        q.order_by(Post.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return [_map_post_out(row, user) for row in rows]


@router.get("/user/{uid}/liked", response_model=List[PostOut])
def list_user_liked_posts(
    uid: str,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    like_rows = (
        db.query(PostLike)
        .filter(PostLike.user_id == user.id)
        .order_by(PostLike.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    post_ids = [r.post_id for r in like_rows]
    if not post_ids:
        return []
    posts = db.query(Post).filter(Post.id.in_(post_ids)).all()
    # Preserve order according to like_rows
    post_map = {p.id: p for p in posts}
    
    # We need to fetch users for these posts if they belong to others.
    # Optimization: Fetch all authors.
    author_ids = list({p.user_id for p in posts})
    authors = db.query(User).filter(User.id.in_(author_ids)).all()
    author_map = {a.id: a for a in authors}

    out: List[PostOut] = []
    for lr in like_rows:
        p = post_map.get(lr.post_id)
        if p is None:
            continue
        author = author_map.get(p.user_id)
        if author:
            item = _map_post_out(p, author, liked=True)
            out.append(item)
    return out


@router.get("/user/{uid}/count", response_model=CountResponse)
def count_user_posts(
    uid: str,
    type: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    q = db.query(Post).filter(Post.user_id == user.id)
    if type:
        q = q.filter(Post.type == type)
    cnt = q.count()
    return CountResponse(count=cnt)


@router.get("/user/{uid}/bookmarked", response_model=List[PostOut])
def list_user_bookmarked_posts(
    uid: str,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    user = db.query(User).filter(User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Check if we are viewing our own bookmarks
    if user.id != current_user.id:
        # In many apps, bookmarks are private. Let's enforce that.
        raise HTTPException(status_code=403, detail="Not allowed to view other users' bookmarks")

    bookmark_rows = (
        db.query(PostBookmark)
        .filter(PostBookmark.user_id == user.id)
        .order_by(PostBookmark.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    post_ids = [r.post_id for r in bookmark_rows]
    if not post_ids:
        return []
    
    posts = db.query(Post).filter(Post.id.in_(post_ids)).all()
    post_map = {p.id: p for p in posts}
    
    # Fetch authors
    author_ids = list({p.user_id for p in posts})
    authors = db.query(User).filter(User.id.in_(author_ids)).all()
    author_map = {a.id: a for a in authors}

    # Fetch likes status for these posts for the current user
    my_likes = db.query(PostLike).filter(
        PostLike.user_id == current_user.id, 
        PostLike.post_id.in_(post_ids)
    ).all()
    liked_post_ids = {l.post_id for l in my_likes}

    out: List[PostOut] = []
    for br in bookmark_rows:
        p = post_map.get(br.post_id)
        if p is None:
            continue
        author = author_map.get(p.user_id)
        if author:
            is_liked = p.id in liked_post_ids
            # Note: is_bookmarked is implicitly true since we are in the bookmarked list
            item = _map_post_out(p, author, liked=is_liked, bookmarked=True)
            out.append(item)
    return out


@router.get("/search", response_model=List[PostOut])
def search_posts(
    q: str = Query(..., min_length=1, description="Search query"),
    type: Optional[str] = Query(default=None, description="Filter by post type: reel, product, photo"),
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional),
):
    """Search posts by caption with pagination"""
    search_pattern = f"%{q}%"
    
    # Base query
    query = db.query(Post).filter(Post.caption.ilike(search_pattern))
    
    # Filter by type if provided
    if type and type in {"reel", "product", "photo"}:
        query = query.filter(Post.type == type)
    
    # Apply pagination
    rows = (
        query.order_by(Post.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    
    if not rows:
        return []
    
    # Fetch authors
    user_ids = list({p.user_id for p in rows})
    users = db.query(User).filter(User.id.in_(user_ids)).all()
    user_map = {u.id: u for u in users}
    
    # Fetch likes if user is authenticated
    liked_post_ids = set()
    if current_user:
        post_ids = [r.id for r in rows]
        my_likes = db.query(PostLike).filter(
            PostLike.user_id == current_user.id,
            PostLike.post_id.in_(post_ids)
        ).all()
        liked_post_ids = {l.post_id for l in my_likes}
    
    # Map to output
    out = []
    for r in rows:
        author = user_map.get(r.user_id)
        if author:
            is_liked = r.id in liked_post_ids
            out.append(_map_post_out(r, author, liked=is_liked))
    
    return out


@router.post("/{post_uid}/like")
def like_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    existing = db.query(PostLike).filter(PostLike.post_id == post.id, PostLike.user_id == current_user.id).first()
    if existing:
        return {"status": "already_liked"}
    like = PostLike(post_id=post.id, user_id=current_user.id)
    db.add(like)
    post.likes_count = (post.likes_count or 0) + 1
    db.commit()
    return {"status": "liked"}


@router.delete("/{post_uid}/like")
def unlike_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    existing = db.query(PostLike).filter(PostLike.post_id == post.id, PostLike.user_id == current_user.id).first()
    if not existing:
        return {"status": "not_liked"}
    db.delete(existing)
    post.likes_count = max(0, (post.likes_count or 0) - 1)
    db.commit()
    return {"status": "unliked"}


@router.get("/{post_uid}/is_liked")
def is_post_liked(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    existing = db.query(PostLike).filter(PostLike.post_id == post.id, PostLike.user_id == current_user.id).first()
    return {"isLiked": existing is not None}


@router.post("/{post_uid}/bookmark")
def bookmark_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    existing = db.query(PostBookmark).filter(
        PostBookmark.post_id == post.id,
        PostBookmark.user_id == current_user.id
    ).first()
    
    if existing:
        return {"status": "already_bookmarked"}
    
    bookmark = PostBookmark(post_id=post.id, user_id=current_user.id)
    db.add(bookmark)
    db.commit()
    return {"status": "bookmarked"}


@router.delete("/{post_uid}/bookmark")
def unbookmark_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    
    existing = db.query(PostBookmark).filter(
        PostBookmark.post_id == post.id,
        PostBookmark.user_id == current_user.id
    ).first()
    
    if not existing:
        return {"status": "not_bookmarked"}
    
    db.delete(existing)
    db.commit()
    return {"status": "unbookmarked"}


@router.get("/{post_uid}/is_bookmarked")
def is_post_bookmarked(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    existing = db.query(PostBookmark).filter(
        PostBookmark.post_id == post.id,
        PostBookmark.user_id == current_user.id
    ).first()
    return {"isBookmarked": existing is not None}


@router.delete("/{post_uid}")
def delete_post(
    post_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    post = db.query(Post).filter(Post.uid == post_uid).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    if post.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")
    post_type = post.type
    # Clean up related product_promotions (post_id is the post UID string)
    db.query(ProductPromotion).filter(ProductPromotion.post_id == post_uid).delete()
    # Delete the post (comments, likes, bookmarks cascade via FK)
    db.delete(post)
    if post_type == "reel":
        current_user.reels_count = max(0, (current_user.reels_count or 0) - 1)
    db.commit()
    return {"status": "deleted"}


# ── Bridge endpoint: marketplace product → post ────────────────────────────────
@router.get("/by-marketplace-product/{product_uuid}", response_model=PostOut)
def get_or_create_post_for_product(
    product_uuid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Find (or auto-create) the Post row associated with a marketplace product UUID.
    This allows likes, comments and bookmarks to work for marketplace products
    that were not originally created as user posts."""
    # 1. Try to find an existing post linked to this marketplace product
    post = db.query(Post).filter(
        Post.marketplace_product_uid == product_uuid
    ).first()

    if not post:
        # 2. Verify the marketplace product actually exists
        mp = db.query(MarketplaceProduct).filter(
            MarketplaceProduct.id == product_uuid
        ).first()
        if not mp:
            raise HTTPException(status_code=404, detail="Marketplace product not found")

        # 3. Auto-create a shadow post linked to this product
        post = Post(
            user_id=current_user.id,
            type="product",
            media_url=mp.main_image_url or "",
            thumbnail_url=mp.main_image_url,
            caption=mp.name,
            marketplace_product_uid=product_uuid,
        )
        db.add(post)
        db.commit()
        db.refresh(post)

    # 4. Check like/bookmark status for current user
    liked = db.query(PostLike).filter(
        PostLike.post_id == post.id,
        PostLike.user_id == current_user.id,
    ).first() is not None

    bookmarked = db.query(PostBookmark).filter(
        PostBookmark.post_id == post.id,
        PostBookmark.user_id == current_user.id,
    ).first() is not None

    owner = db.query(User).filter(User.id == post.user_id).first()
    return _map_post_out(post, owner or current_user, liked=liked, bookmarked=bookmarked)
