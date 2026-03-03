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


# ============================================================
# Audio Extraction from Video
# ============================================================

@router.post("/extract")
async def extract_audio_from_video(
    video_url: str = Query(..., description="Public URL of the source video"),
    title: Optional[str] = Query(None, description="Title for the extracted sound"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Download a video from *video_url* and extract its audio track using FFmpeg.

    Returns a URL to the extracted audio file (uploaded to Cloudinary when configured,
    otherwise served from /tmp as a base64 data-URI fallback).

    Requires FFmpeg installed on the server.
    """
    import subprocess
    import tempfile
    import os
    import base64
    import mimetypes
    import httpx as _httpx

    # ---- Download source video ----
    try:
        async with _httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
            video_resp = await client.get(video_url)
        if video_resp.status_code != 200:
            raise HTTPException(status_code=400, detail=f"Could not download video (HTTP {video_resp.status_code})")
        video_bytes = video_resp.content
    except _httpx.RequestError as exc:
        raise HTTPException(status_code=503, detail=f"Network error downloading video: {exc}")

    # ---- Write to temp file ----
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp_video:
        tmp_video.write(video_bytes)
        tmp_video_path = tmp_video.name

    tmp_audio_path = tmp_video_path.replace(".mp4", "_audio.aac")

    try:
        # ---- Run FFmpeg ----
        result = subprocess.run(
            [
                "ffmpeg", "-y", "-i", tmp_video_path,
                "-vn", "-acodec", "aac", "-b:a", "128k",
                tmp_audio_path,
            ],
            capture_output=True,
            timeout=120,
        )
        if result.returncode != 0:
            # Fallback: copy audio stream without re-encoding
            result2 = subprocess.run(
                ["ffmpeg", "-y", "-i", tmp_video_path, "-vn", "-acodec", "copy", tmp_audio_path],
                capture_output=True,
                timeout=120,
            )
            if result2.returncode != 0:
                raise HTTPException(status_code=500, detail="FFmpeg failed to extract audio")

        # ---- Probe duration ----
        probe = subprocess.run(
            [
                "ffprobe", "-v", "quiet", "-print_format", "json",
                "-show_format", tmp_audio_path,
            ],
            capture_output=True,
            timeout=30,
        )
        duration: Optional[float] = None
        if probe.returncode == 0:
            import json as _json
            probe_data = _json.loads(probe.stdout)
            try:
                duration = float(probe_data["format"]["duration"])
            except (KeyError, ValueError):
                duration = None

        # ---- Upload or encode ----
        cloudinary_url = os.getenv("CLOUDINARY_URL")
        audio_url: str

        if cloudinary_url:
            import cloudinary
            import cloudinary.uploader
            upload_result = cloudinary.uploader.upload(
                tmp_audio_path,
                resource_type="video",  # Cloudinary uses "video" for audio files
                folder="sounds/extracted",
                format="aac",
            )
            audio_url = upload_result.get("secure_url", upload_result["url"])
        else:
            # Fallback: return base64 data-URI (MVP — not suitable for large files)
            with open(tmp_audio_path, "rb") as f:
                audio_b64 = base64.b64encode(f.read()).decode()
            audio_url = f"data:audio/aac;base64,{audio_b64}"

    finally:
        for path in (tmp_video_path, tmp_audio_path):
            try:
                os.unlink(path)
            except OSError:
                pass

    return {
        "audio_url": audio_url,
        "duration": duration,
        "source_video_url": video_url,
        "title": title or "Extracted Sound",
        "message": "Audio extracted successfully",
    }


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
