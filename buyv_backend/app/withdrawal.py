"""
Phase 8: Withdrawal Management Endpoints
Handles withdrawal requests from promoters and admin approvals
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field, validator
import json

from .database import get_db
from .models import WithdrawalRequest, PromoterWallet, User
from .auth import get_current_user_uid, require_admin_role

router = APIRouter(prefix="/api/marketplace/withdrawal", tags=["Withdrawal"])


# ============ Schemas ============
class CreateWithdrawalRequest(BaseModel):
    amount: float = Field(..., gt=0, description="Amount to withdraw in USD")
    payment_method: str = Field(..., description="Payment method: paypal, bank_transfer")
    payment_details: dict = Field(..., description="Payment account details")
    
    @validator('amount')
    def validate_amount(cls, v):
        if v < 50:
            raise ValueError("Minimum withdrawal amount is $50")
        if v > 10000:
            raise ValueError("Maximum withdrawal amount is $10,000 per request")
        return round(v, 2)
    
    @validator('payment_method')
    def validate_payment_method(cls, v):
        allowed_methods = ['paypal', 'bank_transfer']
        if v.lower() not in allowed_methods:
            raise ValueError(f"Payment method must be one of: {allowed_methods}")
        return v.lower()
    
    @validator('payment_details')
    def validate_payment_details(cls, v, values):
        method = values.get('payment_method', '').lower()
        
        if method == 'paypal':
            if 'email' not in v:
                raise ValueError("PayPal email is required")
            if not v['email'] or '@' not in v['email']:
                raise ValueError("Valid PayPal email is required")
        
        elif method == 'bank_transfer':
            required_fields = ['account_holder_name', 'bank_name', 'account_number', 'routing_number']
            missing = [f for f in required_fields if f not in v or not v[f]]
            if missing:
                raise ValueError(f"Missing bank details: {', '.join(missing)}")
        
        return v


class WithdrawalRequestResponse(BaseModel):
    id: int
    user_id: str
    wallet_id: int
    amount: float
    payment_method: str
    payment_details: dict
    status: str
    rejection_reason: Optional[str] = None
    created_at: datetime
    processed_at: Optional[datetime] = None
    processed_by: Optional[str] = None
    
    class Config:
        from_attributes = True


class ApproveWithdrawalRequest(BaseModel):
    admin_notes: Optional[str] = None


class RejectWithdrawalRequest(BaseModel):
    admin_notes: str = Field(..., min_length=10, description="Reason for rejection")


class CompleteWithdrawalRequest(BaseModel):
    transaction_id: str = Field(..., min_length=5, description="External payment transaction ID")
    admin_notes: Optional[str] = None


class WithdrawalHistoryResponse(BaseModel):
    total: int
    requests: List[WithdrawalRequestResponse]


class WithdrawalStatsResponse(BaseModel):
    available_balance: float
    pending_balance: float
    total_withdrawn: float
    pending_requests_count: int
    approved_requests_count: int
    total_requests_count: int


# ============ Promoter Endpoints ============

@router.post("/request", response_model=WithdrawalRequestResponse)
async def create_withdrawal_request(
    request: CreateWithdrawalRequest,
    current_user_uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    """
    Create a new withdrawal request
    
    Requirements:
    - Amount must be >= $50 and <= available balance
    - Valid payment details
    - No pending requests
    """
    # Get promoter wallet
    wallet = db.query(PromoterWallet).filter(
        PromoterWallet.user_id == current_user_uid
    ).first()
    
    if not wallet:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Promoter wallet not found. Start promoting products first!"
        )
    
    # Check available balance
    if request.amount > wallet.available_amount:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Insufficient balance. Available: ${wallet.available_amount:.2f}"
        )
    
    # Check for existing pending requests
    pending_request = db.query(WithdrawalRequest).filter(
        WithdrawalRequest.user_id == current_user_uid,
        WithdrawalRequest.status == "pending"
    ).first()
    
    if pending_request:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You already have a pending withdrawal request. Please wait for approval."
        )
    
    # Create withdrawal request
    withdrawal = WithdrawalRequest(
        user_id=current_user_uid,
        wallet_id=wallet.id,
        amount=request.amount,
        payment_method=request.payment_method,
        payment_details=json.dumps(request.payment_details),
        status="pending",
        created_at=datetime.utcnow()
    )
    db.add(withdrawal)
    
    # Update wallet (move from available to pending)
    wallet.available_amount -= request.amount
    wallet.pending_amount += request.amount
    wallet.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(withdrawal)
    
    # Prepare response
    response = WithdrawalRequestResponse.from_orm(withdrawal)
    response.payment_details = json.loads(withdrawal.payment_details)
    
    return response


@router.get("/history", response_model=WithdrawalHistoryResponse)
async def get_withdrawal_history(
    current_user_uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    """Get withdrawal history for current promoter"""
    
    requests = db.query(WithdrawalRequest).filter(
        WithdrawalRequest.user_id == current_user_uid
    ).order_by(desc(WithdrawalRequest.created_at)).all()
    
    total = len(requests)
    
    # Parse payment details
    response_list = []
    for req in requests:
        res = WithdrawalRequestResponse.from_orm(req)
        res.payment_details = json.loads(req.payment_details)
        response_list.append(res)
    
    return WithdrawalHistoryResponse(
        total=total,
        requests=response_list
    )


@router.get("/stats", response_model=WithdrawalStatsResponse)
async def get_withdrawal_stats(
    current_user_uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    """Get withdrawal statistics for current promoter"""
    
    # Get wallet
    wallet = db.query(PromoterWallet).filter(
        PromoterWallet.user_id == current_user_uid
    ).first()
    
    if not wallet:
        return WithdrawalStatsResponse(
            available_balance=0.0,
            pending_balance=0.0,
            total_withdrawn=0.0,
            pending_requests_count=0,
            approved_requests_count=0,
            total_requests_count=0
        )
    
    # Count requests by status
    pending_count = db.query(func.count(WithdrawalRequest.id)).filter(
        WithdrawalRequest.user_id == current_user_uid,
        WithdrawalRequest.status == "pending"
    ).scalar() or 0
    
    approved_count = db.query(func.count(WithdrawalRequest.id)).filter(
        WithdrawalRequest.user_id == current_user_uid,
        WithdrawalRequest.status.in_(["approved", "completed"])
    ).scalar() or 0
    
    total_count = db.query(func.count(WithdrawalRequest.id)).filter(
        WithdrawalRequest.user_id == current_user_uid
    ).scalar() or 0
    
    return WithdrawalStatsResponse(
        available_balance=wallet.available_amount,
        pending_balance=wallet.pending_amount,
        total_withdrawn=wallet.withdrawn_amount,
        pending_requests_count=pending_count,
        approved_requests_count=approved_count,
        total_requests_count=total_count
    )


# ============ Admin Endpoints ============

@router.get("/admin/list", response_model=WithdrawalHistoryResponse)
async def admin_list_withdrawals(
    status_filter: Optional[str] = None,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """
    Admin: List all withdrawal requests with optional status filter
    Required role: super_admin or finance
    """
    
    query = db.query(WithdrawalRequest)
    
    if status_filter:
        query = query.filter(WithdrawalRequest.status == status_filter)
    
    requests = query.order_by(desc(WithdrawalRequest.created_at)).all()
    
    # Enrich with promoter names
    response_list = []
    for req in requests:
        res = WithdrawalRequestResponse.from_orm(req)
        res.payment_details = json.loads(req.payment_details)
        
        # Get promoter name
        promoter = db.query(User).filter(User.uid == req.user_id).first()
        if promoter:
            res.promoter_name = promoter.display_name or promoter.username
        
        response_list.append(res)
    
    return WithdrawalHistoryResponse(
        total=len(requests),
        requests=response_list
    )


@router.post("/admin/{withdrawal_id}/approve", response_model=WithdrawalRequestResponse)
async def admin_approve_withdrawal(
    withdrawal_id: int,
    request: ApproveWithdrawalRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """
    Admin: Approve a withdrawal request
    Required role: super_admin or finance
    """
    
    withdrawal = db.query(WithdrawalRequest).filter(
        WithdrawalRequest.id == withdrawal_id
    ).first()
    
    if not withdrawal:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Withdrawal request not found"
        )
    
    if withdrawal.status != "pending":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot approve request with status: {withdrawal.status}"
        )
    
    # Update withdrawal
    withdrawal.status = "approved"
    withdrawal.processed_at = datetime.utcnow()
    withdrawal.processed_by = admin.uid
    
    db.commit()
    db.refresh(withdrawal)
    
    # Prepare response
    response = WithdrawalRequestResponse.from_orm(withdrawal)
    response.payment_details = json.loads(withdrawal.payment_details)
    
    return response


@router.post("/admin/{withdrawal_id}/reject", response_model=WithdrawalRequestResponse)
async def admin_reject_withdrawal(
    withdrawal_id: int,
    request: RejectWithdrawalRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """
    Admin: Reject a withdrawal request and return funds to available balance
    Required role: super_admin or finance
    """
    
    withdrawal = db.query(WithdrawalRequest).filter(
        WithdrawalRequest.id == withdrawal_id
    ).first()
    
    if not withdrawal:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Withdrawal request not found"
        )
    
    if withdrawal.status != "pending":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot reject request with status: {withdrawal.status}"
        )
    
    # Update withdrawal
    withdrawal.status = "rejected"
    withdrawal.rejection_reason = request.admin_notes
    withdrawal.processed_at = datetime.utcnow()
    withdrawal.processed_by = admin.uid
    
    # Return funds to available balance
    wallet = db.query(PromoterWallet).filter(
        PromoterWallet.user_id == withdrawal.user_id
    ).first()
    
    if wallet:
        wallet.available_amount += withdrawal.amount
        wallet.pending_amount -= withdrawal.amount
        wallet.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(withdrawal)
    
    # Prepare response
    response = WithdrawalRequestResponse.from_orm(withdrawal)
    response.payment_details = json.loads(withdrawal.payment_details)
    
    return response


@router.post("/admin/{withdrawal_id}/complete", response_model=WithdrawalRequestResponse)
async def admin_complete_withdrawal(
    withdrawal_id: int,
    request: CompleteWithdrawalRequest,
    admin: User = Depends(require_admin_role("admin")),
    db: Session = Depends(get_db)
):
    """
    Admin: Mark an approved withdrawal as completed (payment sent)
    Required role: super_admin or finance
    """
    
    withdrawal = db.query(WithdrawalRequest).filter(
        WithdrawalRequest.id == withdrawal_id
    ).first()
    
    if not withdrawal:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Withdrawal request not found"
        )
    
    if withdrawal.status not in ["pending", "approved"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot complete request with status: {withdrawal.status}"
        )
    
    # Update withdrawal
    withdrawal.status = "completed"
    withdrawal.processed_at = datetime.utcnow()
    if request.admin_notes:
        withdrawal.rejection_reason = request.admin_notes  # Reusing field for notes
    if not withdrawal.processed_by:
        withdrawal.processed_by = admin.uid
    
    # Update wallet (move from pending to withdrawn)
    wallet = db.query(PromoterWallet).filter(
        PromoterWallet.user_id == withdrawal.user_id
    ).first()
    
    if wallet:
        wallet.pending_amount -= withdrawal.amount
        wallet.withdrawn_amount += withdrawal.amount
        wallet.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(withdrawal)
    
    # Prepare response
    response = WithdrawalRequestResponse.from_orm(withdrawal)
    response.payment_details = json.loads(withdrawal.payment_details)
    
    return response
