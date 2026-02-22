"""
Génère un Access Token CJ Dropshipping.
Essaie d'abord le refresh token, puis l'API key.
Documentation: https://developers.cjdropshipping.cn/en/api/api2/api/auth.html
"""
import httpx
import os
import re
from dotenv import load_dotenv
from pathlib import Path

# Charger .env
env_path = Path(__file__).parent / '.env'
load_dotenv(dotenv_path=env_path)

api_key = os.getenv('CJ_API_KEY', '').strip()
refresh_token = os.getenv('CJ_REFRESH_TOKEN', '').strip()
email = os.getenv('CJ_EMAIL', '').strip()
base_url = os.getenv('CJ_BASE_URL', 'https://developers.cjdropshipping.com/api2.0/v1').strip()

print("=" * 60)
print("CJ Dropshipping - Generate / Refresh Access Token")
print("=" * 60)
print(f"Email: {email}")
print(f"Refresh token present: {'YES' if refresh_token else 'NO'}")
print(f"API key present: {'YES' if api_key else 'NO'}")
print("=" * 60)


def update_env(access_token: str, new_refresh: str):
    """Rewrite CJ_API_KEY and CJ_REFRESH_TOKEN in .env"""
    content = env_path.read_text(encoding='utf-8')
    content = re.sub(r'^CJ_API_KEY=.*$', f'CJ_API_KEY={access_token}', content, flags=re.MULTILINE)
    content = re.sub(r'^CJ_REFRESH_TOKEN=.*$', f'CJ_REFRESH_TOKEN={new_refresh}', content, flags=re.MULTILINE)
    env_path.write_text(content, encoding='utf-8')
    print(f"\n✅ .env updated with new tokens")


def print_tokens(access_token: str, refresh: str, expiry: str = ""):
    print("\n" + "=" * 60)
    print("🔑 Access Token (use as CJ_API_KEY):")
    print(f"   {access_token}")
    if expiry:
        print(f"⏰ Expires: {expiry}")
    print("🔄 Refresh Token (use as CJ_REFRESH_TOKEN):")
    print(f"   {refresh}")
    print("=" * 60)


def try_refresh_token():
    """Use CJ_REFRESH_TOKEN to get a new access token."""
    if not refresh_token:
        print("\u26a0️  No CJ_REFRESH_TOKEN found in .env — skipping refresh attempt")
        return False

    url = f"{base_url}/authentication/refreshAccessToken"
    print(f"\n🔄 Trying refresh token → {url}")
    try:
        resp = httpx.post(url, json={"refreshToken": refresh_token},
                          headers={"Content-Type": "application/json"}, timeout=30)
        print(f"   Status: {resp.status_code}")
        print(f"   Body: {resp.text[:400]}")
        data = resp.json()
        if data.get('code') == 200:
            td = data.get('data', {})
            new_access = td.get('accessToken', '')
            new_refresh = td.get('refreshToken', refresh_token)
            expiry = td.get('accessTokenExpiryDate', '')
            if new_access:
                print("\n✅ Refresh successful!")
                print_tokens(new_access, new_refresh, expiry)
                update_env(new_access, new_refresh)
                return True
        print(f"\n❌ Refresh failed: code={data.get('code')} msg={data.get('message')}")
        return False
    except Exception as e:
        print(f"\n❌ Refresh exception: {e}")
        return False


def try_api_key():
    """Use CJ_API_KEY (old static key) to get a new JWT access token."""
    if not api_key:
        print("⚠️  No CJ_API_KEY found in .env")
        return False

    url = f"{base_url}/authentication/getAccessToken"
    print(f"\n🔑 Trying API key → {url}")
    try:
        resp = httpx.post(url, json={"apiKey": api_key},
                          headers={"Content-Type": "application/json"}, timeout=30)
        print(f"   Status: {resp.status_code}")
        print(f"   Body: {resp.text[:400]}")
        data = resp.json()
        if data.get('code') == 200:
            td = data.get('data', {})
            new_access = td.get('accessToken', '')
            new_refresh = td.get('refreshToken', '')
            expiry = td.get('accessTokenExpiryDate', '')
            if new_access:
                print("\n✅ Got new access token via API key!")
                print_tokens(new_access, new_refresh or refresh_token, expiry)
                update_env(new_access, new_refresh or refresh_token)
                return True
        print(f"\n❌ API key auth failed: code={data.get('code')} msg={data.get('message')}")
        return False
    except Exception as e:
        print(f"\n❌ API key exception: {e}")
        return False


if __name__ == '__main__':
    # Try refresh token first (preferred — doesn't require stored API key)
    if try_refresh_token():
        print("\n🚀 Token renewed. Restart the backend (uvicorn will reload automatically).")
    elif try_api_key():
        print("\n🚀 Token obtained via API key. Restart the backend.")
    else:
        print("\n❌ Could not obtain a new token automatically.")
        print("   → Go to https://cjdropshipping.com → Settings → API → Generate Token")
        print("   → Paste the new JWT into .env as CJ_API_KEY=<token>")
        print(f"   → Your CJ account: {email}")
