from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from jose import jwt
from passlib.context import CryptContext
from datetime import datetime, timedelta
from slowapi import Limiter
from slowapi.util import get_remote_address
from .database import get_db
from .config import SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES
from . import models
from .schemas import UserCreate, LoginRequest, AuthResponse, UserOut, RefreshTokenRequest, PasswordResetRequest, PasswordResetConfirm
import httpx
import uuid

router = APIRouter(prefix="/auth", tags=["auth"])
limiter = Limiter(key_func=get_remote_address)

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    delta = expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    expire = datetime.utcnow() + delta
    to_encode.update({"exp": expire, "jti": str(uuid.uuid4())})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt, int(delta.total_seconds())


def create_refresh_token(data: dict):
    """Create a refresh token with longer expiry (7 days)"""
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(days=7)
    to_encode.update({"exp": expire, "type": "refresh"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def user_to_out(user: models.User) -> UserOut:
    import json
    interests = []
    settings = None
    try:
        interests = json.loads(user.interests) if user.interests else []
    except Exception:
        interests = []
    try:
        settings = json.loads(user.settings) if user.settings else None
    except Exception:
        settings = None

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
        role=user.role,  # Include role in response
        created_at=user.created_at,
        updated_at=user.updated_at,
        interests=interests,
        settings=settings,
    )

@router.post("/register", response_model=AuthResponse)
@limiter.limit("10/hour")
def register(request: Request, payload: UserCreate, db: Session = Depends(get_db)):
    # H-6: Validate password length (aligned with client-side 8-char minimum)
    if len(payload.password) < 8:
        raise HTTPException(status_code=400, detail="Password must be at least 8 characters")

    # Check if email or username exists
    if db.query(models.User).filter(models.User.email == payload.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    if db.query(models.User).filter(models.User.username == payload.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")

    try:
        user = models.User(
            email=payload.email,
            username=payload.username,
            display_name=payload.display_name, # Accessed via snake_case attribute on model
            password_hash=pwd_context.hash(payload.password),
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    except IntegrityError as e:
        db.rollback()
        # Handle database constraint violations
        error_msg = str(e.orig)
        print(f"IntegrityError during registration: {error_msg}")
        import traceback
        print(traceback.format_exc())
        if "users_email_key" in error_msg or "email" in error_msg.lower():
            raise HTTPException(status_code=400, detail="Email already registered")
        elif "users_username_key" in error_msg or "username" in error_msg.lower():
            raise HTTPException(status_code=400, detail="Username already taken")
        elif "users_pkey" in error_msg or "duplicate key" in error_msg.lower():
            raise HTTPException(
                status_code=500, 
                detail="Database configuration error. Please contact support."
            )
        else:
            raise HTTPException(status_code=500, detail=f"Registration failed: {error_msg}")
    except Exception as e:
        db.rollback()
        import traceback
        print(f"Registration error: {str(e)}")
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=f"Registration error: {str(e)}")

    token, expires_in = create_access_token({"sub": user.uid})
    refresh_token = create_refresh_token({"sub": user.uid})
    return AuthResponse(access_token=token, expires_in=expires_in, user=user_to_out(user), refresh_token=refresh_token)

@router.post("/login", response_model=AuthResponse)
@limiter.limit("5/minute")
def login(request: Request, payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == payload.email).first()
    if not user or not pwd_context.verify(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    
    # Include role in token for unified authentication
    token_data = {"sub": user.uid, "role": user.role}
    token, expires_in = create_access_token(token_data)
    refresh_token = create_refresh_token(token_data)
    
    return AuthResponse(access_token=token, expires_in=expires_in, user=user_to_out(user), refresh_token=refresh_token)


# ============================================================
# Google Sign-In (users only — admin stays credentials-only)
# ============================================================

from pydantic import BaseModel as _PydanticBase

class GoogleSignInRequest(_PydanticBase):
    id_token: str

@router.post("/google-signin", response_model=AuthResponse)
@limiter.limit("10/minute")
async def google_signin(request: Request, payload: GoogleSignInRequest, db: Session = Depends(get_db)):
    """
    Authenticate or register a user via Google Sign-In.
    
    1. Verify the Google ID token using Google's tokeninfo endpoint
    2. If user exists with that email → login
    3. If user does NOT exist → auto-register
    4. Admin accounts cannot be created via Google Sign-In
    """
    # Step 1: Verify Google ID token
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"https://oauth2.googleapis.com/tokeninfo?id_token={payload.id_token}"
            )
        
        if resp.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid Google ID token"
            )
        
        google_data = resp.json()
        
        # Validate the token
        google_email = google_data.get("email")
        email_verified = google_data.get("email_verified", "false")
        google_name = google_data.get("name", "")
        google_picture = google_data.get("picture", "")
        
        if not google_email:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No email in Google token"
            )
        
        if email_verified not in ("true", True):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Google email not verified"
            )
        
    except httpx.RequestError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Could not verify Google token (network error)"
        )
    
    # Step 2: Find or create user
    user = db.query(models.User).filter(models.User.email == google_email).first()
    
    if user:
        # Existing user — block admin accounts from Google Sign-In
        if user.role == "admin":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Admin accounts must use credential login"
            )
        
        # Update profile picture if not set
        if not user.profile_image_url and google_picture:
            user.profile_image_url = google_picture
            user.updated_at = datetime.utcnow()
            db.commit()
            db.refresh(user)
    else:
        # New user — auto-register
        # Generate unique username from Google name or email
        base_username = google_name.replace(" ", "_").lower()[:40] if google_name else google_email.split("@")[0]
        username = base_username
        counter = 1
        while db.query(models.User).filter(models.User.username == username).first():
            username = f"{base_username}_{counter}"
            counter += 1
        
        # Create user with a random password hash (they'll use Google to sign in)
        import secrets
        random_password = secrets.token_urlsafe(32)
        
        try:
            user = models.User(
                email=google_email,
                username=username,
                display_name=google_name or google_email.split("@")[0],
                password_hash=pwd_context.hash(random_password),
                profile_image_url=google_picture or None,
                role="user",  # Never admin via Google
            )
            db.add(user)
            db.commit()
            db.refresh(user)
        except IntegrityError:
            db.rollback()
            # Race condition — user was just created by another request
            user = db.query(models.User).filter(models.User.email == google_email).first()
            if not user:
                raise HTTPException(status_code=500, detail="Registration failed")
    
    # Step 3: Issue JWT tokens (same as regular login)
    token_data = {"sub": user.uid, "role": user.role}
    token, expires_in = create_access_token(token_data)
    refresh_token = create_refresh_token(token_data)
    
    return AuthResponse(
        access_token=token,
        expires_in=expires_in,
        user=user_to_out(user),
        refresh_token=refresh_token
    )


from fastapi import Header
from jose import JWTError

@router.get("/me", response_model=UserOut)
def me(authorization: str | None = Header(default=None), db: Session = Depends(get_db)):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Not authenticated")
    token = authorization.split(" ", 1)[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        uid = payload.get("sub")
        if not uid:
            raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    # H-3: Check if token has been revoked (blacklisted)
    jti = payload.get("jti")
    if jti and db.query(models.RevokedToken).filter(models.RevokedToken.jti == jti).first():
        raise HTTPException(status_code=401, detail="Token has been revoked")

    user = db.query(models.User).filter(models.User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user_to_out(user)

# Dependency to get the current authenticated user (for protected routes)
def get_current_user(authorization: str | None = Header(default=None), db: Session = Depends(get_db)) -> models.User:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Not authenticated")
    token = authorization.split(" ", 1)[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        uid = payload.get("sub")
        if not uid:
            raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    # H-3: Check if token has been revoked (blacklisted)
    jti = payload.get("jti")
    if jti and db.query(models.RevokedToken).filter(models.RevokedToken.jti == jti).first():
        raise HTTPException(status_code=401, detail="Token has been revoked")

    user = db.query(models.User).filter(models.User.uid == uid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

@router.post("/refresh", response_model=AuthResponse)
def refresh_token(payload: RefreshTokenRequest, db: Session = Depends(get_db)):
    """Refresh access token using refresh token"""
    try:
        decoded = jwt.decode(payload.refresh_token, SECRET_KEY, algorithms=[ALGORITHM])
        token_type = decoded.get("type")
        
        # Verify it's a refresh token
        if token_type != "refresh":
            raise HTTPException(status_code=401, detail="Invalid token type")
        
        uid = decoded.get("sub")
        if not uid:
            raise HTTPException(status_code=401, detail="Invalid token")
        
        # Get user
        user = db.query(models.User).filter(models.User.uid == uid).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        
        # Generate new tokens
        new_access_token, expires_in = create_access_token({"sub": user.uid})
        new_refresh_token = create_refresh_token({"sub": user.uid})
        
        return AuthResponse(
            access_token=new_access_token,
            expires_in=expires_in,
            user=user_to_out(user),
            refresh_token=new_refresh_token
        )
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired refresh token")


# Dependency to get the current authenticated user (optional, for routes that work with or without auth)
def get_current_user_optional(authorization: str | None = Header(default=None), db: Session = Depends(get_db)) -> models.User | None:
    try:
        if not authorization or not authorization.lower().startswith("bearer "):
            return None
        token = authorization.split(" ", 1)[1]
        if not token:
            return None
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_uid: str = payload.get("sub")
        if not user_uid:
            return None
        user = db.query(models.User).filter(models.User.uid == user_uid).first()
        return user
    except:
        return None

# Dependency to get the current user UID (string) - convenience wrapper
def get_current_user_uid(current_user: models.User = Depends(get_current_user)) -> str:
    """Return just the UID string of the current authenticated user."""
    return current_user.uid


# ============================================
# ADMIN AUTHENTICATION
# ============================================

def get_current_admin_user(
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_db)
) -> models.User:
    """
    Dependency to get the current authenticated admin user.
    Raises 401 if not authenticated or not an admin.
    Supports both legacy token format (user_id claim) and unified format (sub claim).
    """
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated as admin"
        )
    
    token = authorization.split(" ", 1)[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        # Support both claim formats for backward compatibility
        user_id = payload.get("user_id")
        user_uid = payload.get("sub")
        if not user_id and not user_uid:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid admin token"
            )
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin token"
        )
    
    # H-3: Check if token has been revoked (blacklisted)
    jti = payload.get("jti")
    if jti and db.query(models.RevokedToken).filter(models.RevokedToken.jti == jti).first():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Admin token has been revoked"
        )
    
    # Look up admin by id (legacy) or uid (unified)
    admin = None
    if user_id:
        admin = db.query(models.User).filter(
            models.User.id == user_id,
            models.User.role == "admin"
        ).first()
    if not admin and user_uid:
        admin = db.query(models.User).filter(
            models.User.uid == user_uid,
            models.User.role == "admin"
        ).first()
    
    if not admin:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Admin not found or insufficient permissions"
        )
    
    return admin


def require_admin_role(*allowed_roles: str):
    """
    Dependency factory to check if admin has required role.
    Usage: Depends(require_admin_role("admin"))
    Note: All admin users now have role='admin' in the users table.
    """
    def check_role(admin: models.User = Depends(get_current_admin_user)) -> models.User:
        if admin.role not in allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Insufficient permissions. Required roles: {', '.join(allowed_roles)}"
            )
        return admin
    return check_role


@router.post("/admin/login")
@limiter.limit("3/minute")
def admin_login(request: Request, payload: LoginRequest, db: Session = Depends(get_db)):
    """Admin login endpoint - returns admin token for users with role='admin'"""
    admin = db.query(models.User).filter(
        models.User.email == payload.email,
        models.User.role == "admin"
    ).first()
    
    if not admin or not pwd_context.verify(payload.password, admin.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin credentials"
        )
    
    # Create admin token with unified claims (sub=uid for consistency, user_id kept for backward compat)
    token_data = {"sub": admin.uid, "user_id": admin.id, "role": admin.role}
    token, expires_in = create_access_token(token_data)
    
    return {
        "access_token": token,
        "token_type": "bearer",
        "expires_in": expires_in,
        "admin": {
            "id": admin.id,
            "username": admin.username,
            "email": admin.email,
            "display_name": admin.display_name,
            "role": admin.role
        }
    }
# ============================================================
# Change Password (Authenticated User)
# ============================================================

from pydantic import BaseModel as PydanticBaseModel

class ChangePasswordRequest(PydanticBaseModel):
    current_password: str
    new_password: str

@router.post("/change-password")
def change_password(
    payload: ChangePasswordRequest,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Change password for the currently authenticated user.
    Requires the current password for verification."""
    
    # Verify current password
    if not pwd_context.verify(payload.current_password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Current password is incorrect"
        )
    
    # Validate new password length (aligned with client-side validation)
    if len(payload.new_password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="New password must be at least 8 characters"
        )
    
    # Ensure new password is different
    if pwd_context.verify(payload.new_password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="New password must be different from current password"
        )
    
    # Update password
    current_user.password_hash = pwd_context.hash(payload.new_password)
    current_user.updated_at = datetime.utcnow()
    db.commit()
    
    return {"message": "Password changed successfully"}

# ============================================================
# Password Reset Endpoints
# ============================================================

@router.post("/request-password-reset")
@limiter.limit("3/hour")
def request_password_reset(
    request: Request,
    payload: PasswordResetRequest,
    db: Session = Depends(get_db)
):
    """
    Request a password reset email.
    
    For MVP, we return a reset token directly (no email service configured).
    In production, this should send an email with a secure link.
    """
    user = db.query(models.User).filter(models.User.email == payload.email).first()
    
    # Security: Don't reveal if email exists or not (prevent enumeration)
    # Always return success, but only create token if user exists
    if user:
        # Create a short-lived token (15 minutes)
        reset_token_data = {
            "user_id": user.uid,
            "type": "password_reset",
            "email": user.email
        }
        reset_token = jwt.encode(
            {**reset_token_data, "exp": datetime.utcnow() + timedelta(minutes=15)},
            SECRET_KEY,
            algorithm=ALGORITHM
        )
        
        # Security: Token is NOT returned in the response.
        # In production, send email with reset link containing this token.
        # For MVP, log the token server-side for manual testing.
        print(f"[MVP] Password reset token for {payload.email}: {reset_token}")
    
    return {"message": "Password reset instructions sent to your email"}


@router.post("/confirm-password-reset")
@limiter.limit("5/hour")
def confirm_password_reset(
    request: Request,
    payload: PasswordResetConfirm,
    db: Session = Depends(get_db)
):
    """
    Confirm password reset with token and set new password.
    """
    try:
        # Decode and verify token
        token_data = jwt.decode(payload.token, SECRET_KEY, algorithms=[ALGORITHM])
        
        # Verify it's a password reset token
        if token_data.get("type") != "password_reset":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid reset token"
            )
        
        user_id = token_data.get("user_id")
        user_email = token_data.get("email")
        
        if not user_id or not user_email:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid token data"
            )
        
        # Find user
        user = db.query(models.User).filter(
            models.User.uid == user_id,
            models.User.email == user_email
        ).first()
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Update password
        user.password_hash = pwd_context.hash(payload.new_password)
        user.updated_at = datetime.utcnow()
        db.commit()
        
        return {"message": "Password reset successfully"}
        
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reset token has expired"
        )
    except jwt.JWTError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid reset token"
        )


# ============================================
# LOGOUT (H-3: TOKEN BLACKLISTING)
# ============================================

@router.post("/logout")
def logout(
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_db)
):
    """
    Logout the current user by revoking their access token.
    The token's JTI is added to the revoked_tokens table so it cannot be reused.
    """
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Not authenticated")
    
    token = authorization.split(" ", 1)[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.ExpiredSignatureError:
        # Token already expired — no need to revoke
        return {"message": "Token already expired"}
    except jwt.JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    jti = payload.get("jti")
    if not jti:
        # Legacy token without JTI — cannot be blacklisted but still "logged out"
        return {"message": "Logged out (legacy token)"}
    
    # Check if already revoked
    existing = db.query(models.RevokedToken).filter(models.RevokedToken.jti == jti).first()
    if existing:
        return {"message": "Token already revoked"}
    
    # Revoke the token
    exp_timestamp = payload.get("exp")
    expires_at = datetime.utcfromtimestamp(exp_timestamp) if exp_timestamp else datetime.utcnow() + timedelta(hours=1)
    
    revoked = models.RevokedToken(
        jti=jti,
        user_uid=payload.get("sub", "unknown"),
        expires_at=expires_at
    )
    db.add(revoked)
    db.commit()
    
    return {"message": "Successfully logged out"}


@router.delete("/revoked-tokens/expired")
def cleanup_expired_revoked_tokens(
    admin: models.User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    """Admin-only: Remove expired revoked tokens to keep the table small."""
    deleted = db.query(models.RevokedToken).filter(
        models.RevokedToken.expires_at < datetime.utcnow()
    ).delete()
    db.commit()
    return {"message": f"Cleaned up {deleted} expired revoked tokens"}
