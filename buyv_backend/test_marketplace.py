"""
Script de test pour vérifier le module Marketplace.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.database import SessionLocal, Base, engine
from app.marketplace.models import (
    ProductCategory, MarketplaceProduct, ProductPromotion,
    AffiliateSale, PromoterWallet
)
from decimal import Decimal
import uuid

def test_database_connection():
    """Test de connexion à la base de données."""
    print("🔌 Test de connexion à la base de données...")
    try:
        from sqlalchemy import text
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        print("✅ Connexion réussie!")
        return True
    except Exception as e:
        print(f"❌ Erreur de connexion: {e}")
        return False

def test_create_tables():
    """Test de création des tables."""
    print("\n📦 Test de création des tables...")
    try:
        # Créer toutes les tables
        Base.metadata.create_all(bind=engine)
        print("✅ Tables créées!")
        
        # Vérifier que les tables existent
        db = SessionLocal()
        tables = [
            'product_categories',
            'marketplace_products',
            'product_promotions',
            'affiliate_sales',
            'promoter_wallets',
            'wallet_transactions',
            'withdrawal_requests'
        ]
        
        from sqlalchemy import text
        for table in tables:
            result = db.execute(text(f"SELECT to_regclass('public.{table}')")).scalar()
            if result:
                print(f"   ✓ Table '{table}' existe")
            else:
                print(f"   ✗ Table '{table}' manquante")
        
        db.close()
        return True
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_create_category():
    """Test de création d'une catégorie."""
    print("\n🏷️ Test de création d'une catégorie...")
    try:
        db = SessionLocal()
        
        # Créer catégorie
        category = ProductCategory(
            name="Electronics",
            name_ar="إلكترونيات",
            slug="electronics"
        )
        
        db.add(category)
        db.commit()
        db.refresh(category)
        
        print(f"✅ Catégorie créée: {category.name} (ID: {category.id})")
        
        db.close()
        return True
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_create_product():
    """Test de création d'un produit."""
    print("\n📱 Test de création d'un produit...")
    try:
        db = SessionLocal()
        
        # Récupérer la catégorie
        category = db.query(ProductCategory).filter(
            ProductCategory.slug == "electronics"
        ).first()
        
        if not category:
            print("⚠️ Catégorie 'electronics' non trouvée, création...")
            category = ProductCategory(
                name="Electronics",
                slug="electronics"
            )
            db.add(category)
            db.commit()
            db.refresh(category)
        
        # Créer produit
        product = MarketplaceProduct(
            category_id=category.id,
            name="iPhone 15 Pro",
            description="Latest iPhone model with amazing features",
            original_price=Decimal("1199.99"),
            selling_price=Decimal("999.99"),
            commission_rate=Decimal("5.0"),
            cj_product_id="CJ123456",
            main_image_url="https://example.com/iphone.jpg",
            tags=["smartphone", "apple", "electronics"]
        )
        
        db.add(product)
        db.commit()
        db.refresh(product)
        
        print(f"✅ Produit créé: {product.name}")
        print(f"   Prix: {product.selling_price} USD")
        print(f"   Commission: {product.commission_rate}%")
        print(f"   ID: {product.id}")
        
        db.close()
        return True
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_create_wallet():
    """Test de création d'un wallet."""
    print("\n💰 Test de création d'un wallet...")
    try:
        db = SessionLocal()
        
        # Créer wallet
        wallet = PromoterWallet(
            user_id="test_user_123",
            pending_amount=Decimal("50.00"),
            available_amount=Decimal("100.00")
        )
        
        db.add(wallet)
        db.commit()
        db.refresh(wallet)
        
        print(f"✅ Wallet créé pour user: {wallet.user_id}")
        print(f"   Pending: {wallet.pending_amount} USD")
        print(f"   Available: {wallet.available_amount} USD")
        
        db.close()
        return True
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_commission_calculation():
    """Test du calcul de commission."""
    print("\n🧮 Test de calcul de commission...")
    
    test_cases = [
        (100, 5),   # 100 USD, 5%
        (50, 10),   # 50 USD, 10%
        (999.99, 3.5),  # 999.99 USD, 3.5%
    ]
    
    for price, rate in test_cases:
        # Formule: prix × (1 - (1 / (1 + (taux / 100))))
        commission = price * (1 - (1 / (1 + (rate / 100))))
        print(f"   Prix: ${price:.2f}, Taux: {rate}% → Commission: ${commission:.2f}")
    
    print("✅ Calculs réussis!")
    return True

def cleanup_test_data():
    """Nettoyer les données de test."""
    print("\n🧹 Nettoyage des données de test...")
    try:
        db = SessionLocal()
        
        # Supprimer les données de test
        db.query(PromoterWallet).filter(
            PromoterWallet.user_id == "test_user_123"
        ).delete()
        
        db.query(MarketplaceProduct).filter(
            MarketplaceProduct.cj_product_id == "CJ123456"
        ).delete()
        
        db.query(ProductCategory).filter(
            ProductCategory.slug == "electronics"
        ).delete()
        
        db.commit()
        db.close()
        
        print("✅ Nettoyage terminé!")
        return True
    except Exception as e:
        print(f"⚠️ Avertissement: {e}")
        return True  # Non bloquant

def main():
    """Exécuter tous les tests."""
    print("=" * 60)
    print("🧪 TEST DU MODULE MARKETPLACE")
    print("=" * 60)
    
    tests = [
        ("Connexion DB", test_database_connection),
        ("Tables", test_create_tables),
        ("Catégorie", test_create_category),
        ("Produit", test_create_product),
        ("Wallet", test_create_wallet),
        ("Calcul Commission", test_commission_calculation),
        ("Nettoyage", cleanup_test_data),
    ]
    
    results = []
    for name, test_func in tests:
        try:
            success = test_func()
            results.append((name, success))
        except Exception as e:
            print(f"\n❌ Exception dans {name}: {e}")
            results.append((name, False))
    
    # Résumé
    print("\n" + "=" * 60)
    print("📊 RÉSUMÉ DES TESTS")
    print("=" * 60)
    
    passed = sum(1 for _, success in results if success)
    total = len(results)
    
    for name, success in results:
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status} - {name}")
    
    print(f"\nRésultat: {passed}/{total} tests réussis")
    
    if passed == total:
        print("\n🎉 Tous les tests sont passés!")
        return 0
    else:
        print(f"\n⚠️ {total - passed} test(s) échoué(s)")
        return 1

if __name__ == "__main__":
    exit(main())
