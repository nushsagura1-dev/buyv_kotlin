import sys
import os
sys.path.append(os.getcwd())

try:
    from app.database import SessionLocal
    from app.models import User

    db = SessionLocal()
    users = db.query(User).all()

    print(f"{'ID':<5} | {'Email':<30} | {'Role':<10}")
    print("-" * 50)
    for user in users:
        print(f"{str(user.id):<5} | {user.email:<30} | {user.role:<10}")

    print("\nTo make a user admin, you can run the following SQL command in your database tool:")
    print("UPDATE users SET role = 'admin' WHERE email = 'USER_EMAIL';")

except Exception as e:
    print(f"Error during execution: {e}")
    import traceback
    traceback.print_exc()
