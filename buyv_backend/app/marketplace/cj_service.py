"""
Service d'intégration avec l'API CJ Dropshipping.

Documentation API: https://developers.cjdropshipping.com/
"""
import httpx
import os
import re
from pathlib import Path
from typing import List, Dict, Any, Optional
from decimal import Decimal
import decimal
import logging
from dotenv import load_dotenv

# Charger .env depuis buyv_backend/
env_path = Path(__file__).parent.parent.parent / '.env'
load_dotenv(dotenv_path=env_path)

logger = logging.getLogger(__name__)


class CJAuthError(Exception):
    """Raised when CJ API authentication fails and cannot be refreshed."""
    pass


class CJDropshippingService:
    """Service pour interagir avec l'API CJ Dropshipping."""
    
    def __init__(self):
        self.api_key = os.getenv("CJ_API_KEY", "").strip()
        self.refresh_token = os.getenv("CJ_REFRESH_TOKEN", "").strip()
        self.account_id = os.getenv("CJ_ACCOUNT_ID", "").strip()
        self.email = os.getenv("CJ_EMAIL", "").strip()
        self.base_url = os.getenv("CJ_BASE_URL", "https://developers.cjdropshipping.com/api2.0/v1").strip()
        self._env_path = env_path
        
        if not self.account_id:
            raise ValueError("CJ_ACCOUNT_ID not configured in .env")
        
        logger.info(f"CJ Service initialized - Account: {self.account_id}, URL: {self.base_url}")
    
    def _get_headers(self) -> Dict[str, str]:
        return {
            "CJ-Access-Token": self.api_key,
            "Content-Type": "application/json"
        }

    async def _refresh_access_token(self) -> bool:
        """
        Use CJ_REFRESH_TOKEN to obtain a new access token.
        Updates self.api_key and rewrites the .env file on success.
        Returns True on success, False on failure.
        """
        if not self.refresh_token:
            logger.error("CJ_REFRESH_TOKEN not set — cannot auto-refresh")
            return False

        url = f"{self.base_url}/authentication/refreshAccessToken"
        payload = {"refreshToken": self.refresh_token}
        logger.info(f"CJ: attempting token refresh via {url}")

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(url, json=payload, headers={"Content-Type": "application/json"})
                logger.info(f"CJ refresh response {resp.status_code}: {resp.text[:300]}")
                data = resp.json()

                if data.get("code") == 200:
                    token_data = data.get("data", {})
                    new_access = token_data.get("accessToken", "")
                    new_refresh = token_data.get("refreshToken", "")

                    if new_access:
                        self.api_key = new_access.strip()
                        if new_refresh:
                            self.refresh_token = new_refresh.strip()
                        self._update_env_tokens(new_access, new_refresh or self.refresh_token)
                        logger.info(f"CJ token refreshed successfully. New token starts: {new_access[:30]}...")
                        return True
                    else:
                        logger.error("CJ refresh returned 200 but no accessToken in data")
                        return False
                else:
                    logger.error(f"CJ refresh failed: code={data.get('code')} msg={data.get('message')}")
                    return False
        except Exception as e:
            logger.error(f"CJ refresh exception: {e}")
            return False

    def _update_env_tokens(self, access_token: str, refresh_token: str):
        """Rewrite CJ_API_KEY and CJ_REFRESH_TOKEN lines in the .env file."""
        try:
            content = self._env_path.read_text(encoding="utf-8")
            content = re.sub(r'^CJ_API_KEY=.*$', f'CJ_API_KEY={access_token}', content, flags=re.MULTILINE)
            content = re.sub(r'^CJ_REFRESH_TOKEN=.*$', f'CJ_REFRESH_TOKEN={refresh_token}', content, flags=re.MULTILINE)
            self._env_path.write_text(content, encoding="utf-8")
            # Reload env so os.getenv picks up the new values in this process
            os.environ["CJ_API_KEY"] = access_token
            os.environ["CJ_REFRESH_TOKEN"] = refresh_token
            logger.info(".env updated with new CJ tokens")
        except Exception as e:
            logger.warning(f"Could not update .env with new tokens: {e}")

    async def _get(self, path: str, params: Dict = None) -> Dict[str, Any]:
        """
        Make an authenticated GET request to CJ API.
        Auto-refreshes token on 401 and retries once.
        Raises CJAuthError if auth cannot be fixed.
        """
        for attempt in range(2):  # 0 = first try, 1 = retry after refresh
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.get(
                    f"{self.base_url}{path}",
                    headers=self._get_headers(),
                    params=params or {}
                )
                logger.info(f"CJ GET {path} → {resp.status_code}")

                if resp.status_code == 401:
                    if attempt == 0:
                        logger.warning("CJ 401 — attempting token refresh")
                        refreshed = await self._refresh_access_token()
                        if not refreshed:
                            raise CJAuthError(
                                "CJ API token expired and refresh failed. "
                                "Please renew your CJ token: run generate_cj_token.py "
                                "or visit https://cjdropshipping.com → Settings → API."
                            )
                        continue  # retry with new token
                    else:
                        raise CJAuthError("CJ API authentication failed even after token refresh.")

                resp.raise_for_status()
                return resp.json()

        raise CJAuthError("CJ API request failed after token refresh.")

    async def _post(self, path: str, payload: Dict) -> Dict[str, Any]:
        """
        Make an authenticated POST request to CJ API.
        Auto-refreshes token on 401 and retries once.
        """
        for attempt in range(2):
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(
                    f"{self.base_url}{path}",
                    headers=self._get_headers(),
                    json=payload
                )
                logger.info(f"CJ POST {path} → {resp.status_code}")

                if resp.status_code == 401:
                    if attempt == 0:
                        refreshed = await self._refresh_access_token()
                        if not refreshed:
                            raise CJAuthError(
                                "CJ API token expired and refresh failed. Please renew CJ_API_KEY."
                            )
                        continue
                    else:
                        raise CJAuthError("CJ API authentication failed even after token refresh.")

                resp.raise_for_status()
                return resp.json()

        raise CJAuthError("CJ API request failed after token refresh.")
    
    async def search_products(
        self,
        query: str,
        category: Optional[str] = None,
        page: int = 1,
        page_size: int = 20
    ) -> Dict[str, Any]:
        """Recherche de produits sur CJ Dropshipping."""
        params = {"productNameEn": query, "pageNum": page, "pageSize": page_size}
        if category:
            params["categoryId"] = category

        data = await self._get("/product/list", params)
        if data.get("code") == 200:
            return data.get("data", {})
        else:
            logger.error(f"CJ search error: code={data.get('code')} msg={data.get('message')}")
            return {"list": [], "total": 0}
    
    async def get_product_details(self, product_id: str) -> Dict[str, Any]:
        """Obtenir les détails d'un produit CJ."""
        data = await self._get("/product/query", {"pid": product_id})
        if data.get("code") == 200:
            return data.get("data", {})
        else:
            raise Exception(f"Product not found: {product_id} — {data.get('message')}")
    
    async def get_product_variants(self, product_id: str) -> List[Dict[str, Any]]:
        """
        Obtenir les variantes d'un produit.
        
        Args:
            product_id: ID du produit CJ
            
        Returns:
            Liste des variantes
        """
        try:
            product_data = await self.get_product_details(product_id)
            return product_data.get("variants", [])
        except Exception as e:
            logger.error(f"Failed to get variants: {str(e)}")
            return []
    
    async def get_categories(self) -> List[Dict[str, Any]]:
        """Obtenir la liste des catégories CJ."""
        try:
            data = await self._get("/product/getCategory")
            if data.get("code") == 200:
                return data.get("data", [])
            return []
        except Exception as e:
            logger.error(f"Failed to get categories: {e}")
            return []
    
    async def check_inventory(self, product_id: str, variant_id: Optional[str] = None) -> Dict[str, Any]:
        """Vérifier le stock d'un produit."""
        try:
            params = {"pid": product_id}
            if variant_id:
                params["vid"] = variant_id
            data = await self._get("/product/inventory", params)
            if data.get("code") == 200:
                return data.get("data", {})
            return {"in_stock": False, "quantity": 0}
        except Exception as e:
            logger.error(f"Failed to check inventory: {e}")
            return {"in_stock": False, "quantity": 0}
    
    async def create_order(self, order_data: Dict[str, Any]) -> Dict[str, Any]:
        """Créer une commande sur CJ Dropshipping."""
        data = await self._post("/order/createOrder", order_data)
        if data.get("code") == 200:
            return data.get("data", {})
        raise Exception(f"CJ order creation failed: {data.get('message')}")
    
    def parse_product_data(self, cj_product: Dict[str, Any]) -> Dict[str, Any]:
        """
        Parser les données CJ en format utilisable.
        
        Args:
            cj_product: Données brutes du produit CJ
            
        Returns:
            Dict formaté pour notre système
        """
        import json
        
        # Parse product image - peut être une URL string ou un JSON array
        product_image = cj_product.get("productImage", "")
        images_list = []
        main_image = ""
        
        if isinstance(product_image, str):
            if product_image.startswith('['):
                # C'est un JSON array
                try:
                    images_list = json.loads(product_image)
                    main_image = images_list[0] if images_list else ""
                except:
                    main_image = product_image
            else:
                # C'est une URL simple
                main_image = product_image
                images_list = [product_image]
        elif isinstance(product_image, list):
            images_list = product_image
            main_image = images_list[0] if images_list else ""
        
        # Parse selling price - handle range format
        sell_price_str = str(cj_product.get("sellPrice", "0"))
        if '--' in sell_price_str:
            sell_price_str = sell_price_str.split('--')[0].strip()
        
        # Valider et nettoyer le prix
        try:
            sell_price_str = sell_price_str.strip()
            if not sell_price_str or sell_price_str.lower() in ['none', 'null', '']:
                sell_price_str = "0"
            selling_price = Decimal(sell_price_str)
        except (ValueError, decimal.InvalidOperation, decimal.ConversionSyntax):
            selling_price = Decimal("0")
        
        # Parse suggested retail price (compare-at / original/strike-through price)
        suggest_price_str = str(cj_product.get("suggestSellPrice", "0"))
        if '--' in suggest_price_str:
            suggest_price_str = suggest_price_str.split('--')[0].strip()
        try:
            suggest_price_str = suggest_price_str.strip()
            if not suggest_price_str or suggest_price_str.lower() in ['none', 'null', '']:
                suggest_price_str = "0"
            original_price = Decimal(suggest_price_str)
        except (ValueError, decimal.InvalidOperation):
            original_price = Decimal("0")
        # Fall back to selling_price if no retail price available
        if original_price <= 0:
            original_price = selling_price
        
        return {
            "name": cj_product.get("productNameEn", "")[:200],  # Limit name length
            "description": cj_product.get("description", ""),
            "short_description": cj_product.get("productSku", "")[:200],
            "main_image_url": main_image[:500],  # Limit to 500 chars
            "images": images_list if images_list else [],
            "thumbnail_url": main_image[:500],  # Limit to 500 chars
            "original_price": original_price,
            "selling_price": selling_price,
            "currency": "USD",
            "cj_product_id": cj_product.get("pid", ""),
            "cj_variant_id": cj_product.get("vid"),
            "cj_product_data": cj_product,
            "tags": cj_product.get("categoryName", "").split() if cj_product.get("categoryName") else []
        }
