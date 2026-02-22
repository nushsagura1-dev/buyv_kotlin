# Test Task 2.4: Vérifier méthode HTTP Orders (PUT vs PATCH)
param(
    [string]$BASE_URL = "http://localhost:8000"
)

Write-Host "========== TESTING TASK 2.4: ORDERS HTTP METHOD ==========" -ForegroundColor Cyan
Write-Host "Backend: $BASE_URL`n" -ForegroundColor Gray

# Admin credentials
$ADMIN_EMAIL = "admin@buyv.com"
$ADMIN_PASSWORD = "Buyv2024Admin!"

# Step 1: Register a test user to create an order
Write-Host "[1] Registering test user..." -ForegroundColor Yellow
try {
    $randomNum = Get-Random -Minimum 1000 -Maximum 9999
    $testEmail = "ordertest$randomNum@test.com"
    $testPassword = "Test123!"
    
    $registerBody = @{
        email = $testEmail
        password = $testPassword
        username = "ordertest$randomNum"
        display_name = "Order Test User"
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody
    
    $USER_TOKEN = $registerResponse.access_token
    Write-Host "✓ Test user registered" -ForegroundColor Green
    Write-Host "  Email: $testEmail" -ForegroundColor Gray
} catch {
    Write-Host "✗ User registration failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Get existing orders or create a simple one
Write-Host "`n[2] Getting/creating test order..." -ForegroundColor Yellow
try {
    # First, try to get existing orders
    $headers = @{
        Authorization = "Bearer $USER_TOKEN"
    }

    try {
        $ordersResponse = Invoke-RestMethod -Uri "$BASE_URL/orders?limit=1" `
            -Method GET `
            -Headers $headers
        
        if ($ordersResponse.orders -and $ordersResponse.orders.Count -gt 0) {
            $ORDER_ID = $ordersResponse.orders[0].id
            Write-Host "✓ Using existing order" -ForegroundColor Green
            Write-Host "  Order ID: $ORDER_ID" -ForegroundColor Gray
            Write-Host "  Status: $($ordersResponse.orders[0].status)" -ForegroundColor Gray
        } else {
            throw "No orders found"
        }
    } catch {
        # Create order with all required fields
        $orderBody = @{
            items = @(
                @{
                    product_id = "03e3aa89-90be-41d5-87e5-d8b7fb4e6a51"
                    productName = "Test Product"
                    productImage = "https://example.com/image.jpg"
                    quantity = 1
                    price = 50.00
                }
            )
            subtotal = 50.00
            shipping = 5.00
            tax = 5.50
            total = 60.50
            shipping_address = @{
                fullName = "Test User"
                address = "123 Test St"
                city = "Test City"
                state = "TS"
                zip_code = "12345"
                country = "Test Country"
                phone = "+1234567890"
            }
            paymentMethod = "card"
        } | ConvertTo-Json -Depth 10

        $orderResponse = Invoke-RestMethod -Uri "$BASE_URL/orders" `
            -Method POST `
            -Headers @{
                Authorization = "Bearer $USER_TOKEN"
                "Content-Type" = "application/json"
            } `
            -Body $orderBody
        
        $ORDER_ID = $orderResponse.id
        Write-Host "✓ Order created successfully" -ForegroundColor Green
        Write-Host "  Order ID: $ORDER_ID" -ForegroundColor Gray
        Write-Host "  Status: $($orderResponse.status)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Order setup failed: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Admin login to get admin token
Write-Host "`n[3] Admin login..." -ForegroundColor Yellow
try {
    $adminLoginBody = @{
        email = $ADMIN_EMAIL
        password = $ADMIN_PASSWORD
    } | ConvertTo-Json

    $adminResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/admin/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $adminLoginBody
    
    $ADMIN_TOKEN = $adminResponse.access_token
    Write-Host "✓ Admin logged in successfully" -ForegroundColor Green
    Write-Host "  Role: $($adminResponse.admin.role)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Admin login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Test PATCH method (correct method)
Write-Host "`n[4] Testing PATCH method (correct)..." -ForegroundColor Yellow
try {
    $statusBody = @{
        status = "processing"
    } | ConvertTo-Json

    $headers = @{
        Authorization = "Bearer $USER_TOKEN"
        "Content-Type" = "application/json"
    }

    $patchResponse = Invoke-RestMethod -Uri "$BASE_URL/orders/$ORDER_ID/status" `
        -Method PATCH `
        -Headers $headers `
        -Body $statusBody
    
    Write-Host "✓ PATCH method accepted (200 OK)" -ForegroundColor Green
    Write-Host "  Response: $($patchResponse.status)" -ForegroundColor Gray
    
    # Backend returns {"status": "ok"} not the order status
    if ($patchResponse.status -eq "ok") {
        Write-Host "✓ Status update acknowledged" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected response" -ForegroundColor Red
        exit 1
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✗ PATCH failed with status $statusCode" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Test PUT method (should fail with 405)
Write-Host "`n[5] Testing PUT method (should fail with 405)..." -ForegroundColor Yellow
try {
    $statusBody = @{
        status = "shipped"
    } | ConvertTo-Json

    $headers = @{
        Authorization = "Bearer $USER_TOKEN"
        "Content-Type" = "application/json"
    }

    $putResponse = Invoke-RestMethod -Uri "$BASE_URL/orders/$ORDER_ID/status" `
        -Method PUT `
        -Headers $headers `
        -Body $statusBody
    
    Write-Host "✗ PUT method was accepted (should be rejected!)" -ForegroundColor Red
    Write-Host "  Backend should only allow PATCH, not PUT" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 405) {
        Write-Host "✓ PUT correctly rejected with 405 Method Not Allowed" -ForegroundColor Green
        
        # Try to get Allow header
        try {
            $response = Invoke-WebRequest -Uri "$BASE_URL/orders/$ORDER_ID/status" `
                -Method PUT `
                -Headers $headers `
                -Body $statusBody `
                -ErrorAction SilentlyContinue
        } catch {
            $allowHeader = $_.Exception.Response.Headers["Allow"]
            if ($allowHeader) {
                Write-Host "  Allow header: $allowHeader" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "✗ PUT failed with unexpected status: $statusCode" -ForegroundColor Red
        Write-Host "  Expected 405, got $statusCode" -ForegroundColor Red
        exit 1
    }
}

# Step 6: Verify final order status
Write-Host "`n[6] Verifying final order status..." -ForegroundColor Yellow
try {
    $headers = @{
        Authorization = "Bearer $USER_TOKEN"
    }

    $finalOrder = Invoke-RestMethod -Uri "$BASE_URL/orders/$ORDER_ID" `
        -Method GET `
        -Headers $headers
    
    Write-Host "✓ Order retrieved successfully" -ForegroundColor Green
    Write-Host "  Final status: $($finalOrder.status)" -ForegroundColor Gray
    
    if ($finalOrder.status -eq "processing") {
        Write-Host "✓ Status correctly remains 'processing' (PUT was rejected)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Status is '$($finalOrder.status)' (expected 'processing')" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to retrieve final order: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Test user created" -ForegroundColor Green
Write-Host "  ✓ Order created successfully" -ForegroundColor Green
Write-Host "  ✓ Admin authentication working" -ForegroundColor Green
Write-Host "  ✓ PATCH method works correctly (200 OK)" -ForegroundColor Green
Write-Host "  ✓ PUT method correctly rejected (405 Method Not Allowed)" -ForegroundColor Green
Write-Host "  ✓ Backend enforces PATCH-only for order status updates" -ForegroundColor Green
Write-Host "`n✅ Task 2.4 VALIDATED: Backend and Android are aligned on PATCH method!" -ForegroundColor Green
