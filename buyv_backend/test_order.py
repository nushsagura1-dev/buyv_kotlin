import requests

# Login
r = requests.post('http://localhost:8000/auth/login', json={'email':'anasjafir@gmail.com','password':'password123'})
print('Login status:', r.status_code)
if r.status_code != 200:
    print('Login error:', r.text)
    exit()
token = r.json()['access_token']
print('Got token')

# POST order
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
    'payment_method': 'stripe'
}
resp = requests.post('http://localhost:8000/orders', json=order, headers={'Authorization': f'Bearer {token}'})
print('Order status:', resp.status_code)
print('Order response:', resp.text[:2000])
