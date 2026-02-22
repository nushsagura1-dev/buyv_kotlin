import sys
import os
sys.path.append(os.getcwd())

try:
    from app.database import SessionLocal
    from app.models import User

    db = SessionLocal()
    email = "admin@buyv.com"
    user = db.query(User).filter(User.email == email).first()

    if user:
        user.role = "admin"
        db.commit()
        print(f"SUCCESS: User {email} promoted to admin.")
        print(f"ID: {user.id}, New Role: {user.role}")
    else:
        print(f"ERROR: User {email} not found in database.")
        print("Existing users:")
        all_users = db.query(User).all()
        for u in all_users:
            print(f"- {u.email} ({u.role})")

except Exception as e:
    print(f"Error during execution: {e}")
    import traceback
    traceback.print_exc()
