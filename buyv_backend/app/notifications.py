from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from .database import get_db
from .models import User, Notification
from .auth import get_current_user
from .schemas import NotificationCreate, NotificationOut
from .firebase_service import FirebaseService, NotificationType
import json
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.post("/", response_model=NotificationOut)
def create_notification(payload: NotificationCreate, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    """
    Create a notification for a target user.
    Also sends a push notification if the user has an FCM token registered.
    """
    # Create notification for the target user by uid provided, defaulting to current user
    target_uid = payload.userId or current_user.uid
    target = db.query(User).filter(User.uid == target_uid).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Create notification in database
    notif = Notification(
        user_id=target.id,
        title=payload.title,
        body=payload.body,
        type=payload.type,
        # Store as JSON string in DB
        data=json.dumps(payload.data or {}),
    )
    db.add(notif)
    db.commit()
    db.refresh(notif)
    
    # Send push notification if user has FCM token
    if target.fcm_token:
        try:
            notification_data = payload.data or {}
            notification_data['notification_id'] = str(notif.id)
            
            FirebaseService.send_notification(
                token=target.fcm_token,
                title=payload.title,
                body=payload.body,
                data=notification_data,
                notification_type=payload.type
            )
            logger.info(f"Push notification sent to user {target.uid}")
        except Exception as e:
            logger.error(f"Failed to send push notification: {e}")
            # Don't fail the request if push notification fails
    
    return NotificationOut(
        id=notif.id,
        userId=target.uid,
        title=notif.title,
        body=notif.body,
        type=notif.type,
        # Return parsed JSON object to client
        data=payload.data or {},
        isRead=notif.is_read,
        createdAt=notif.created_at,
    )


@router.get("/me", response_model=list[NotificationOut])
def list_my_notifications(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    rows = db.query(Notification).filter(Notification.user_id == current_user.id).order_by(Notification.created_at.desc()).all()
    return [
        NotificationOut(
            id=row.id,
            userId=current_user.uid,
            title=row.title,
            body=row.body,
            type=row.type,
            # Parse JSON string from DB
            data=(json.loads(row.data) if row.data else {}),
            isRead=row.is_read,
            createdAt=row.created_at,
        ) for row in rows
    ]


@router.post("/{notification_id}/read")
def mark_as_read(notification_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    notif = db.query(Notification).filter(Notification.id == notification_id, Notification.user_id == current_user.id).first()
    if not notif:
        raise HTTPException(status_code=404, detail="Notification not found")
    notif.is_read = True
    db.commit()
    return {"status": "ok"}


@router.delete("/{notification_id}")
def delete_notification(notification_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    notif = db.query(Notification).filter(Notification.id == notification_id, Notification.user_id == current_user.id).first()
    if not notif:
        raise HTTPException(status_code=404, detail="Notification not found")
    db.delete(notif)
    db.commit()
    return {"status": "ok"}


@router.delete("/")
def clear_all_notifications(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    count = db.query(Notification).filter(Notification.user_id == current_user.id).delete()
    db.commit()
    return {"status": "ok", "deleted": count}