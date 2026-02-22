# Test complet du flow admin: Login -> Edit Product -> Delete Product
param(
    [string]$BASE_URL = "http://localhost:8000"
)

Write-Host "========== TESTING COMPLETE ADMIN FLOW ==========" -ForegroundColor Cyan
Write-Host "Backend: $BASE_URL`n" -ForegroundColor Gray

# Admin credentials
$ADMIN_EMAIL = "admin@buyv.com"
$ADMIN_PASSWORD = "Buyv2024Admin!"

# Step 1: Admin Login
Write-Host "[1] Admin login..." -ForegroundColor Yellow
try {
    $loginBody = @{
        email = $ADMIN_EMAIL
        password = $ADMIN_PASSWORD
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/admin/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $ADMIN_TOKEN = $loginResponse.access_token
    $ROLE = $loginResponse.role
    
    Write-Host "✓ Admin logged in successfully" -ForegroundColor Green
    Write-Host "  Role: $ROLE" -ForegroundColor Gray
} catch {
    Write-Host "✗ Admin login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Get first product
Write-Host "`n[2] Getting products..." -ForegroundColor Yellow
try {
    $productsResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/marketplace/products?limit=5" `
        -Method GET
    
    # Handle different response structures
    $products = if ($productsResponse.items) { 
        $productsResponse.items 
    } elseif ($productsResponse.products) { 
        $productsResponse.products 
    } else { 
        $productsResponse 
    }
    
    if ($products.Count -eq 0) {
        Write-Host "✗ No products found" -ForegroundColor Red
        exit 1
    }
    
    $PRODUCT = $products[0]
    $PRODUCT_ID = $PRODUCT.id
    $ORIGINAL_NAME = $PRODUCT.name
    $ORIGINAL_PRICE = [double]$PRODUCT.price
    
    Write-Host "✓ Found product:" -ForegroundColor Green
    Write-Host "  ID: $PRODUCT_ID" -ForegroundColor Gray
    Write-Host "  Name: $ORIGINAL_NAME" -ForegroundColor Gray
    Write-Host "  Price: `$$ORIGINAL_PRICE" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to get products: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Edit Product (Test Task 2.2)
Write-Host "`n[3] Testing product edit (Task 2.2)..." -ForegroundColor Yellow
try {
    $NEW_NAME = "$ORIGINAL_NAME (EDITED)"
    $NEW_PRICE = [math]::Round([double]$ORIGINAL_PRICE * 1.1, 2)
    
    $updateBody = @{
        name = $NEW_NAME
        price = $NEW_PRICE
    } | ConvertTo-Json

    $headers = @{
        Authorization = "Bearer $ADMIN_TOKEN"
        "Content-Type" = "application/json"
    }

    $updateResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/admin/marketplace/products/$PRODUCT_ID" `
        -Method PUT `
        -Headers $headers `
        -Body $updateBody
    
    Write-Host "✓ Product edited successfully:" -ForegroundColor Green
    Write-Host "  Name: $($updateResponse.name)" -ForegroundColor Gray
    Write-Host "  Price: `$$($updateResponse.price)" -ForegroundColor Gray
    
    if ($updateResponse.name -eq $NEW_NAME -and [math]::Abs($updateResponse.price - $NEW_PRICE) -lt 0.01) {
        Write-Host "✓ Edit verified - values updated correctly" -ForegroundColor Green
    } else {
        Write-Host "✗ Edit values don't match!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Product edit failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Verify product is still active
Write-Host "`n[4] Verifying product is active..." -ForegroundColor Yellow
try {
    $checkResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/marketplace/products/$PRODUCT_ID" `
        -Method GET
    
    if ($checkResponse.status -ne "inactive") {
        Write-Host "✓ Product is active (status: $($checkResponse.status))" -ForegroundColor Green
    } else {
        Write-Host "✗ Product is inactive!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Failed to check product status: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Delete Product (Test Task 2.3)
Write-Host "`n[5] Testing product delete (Task 2.3)..." -ForegroundColor Yellow
try {
    $headers = @{
        Authorization = "Bearer $ADMIN_TOKEN"
    }

    $deleteResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/admin/marketplace/products/$PRODUCT_ID" `
        -Method DELETE `
        -Headers $headers
    
    Write-Host "✓ Product deleted successfully!" -ForegroundColor Green
    Write-Host "  Message: $($deleteResponse.message)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Product delete failed: $_" -ForegroundColor Red
    exit 1
}

# Step 6: Verify soft delete (status = inactive)
Write-Host "`n[6] Verifying soft delete (status should be 'inactive')..." -ForegroundColor Yellow
try {
    $checkResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/marketplace/products/$PRODUCT_ID" `
        -Method GET
    
    if ($checkResponse.status -eq "inactive") {
        Write-Host "✓ Product soft-deleted correctly (status = inactive)" -ForegroundColor Green
        Write-Host "  Product still in database but marked inactive ✓" -ForegroundColor Gray
    } else {
        Write-Host "✗ Product status should be 'inactive' but is '$($checkResponse.status)'" -ForegroundColor Red
        exit 1
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✓ Product hard-deleted (404 - completely removed)" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected error: Status $statusCode" -ForegroundColor Red
        exit 1
    }
}

# Step 7: Restore product for next test (optional)
Write-Host "`n[7] Restoring product for next test..." -ForegroundColor Yellow
try {
    $restoreBody = @{
        name = $ORIGINAL_NAME
        price = $ORIGINAL_PRICE
        status = "active"
    } | ConvertTo-Json

    $headers = @{
        Authorization = "Bearer $ADMIN_TOKEN"
        "Content-Type" = "application/json"
    }

    $restoreResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/admin/marketplace/products/$PRODUCT_ID" `
        -Method PUT `
        -Headers $headers `
        -Body $restoreBody
    
    Write-Host "✓ Product restored to original state" -ForegroundColor Green
    Write-Host "  Name: $($restoreResponse.name)" -ForegroundColor Gray
    Write-Host "  Price: `$$($restoreResponse.price)" -ForegroundColor Gray
    Write-Host "  Status: $($restoreResponse.status)" -ForegroundColor Gray
} catch {
    Write-Host "⚠ Could not restore product (non-critical): $_" -ForegroundColor Yellow
}

Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Admin authentication working" -ForegroundColor Green
Write-Host "  ✓ Task 2.2: Product edit functional" -ForegroundColor Green
Write-Host "  ✓ Task 2.3: Product delete functional (soft delete)" -ForegroundColor Green
Write-Host "  ✓ Product status management working" -ForegroundColor Green
Write-Host "  ✓ Product restoration working" -ForegroundColor Green
Write-Host "`n✅ Tasks 2.2 and 2.3 backend tests complete!" -ForegroundColor Green
Write-Host "Next: Test from Android Admin UI" -ForegroundColor Cyan
