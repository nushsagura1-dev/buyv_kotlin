"""
Reports API Endpoints
Allows users to report content (posts, comments, users) and admins to manage reports.
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc
from datetime import datetime
from typing import List, Optional

from .database import get_db
from .models import User, Report, Post, Comment
from .auth import get_current_user, require_admin_role
from .schemas import CamelModel

router = APIRouter(tags=["Reports"])


# ============ Response Schemas ============

class ReportCreateRequest(CamelModel):
    target_type: str  # "post", "comment", "user"
    target_id: str    # UID/ID of the reported item
    reason: str       # Reason category: "spam", "harassment", "inappropriate", "violence", "other"
    description: Optional[str] = None  # Optional detailed description


class ReportOut(CamelModel):
    id: int
    reporter_uid: str
    reporter_username: str
    target_type: str
    target_id: str
    reason: str
    description: Optional[str] = None
    status: str
    admin_notes: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class ReportActionRequest(CamelModel):
    status: str  # "reviewed", "resolved", "dismissed"
    admin_notes: Optional[str] = None


VALID_TARGET_TYPES = {"post", "comment", "user"}
VALID_REASONS = {"spam", "harassment", "inappropriate", "violence", "hate_speech", "misinformation", "other"}


# ============ User Endpoints ============

@router.post("/api/reports", response_model=ReportOut)
def create_report(
    request: ReportCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Submit a report for a post, comment, or user."""
    if request.target_type not in VALID_TARGET_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid target_type. Must be one of: {', '.join(VALID_TARGET_TYPES)}"
        )

    if request.reason not in VALID_REASONS:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid reason. Must be one of: {', '.join(VALID_REASONS)}"
        )

    # Validate target exists
    if request.target_type == "post":
        target = db.query(Post).filter(Post.uid == request.target_id).first()
        if not target:
            raise HTTPException(status_code=404, detail="Post not found")
    elif request.target_type == "comment":
        target = db.query(Comment).filter(Comment.id == int(request.target_id)).first()
        if not target:
            raise HTTPException(status_code=404, detail="Comment not found")
    elif request.target_type == "user":
        target = db.query(User).filter(User.uid == request.target_id).first()
        if not target:
            raise HTTPException(status_code=404, detail="User not found")
        if request.target_id == current_user.uid:
            raise HTTPException(status_code=400, detail="Cannot report yourself")

    report = Report(
        reporter_uid=current_user.uid,
        target_type=request.target_type,
        target_id=request.target_id,
        reason=request.reason,
        description=request.description
    )
    db.add(report)
    db.commit()
    db.refresh(report)

    return ReportOut(
        id=report.id,
        reporter_uid=current_user.uid,
        reporter_username=current_user.username,
        target_type=report.target_type,
        target_id=report.target_id,
        reason=report.reason,
        description=report.description,
        status=report.status,
        admin_notes=report.admin_notes,
        created_at=report.created_at,
        updated_at=report.updated_at
    )


@router.get("/api/reports/me", response_model=List[ReportOut])
def get_my_reports(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get reports submitted by the current user."""
    reports = (
        db.query(Report)
        .filter(Report.reporter_uid == current_user.uid)
        .order_by(desc(Report.created_at))
        .all()
    )

    return [
        ReportOut(
            id=r.id,
            reporter_uid=r.reporter_uid,
            reporter_username=current_user.username,
            target_type=r.target_type,
            target_id=r.target_id,
            reason=r.reason,
            description=r.description,
            status=r.status,
            admin_notes=r.admin_notes,
            created_at=r.created_at,
            updated_at=r.updated_at
        )
        for r in reports
    ]


# ============ Admin Endpoints ============

@router.get("/api/admin/reports", response_model=List[ReportOut])
def get_all_reports(
    status: Optional[str] = Query(None, description="Filter by status: pending, reviewed, resolved, dismissed"),
    target_type: Optional[str] = Query(None, description="Filter by target type: post, comment, user"),
    limit: int = Query(50, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin"))
):
    """Admin: Get all reports with optional filters."""
    query = db.query(Report)

    if status:
        query = query.filter(Report.status == status)
    if target_type:
        query = query.filter(Report.target_type == target_type)

    reports = query.order_by(desc(Report.created_at)).offset(offset).limit(limit).all()

    result = []
    for r in reports:
        reporter = db.query(User).filter(User.uid == r.reporter_uid).first()
        result.append(ReportOut(
            id=r.id,
            reporter_uid=r.reporter_uid,
            reporter_username=reporter.username if reporter else "deleted_user",
            target_type=r.target_type,
            target_id=r.target_id,
            reason=r.reason,
            description=r.description,
            status=r.status,
            admin_notes=r.admin_notes,
            created_at=r.created_at,
            updated_at=r.updated_at
        ))
    return result


@router.put("/api/admin/reports/{report_id}", response_model=ReportOut)
def update_report_status(
    report_id: int,
    request: ReportActionRequest,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin"))
):
    """Admin: Update report status (reviewed/resolved/dismissed)."""
    valid_statuses = {"reviewed", "resolved", "dismissed"}
    if request.status not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid status. Must be one of: {', '.join(valid_statuses)}"
        )

    report = db.query(Report).filter(Report.id == report_id).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")

    report.status = request.status
    report.admin_notes = request.admin_notes
    report.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(report)

    reporter = db.query(User).filter(User.uid == report.reporter_uid).first()
    return ReportOut(
        id=report.id,
        reporter_uid=report.reporter_uid,
        reporter_username=reporter.username if reporter else "deleted_user",
        target_type=report.target_type,
        target_id=report.target_id,
        reason=report.reason,
        description=report.description,
        status=report.status,
        admin_notes=report.admin_notes,
        created_at=report.created_at,
        updated_at=report.updated_at
    )


@router.get("/api/admin/reports/stats")
def get_report_stats(
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin_role("admin", "super_admin"))
):
    """Admin: Get report statistics."""
    from sqlalchemy import func

    total = db.query(func.count(Report.id)).scalar() or 0
    pending = db.query(func.count(Report.id)).filter(Report.status == "pending").scalar() or 0
    reviewed = db.query(func.count(Report.id)).filter(Report.status == "reviewed").scalar() or 0
    resolved = db.query(func.count(Report.id)).filter(Report.status == "resolved").scalar() or 0
    dismissed = db.query(func.count(Report.id)).filter(Report.status == "dismissed").scalar() or 0

    return {
        "total": total,
        "pending": pending,
        "reviewed": reviewed,
        "resolved": resolved,
        "dismissed": dismissed
    }
