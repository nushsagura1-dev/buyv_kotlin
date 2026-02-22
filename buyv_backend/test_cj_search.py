"""Test de recherche produits CJ API"""
import asyncio
import httpx
import os
import json
from dotenv import load_dotenv
from pathlib import Path

# Charger .env
env_path = Path(__file__).parent / '.env'
load_dotenv(dotenv_path=env_path)

token = os.getenv('CJ_API_KEY')

print("=" * 60)
print("CJ API - Test Product Search")
print("=" * 60)
print(f"Token: {token[:50]}...")
print("=" * 60)

async def test_search():
    """Test différents paramètres de recherche"""
    
    headers = {
        'CJ-Access-Token': token,
        'Content-Type': 'application/json'
    }
    
    # Test 1: productNameEn
    print("\n🔍 Test 1: Paramètre productNameEn")
    async with httpx.AsyncClient() as client:
        url = 'https://developers.cjdropshipping.com/api2.0/v1/product/list'
        params1 = {
            'productNameEn': 'laptop',
            'pageNum': 1,
            'pageSize': 5
        }
        print(f"URL: {url}")
        print(f"Params: {params1}")
        
        try:
            response = await client.get(url, headers=headers, params=params1, timeout=30)
            print(f"Status: {response.status_code}")
            data = response.json()
            print(f"Code: {data.get('code')}")
            print(f"Message: {data.get('message')}")
            print(f"Data keys: {list(data.get('data', {}).keys())}")
            if 'list' in data.get('data', {}):
                print(f"Products count: {len(data.get('data', {}).get('list', []))}")
            print(f"Full response: {json.dumps(data, indent=2)[:1000]}")
        except Exception as e:
            print(f"Error: {e}")
    
    # Test 2: categoryId (Electronics)
    print("\n🔍 Test 2: Paramètre categoryId seul")
    async with httpx.AsyncClient() as client:
        params2 = {
            'categoryId': '1',  # Electronics
            'pageNum': 1,
            'pageSize': 5
        }
        print(f"Params: {params2}")
        
        try:
            response = await client.get(url, headers=headers, params=params2, timeout=30)
            print(f"Status: {response.status_code}")
            data = response.json()
            print(f"Code: {data.get('code')}")
            print(f"Message: {data.get('message')}")
            if 'list' in data.get('data', {}):
                print(f"Products count: {len(data.get('data', {}).get('list', []))}")
        except Exception as e:
            print(f"Error: {e}")
    
    # Test 3: Sans paramètres (liste complète)
    print("\n🔍 Test 3: Sans filtres de recherche")
    async with httpx.AsyncClient() as client:
        params3 = {
            'pageNum': 1,
            'pageSize': 5
        }
        print(f"Params: {params3}")
        
        try:
            response = await client.get(url, headers=headers, params=params3, timeout=30)
            print(f"Status: {response.status_code}")
            data = response.json()
            print(f"Code: {data.get('code')}")
            print(f"Message: {data.get('message')}")
            if 'list' in data.get('data', {}):
                products = data.get('data', {}).get('list', [])
                print(f"Products count: {len(products)}")
                if products:
                    print(f"\nPremier produit trouvé:")
                    print(f"  - ID: {products[0].get('pid')}")
                    print(f"  - Name: {products[0].get('productNameEn')}")
                    print(f"  - Price: {products[0].get('sellPrice')}")
        except Exception as e:
            print(f"Error: {e}")

if __name__ == '__main__':
    asyncio.run(test_search())
