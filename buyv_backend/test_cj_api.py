"""Test script pour vérifier l'authentification CJ API"""
import asyncio
import httpx
import os
from dotenv import load_dotenv
from pathlib import Path

# Charger .env
env_path = Path(__file__).parent / '.env'
load_dotenv(dotenv_path=env_path)

token = os.getenv('CJ_API_KEY')
account_id = os.getenv('CJ_ACCOUNT_ID')

print("=" * 60)
print("CJ API Test")
print("=" * 60)
print(f"Token: {token}")
print(f"Account ID: {account_id}")
print(f"Token length: {len(token) if token else 0}")
print("=" * 60)

async def test_api():
    """Test l'API CJ avec différentes configurations"""
    
    # Configuration 1: Header CJ-Access-Token
    headers1 = {
        'CJ-Access-Token': token,
        'Content-Type': 'application/json'
    }
    
    # Configuration 2: Authorization Bearer
    headers2 = {
        'Authorization': f'Bearer {token}',
        'Content-Type': 'application/json'
    }
    
    # Configuration 3: Avec Account ID dans le header
    headers3 = {
        'CJ-Access-Token': token,
        'CJ-Account-Id': account_id,
        'Content-Type': 'application/json'
    }
    
    async with httpx.AsyncClient() as client:
        url = 'https://developers.cjdropshipping.com/api2.0/v1/product/list'
        params = {'pageNum': 1, 'pageSize': 5}
        
        print("\n🔍 Test 1: CJ-Access-Token header")
        print(f"Headers: {headers1}")
        try:
            response = await client.get(url, headers=headers1, params=params, timeout=10)
            print(f"Status: {response.status_code}")
            print(f"Response: {response.text[:500]}")
        except Exception as e:
            print(f"Error: {e}")
        
        print("\n🔍 Test 2: Authorization Bearer")
        print(f"Headers: {headers2}")
        try:
            response = await client.get(url, headers=headers2, params=params, timeout=10)
            print(f"Status: {response.status_code}")
            print(f"Response: {response.text[:500]}")
        except Exception as e:
            print(f"Error: {e}")
        
        print("\n🔍 Test 3: CJ-Access-Token + CJ-Account-Id")
        print(f"Headers: {headers3}")
        try:
            response = await client.get(url, headers=headers3, params=params, timeout=10)
            print(f"Status: {response.status_code}")
            print(f"Response: {response.text[:500]}")
        except Exception as e:
            print(f"Error: {e}")

if __name__ == '__main__':
    asyncio.run(test_api())
