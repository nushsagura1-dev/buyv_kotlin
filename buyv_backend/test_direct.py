import requests
import json

BASE_URL = "http://localhost:8000"

# Register
print("1. Registering user...")
import random
username = f"testdir{random.randint(1000,9999)}"
reg_data = {
    "email": f"{username}@example.com",
    "password": "Pass123!",
    "username": username,
    "display_name": "Test Direct"
}
reg_response = requests.post(f"{BASE_URL}/auth/register", json=reg_data)
print(f"Register status: {reg_response.status_code}")
if reg_response.status_code == 200:
    token = reg_response.json()["access_token"]
    print(f"Token: {token[:40]}...")
else:
    print(f"Error: {reg_response.text}")
    exit(1)

# Create post
print("\n2. Creating post...")
headers = {"Authorization": f"Bearer {token}"}
post_data = {
    "type": "reel",
    "mediaUrl": "https://example.com/video.mp4",
    "caption": "Test post from Python"
}
try:
    post_response = requests.post(f"{BASE_URL}/posts/", json=post_data, headers=headers)
    print(f"Create post status: {post_response.status_code}")
    if post_response.status_code == 200:
        post = post_response.json()
        post_id = post["id"]
        print(f"✅ Post created: {post_id}")
        print(f"   Type: {post['type']}, Likes: {post['likesCount']}")
    else:
        print(f"❌ Error: {post_response.text}")
        exit(1)
except Exception as e:
    print(f"❌ Exception: {e}")
    import traceback
    traceback.print_exc()
    exit(1)

print("\nTest completed!")
