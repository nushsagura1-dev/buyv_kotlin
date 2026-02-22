"""
Script pour ajouter des données initiales au marketplace.
"""
import sys
import os

# Ajouter le chemin du backend
backend_path = os.path.join(os.path.dirname(__file__), '..', 'buyv_backend')
sys.path.append(backend_path)

from app.database import SessionLocal
from app.marketplace.models import ProductCategory
from decimal import Decimal


def seed_categories():
    """Créer les catégories initiales."""
    db = SessionLocal()
    
    categories = [
        # Catégories principales
        {
            'name': 'Electronics',
            'name_ar': 'إلكترونيات',
            'slug': 'electronics',
            'display_order': 1
        },
        {
            'name': 'Fashion',
            'name_ar': 'أزياء',
            'slug': 'fashion',
            'display_order': 2
        },
        {
            'name': 'Home & Garden',
            'name_ar': 'المنزل والحديقة',
            'slug': 'home-garden',
            'display_order': 3
        },
        {
            'name': 'Beauty & Health',
            'name_ar': 'الجمال والصحة',
            'slug': 'beauty-health',
            'display_order': 4
        },
        {
            'name': 'Sports & Outdoor',
            'name_ar': 'الرياضة والهواء الطلق',
            'slug': 'sports-outdoor',
            'display_order': 5
        },
        {
            'name': 'Toys & Kids',
            'name_ar': 'ألعاب وأطفال',
            'slug': 'toys-kids',
            'display_order': 6
        },
        {
            'name': 'Automotive',
            'name_ar': 'السيارات',
            'slug': 'automotive',
            'display_order': 7
        },
        {
            'name': 'Books & Media',
            'name_ar': 'كتب ووسائط',
            'slug': 'books-media',
            'display_order': 8
        },
    ]
    
    created_count = 0
    skipped_count = 0
    
    print("🌱 Seed des catégories initiales...")
    print("=" * 60)
    
    for cat_data in categories:
        # Vérifier si existe déjà
        existing = db.query(ProductCategory).filter(
            ProductCategory.slug == cat_data['slug']
        ).first()
        
        if existing:
            print(f"⏭️  Catégorie '{cat_data['name']}' existe déjà")
            skipped_count += 1
            continue
        
        # Créer la catégorie
        category = ProductCategory(**cat_data)
        db.add(category)
        created_count += 1
        print(f"✅ Catégorie '{cat_data['name']}' créée")
    
    # Sous-catégories Electronics
    electronics = db.query(ProductCategory).filter(
        ProductCategory.slug == 'electronics'
    ).first()
    
    if electronics:
        electronics_subs = [
            {'name': 'Smartphones', 'name_ar': 'هواتف ذكية', 'slug': 'smartphones', 'parent_id': electronics.id, 'display_order': 1},
            {'name': 'Laptops', 'name_ar': 'أجهزة كمبيوتر محمولة', 'slug': 'laptops', 'parent_id': electronics.id, 'display_order': 2},
            {'name': 'Tablets', 'name_ar': 'أجهزة لوحية', 'slug': 'tablets', 'parent_id': electronics.id, 'display_order': 3},
            {'name': 'Headphones', 'name_ar': 'سماعات', 'slug': 'headphones', 'parent_id': electronics.id, 'display_order': 4},
            {'name': 'Cameras', 'name_ar': 'كاميرات', 'slug': 'cameras', 'parent_id': electronics.id, 'display_order': 5},
        ]
        
        for sub_data in electronics_subs:
            existing = db.query(ProductCategory).filter(
                ProductCategory.slug == sub_data['slug']
            ).first()
            
            if not existing:
                sub_category = ProductCategory(**sub_data)
                db.add(sub_category)
                created_count += 1
                print(f"  ✅ Sous-catégorie '{sub_data['name']}' créée")
            else:
                skipped_count += 1
    
    try:
        db.commit()
        print("=" * 60)
        print(f"✨ Seed terminé!")
        print(f"   {created_count} catégories créées")
        print(f"   {skipped_count} catégories existantes ignorées")
        return True
    except Exception as e:
        db.rollback()
        print(f"❌ Erreur lors du seed: {e}")
        return False
    finally:
        db.close()


def create_admin_user():
    """Créer un utilisateur admin si n'existe pas."""
    from app.models import User
    from werkzeug.security import generate_password_hash
    
    db = SessionLocal()
    
    # Vérifier si admin existe
    admin = db.query(User).filter(User.email == 'admin@buyv.com').first()
    
    if admin:
        print("✓ Utilisateur admin existe déjà")
        db.close()
        return
    
    # Créer admin
    admin = User(
        email='admin@buyv.com',
        username='admin',
        display_name='Admin',
        password_hash=generate_password_hash('admin123'),
        is_verified=True
    )
    
    db.add(admin)
    db.commit()
    db.close()
    
    print("✅ Utilisateur admin créé")
    print("   Email: admin@buyv.com")
    print("   Password: admin123")


def main():
    """Exécuter tous les seeds."""
    print("\n" + "=" * 60)
    print("🌱 SEED DATA - MARKETPLACE")
    print("=" * 60 + "\n")
    
    try:
        # 1. Catégories
        if seed_categories():
            print("\n✅ Seed des catégories réussi!")
        else:
            print("\n❌ Seed des catégories échoué")
            return 1
        
        # 2. Admin user
        print("\n👤 Vérification utilisateur admin...")
        try:
            create_admin_user()
        except Exception as e:
            print(f"⚠️ Note: {e}")
        
        print("\n" + "=" * 60)
        print("🎉 SEED TERMINÉ AVEC SUCCÈS!")
        print("=" * 60)
        print("\n🚀 Prochaines étapes:")
        print("   1. Démarrer l'admin: python admin_app.py")
        print("   2. Se connecter: http://localhost:5000")
        print("   3. Importer des produits depuis CJ Dropshipping")
        print("=" * 60 + "\n")
        
        return 0
        
    except Exception as e:
        print(f"\n❌ Erreur lors du seed: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    exit(main())
