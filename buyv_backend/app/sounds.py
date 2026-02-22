"""
Sounds API Endpoints
Provides sound/music library for Reels creation.
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc, func
from datetime import datetime
from typing import List, Optional

from .database import get_db
from .models import User, Sound
from .auth import get_current_user, get_current_user_optional, require_admin_role
from .schemas import CamelModel

router = APIRouter(prefix="/api/sounds", tags=["Sounds"])


# ============ Response Schemas ============

class SoundOut(CamelModel):
    id: int
    uid: str
    title: str
    artist: str
    audio_url: str
    cover_image_url: Optional[str] = None
    duration: float  # seconds
    genre: Optional[str] = None
    usage_count: int = 0
    is_featured: bool = False
    created_at: datetime


class SoundCreateRequest(CamelModel):
    title: str
    artist: str
    audio_url: str
    cover_image_url: Optional[str] = None
    duration: float
    genre: Optional[str] = None


# ============ Public Endpoints ============

@router.get("", response_model=List[SoundOut])
def get_sounds(
    search: Optional[str] = Query(None, description="Search by title or artist"),
    genre: Optional[str] = Query(None, description="Filter by genre"),
    featured: Optional[bool] = Query(None, description="Filter featured sounds"),
    limit: int = Query(30, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
):
    """Get available sounds for Reels creation."""
    query = db.query(Sound)

    if search:
        search_term = f"%{search}%"
        query = query.filter(
            (Sound.title.ilike(search_term)) | (Sound.artist.ilike(search_term))
        )
    if genre:
        query = query.filter(Sound.genre == genre)
    if featured is not None:
        query = query.filter(Sound.is_featured == featured)

    sounds = query.order_by(desc(Sound.usage_count)).offset(offset).limit(limit).all()

    return [_sound_to_out(s) for s in sounds]


@router.get("/genres")
def get_genres(db: Session = Depends(get_db)):
    """Get all available sound genres."""
    genres = (
        db.query(Sound.genre)
        .filter(Sound.genre.isnot(None))
        .distinct()
        .all()
    )
    return [g[0] for g in genres if g[0]]


@router.get("/trending", response_model=List[SoundOut])
def get_trending_sounds(
    limit: int = Query(20, le=50),
    db: Session = Depends(get_db),
):
    """Get trending sounds (most used)."""
    sounds = (
        db.query(Sound)
        .order_by(desc(Sound.usage_count))
        .limit(limit)
        .all()
    )
    return [_sound_to_out(s) for s in sounds]


@router.get("/{sound_uid}", response_model=SoundOut)
def get_sound(
    sound_uid: str,
    db: Session = Depends(get_db),
):
    """Get a specific sound by UID."""
    sound = db.query(Sound).filter(Sound.uid == sound_uid).first()
    if not sound:
        raise HTTPException(status_code=404, detail="Sound not found")
    return _sound_to_out(sound)


@router.post("/{sound_uid}/use")
def increment_sound_usage(
    sound_uid: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Increment usage count when a sound is used in a Reel."""
    sound = db.query(Sound).filter(Sound.uid == sound_uid).first()
    if not sound:
        raise HTTPException(status_code=404, detail="Sound not found")

    sound.usage_count += 1
    db.commit()
    return {"message": "Usage recorded", "usage_count": sound.usage_count}


# ============ Admin Endpoints ============

@router.post("", response_model=SoundOut)
def create_sound(
    request: SoundCreateRequest,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin")),
):
    """Admin: Add a new sound to the library."""
    sound = Sound(
        title=request.title,
        artist=request.artist,
        audio_url=request.audio_url,
        cover_image_url=request.cover_image_url,
        duration=request.duration,
        genre=request.genre,
    )
    db.add(sound)
    db.commit()
    db.refresh(sound)
    return _sound_to_out(sound)


@router.delete("/{sound_uid}")
def delete_sound(
    sound_uid: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin")),
):
    """Admin: Delete a sound from the library."""
    sound = db.query(Sound).filter(Sound.uid == sound_uid).first()
    if not sound:
        raise HTTPException(status_code=404, detail="Sound not found")

    db.delete(sound)
    db.commit()
    return {"message": "Sound deleted successfully"}


@router.put("/{sound_uid}/featured")
def toggle_featured(
    sound_uid: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin")),
):
    """Admin: Toggle featured status of a sound."""
    sound = db.query(Sound).filter(Sound.uid == sound_uid).first()
    if not sound:
        raise HTTPException(status_code=404, detail="Sound not found")

    sound.is_featured = not sound.is_featured
    db.commit()
    return {"message": f"Sound {'featured' if sound.is_featured else 'unfeatured'}", "is_featured": sound.is_featured}


# ============ Helpers ============

def _sound_to_out(sound: Sound) -> SoundOut:
    return SoundOut(
        id=sound.id,
        uid=sound.uid,
        title=sound.title,
        artist=sound.artist,
        audio_url=sound.audio_url,
        cover_image_url=sound.cover_image_url,
        duration=sound.duration,
        genre=sound.genre,
        usage_count=sound.usage_count,
        is_featured=sound.is_featured,
        created_at=sound.created_at
    )
