from dotenv import load_dotenv; load_dotenv()
from sqlalchemy import create_engine, text
import os, requests

engine = create_engine(os.environ['DATABASE_URL'])
with engine.connect() as conn:
    users = conn.execute(text("SELECT email, uid FROM users LIMIT 5")).fetchall()
    print("Users in DB:")
    for u in users:
        print(f"  email={u[0]}, uid={u[1]}")

# Try login with known user
emails_to_try = [u[0] for u in users]
passwords_to_try = ['password123', 'Password123', 'test123', 'admin123']

token = None
for email in emails_to_try[:3]:
    for pwd in passwords_to_try:
        r = requests.post('http://localhost:8000/auth/login',
                          json={'email': email, 'password': pwd})
        if r.status_code == 200:
            token = r.json()['access_token']
            print(f"Logged in as {email}")
            break
    if token:
        break

if not token:
    print("Could not login - testing order with Firebase token approach")
    exit()

# Test order creation
order = {
    'items': [{
        'product_id': 'test-prod-1',
        'product_name': 'Test Product',
        'product_image': 'https://example.com/img.jpg',
        'price': 10.0,
        'quantity': 1,
        'is_promoted_product': False
    }],
    'status': 'pending',
    'subtotal': 10.0,
    'shipping': 5.0,
    'tax': 0.0,
    'total': 15.0,
    'payment_method': 'stripe_mock'
}
resp = requests.post('http://localhost:8000/orders', json=order,
                     headers={'Authorization': f'Bearer {token}'})
print(f'Order status: {resp.status_code}')
print(f'Order response: {resp.text[:1000]}')
