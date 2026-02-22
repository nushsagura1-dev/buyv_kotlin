#!/bin/bash

echo "🚀 Initialisation du Marketplace"
echo "================================"

# Vérifier que nous sommes dans le bon répertoire
if [ ! -d "buyv_backend" ]; then
    echo "❌ Erreur: Exécutez ce script depuis la racine du projet"
    exit 1
fi

cd buyv_backend

echo "✅ Installation des dépendances..."
pip install httpx python-dotenv alembic

echo ""
echo "✅ Vérification de la configuration CJ Dropshipping..."
if grep -q "CJ_API_KEY" .env 2>/dev/null; then
    echo "   ✓ CJ_API_KEY trouvée dans .env"
else
    echo "   ⚠️  CJ_API_KEY manquante dans .env"
    echo "   Ajoutez: CJ_API_KEY=votre_clé"
fi

echo ""
echo "✅ Exécution de la migration de base de données..."
cd ..
python -m alembic upgrade head

if [ $? -eq 0 ]; then
    echo ""
    echo "✨ Migration réussie !"
    echo ""
    echo "📦 Tables créées:"
    echo "   • product_categories"
    echo "   • marketplace_products"
    echo "   • product_promotions"
    echo "   • affiliate_sales"
    echo "   • promoter_wallets"
    echo "   • wallet_transactions"
    echo "   • withdrawal_requests"
    echo ""
    echo "🎯 Prochaines étapes:"
    echo "   1. Redémarrer le serveur backend: python -m uvicorn app.main:app --reload"
    echo "   2. Tester les endpoints: http://localhost:8000/docs"
    echo "   3. Créer des catégories via l'admin"
    echo "   4. Importer des produits depuis CJ"
else
    echo ""
    echo "❌ Erreur lors de la migration"
    echo "Vérifiez la configuration PostgreSQL et réessayez"
    exit 1
fi
