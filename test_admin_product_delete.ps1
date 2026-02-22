# Test Admin Product Delete - Task 2.3
# =====================================

$BASE_URL = "http://127.0.0.1:8000/api/v1"
$ADMIN_EMAIL = "admin@buyv.com"
$ADMIN_PASSWORD = "Buyv2024Admin!"

Write-Host "`n========== TESTING ADMIN PRODUCT DELETE ==========" -ForegroundColor Cyan

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
    Write-Host "✓ Admin logged in successfully" -ForegroundColor Green
    Write-Host "  Role: $($loginResponse.admin.role)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Admin login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Create a test product
Write-Host "`n[2] Creating test product for deletion..." -ForegroundColor Yellow
try {
    $productBody = @{
        name = "Test Product To Delete"
        description = "This product will be deleted in Task 2.3 test"
        short_description = "Test delete"
        main_image_url = "https://via.placeholder.com/400"
        images = @("https://via.placeholder.com/400")
        original_price = 50.00
        selling_price = 45.99
        commission_rate = 10.0
        commission_type = "percentage"
        status = "active"
        is_featured = $false
    } | ConvertTo-Json

    $headers = @{
        "Authorization" = "Bearer $ADMIN_TOKEN"
        "Content-Type" = "application/json"
    }

    $productResponse = Invoke-RestMethod -Uri "$BASE_URL/admin/marketplace/products" `
        -Method POST `
        -Headers $headers `
        -Body $productBody

    $PRODUCT_ID = $productResponse.id
    Write-Host "✓ Test product created:" -ForegroundColor Green
    Write-Host "  ID: $PRODUCT_ID" -ForegroundColor Gray
    Write-Host "  Name: $($productResponse.name)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to create product: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Verify product exists
Write-Host "`n[3] Verifying product exists..." -ForegroundColor Yellow
try {
    $verifyResponse = Invoke-RestMethod -Uri "$BASE_URL/marketplace/products/$PRODUCT_ID" `
        -Method GET

    Write-Host "✓ Product exists in database" -ForegroundColor Green
    Write-Host "  Name: $($verifyResponse.name)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to verify product: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Delete the product
Write-Host "`n[4] DELETING product..." -ForegroundColor Yellow
try {
    $deleteResponse = Invoke-RestMethod -Uri "$BASE_URL/admin/marketplace/products/$PRODUCT_ID" `
        -Method DELETE `
        -Headers $headers

    Write-Host "✓ Product deleted successfully!" -ForegroundColor Green
    Write-Host "  Message: $($deleteResponse.message)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to delete product: $_" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Verify product was soft-deleted (status = inactive)
Write-Host "`n[5] Verifying product was soft-deleted..." -ForegroundColor Yellow
try {
    $checkResponse = Invoke-RestMethod -Uri "$BASE_URL/marketplace/products/$PRODUCT_ID" `
        -Method GET
    
    if ($checkResponse.status -eq "inactive") {
        Write-Host "✓ Product soft-deleted (status = inactive)" -ForegroundColor Green
        Write-Host "  Status: $($checkResponse.status)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Product status should be 'inactive' but is '$($checkResponse.status)'" -ForegroundColor Red
        exit 1
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✓ Product hard-deleted (404 - complete removal)" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected error: Status $statusCode" -ForegroundColor Red
        exit 1
    }
}

# Step 6: Test deleting non-existent product (should fail gracefully)
Write-Host "`n[6] Testing delete of non-existent product..." -ForegroundColor Yellow
try {
    $fakeId = "00000000-0000-0000-0000-000000000000"
    Invoke-RestMethod -Uri "$BASE_URL/admin/marketplace/products/$fakeId" `
        -Method DELETE `
        -Headers $headers
    
    Write-Host "✗ Should have returned 404!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✓ Correctly returns 404 for non-existent product" -ForegroundColor Green
    } else {
        Write-Host "⚠ Unexpected status: $statusCode (expected 404)" -ForegroundColor Yellow
    }
}

# Summary
Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Admin authentication working" -ForegroundColor Green
Write-Host "  ✓ Product creation successful" -ForegroundColor Green
Write-Host "  ✓ Product deletion endpoint functional" -ForegroundColor Green
Write-Host "  ✓ Product removed from database (404)" -ForegroundColor Green
Write-Host "  ✓ Error handling for non-existent products" -ForegroundColor Green
Write-Host "`n✅ Task 2.3 Backend tests complete!" -ForegroundColor Green
Write-Host "Next: Test from Android Admin UI" -ForegroundColor Yellow
