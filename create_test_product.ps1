# Create Test Product for Admin Edit Testing
# ===========================================

$BASE_URL = "http://127.0.0.1:8000/api/v1"
$ADMIN_EMAIL = "admin@buyv.com"
$ADMIN_PASSWORD = "Buyv2024Admin!"

Write-Host "`n========== CREATING TEST PRODUCT ==========" -ForegroundColor Cyan

# Step 1: Admin Login
Write-Host "`n[1] Admin login..." -ForegroundColor Yellow
try {
    $loginBody = @{
        email = $ADMIN_EMAIL
        password = $ADMIN_PASSWORD
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8000/auth/admin/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"

    $ADMIN_TOKEN = $loginResponse.access_token
    Write-Host "✓ Admin logged in" -ForegroundColor Green
} catch {
    Write-Host "✗ Admin login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Create a test product
Write-Host "`n[2] Creating test product..." -ForegroundColor Yellow
try {
    $productBody = @{
        name = "Test Product for Edit"
        description = "This is a test product created for Task 2.2 testing"
        short_description = "Test product"
        main_image_url = "https://via.placeholder.com/400"
        images = @("https://via.placeholder.com/400")
        original_price = 100.00
        selling_price = 89.99
        commission_rate = 10.0
        commission_type = "percentage"
        status = "active"
        is_featured = $false
        is_choice = $false
    } | ConvertTo-Json

    $headers = @{
        "Authorization" = "Bearer $ADMIN_TOKEN"
        "Content-Type" = "application/json"
    }

    $productResponse = Invoke-RestMethod -Uri "$BASE_URL/admin/marketplace/products" `
        -Method POST `
        -Headers $headers `
        -Body $productBody

    Write-Host "✓ Product created successfully!" -ForegroundColor Green
    Write-Host "  Product ID: $($productResponse.id)" -ForegroundColor Gray
    Write-Host "  Name: $($productResponse.name)" -ForegroundColor Gray
    Write-Host "  Price: $($productResponse.selling_price)" -ForegroundColor Gray
    Write-Host "  Commission: $($productResponse.commission_rate)%" -ForegroundColor Gray
    Write-Host "`n✅ Test product ready for testing Task 2.2!" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create product: $_" -ForegroundColor Red
    Write-Host "  Error details: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
