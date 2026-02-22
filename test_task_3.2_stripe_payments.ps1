# ========================================
# TEST TASK 3.2: STRIPE PAYMENTS
# ========================================
# Tests l'intégration Stripe Payment Intent backend
# L'app Android utilisera Stripe Payment Sheet pour le paiement

$BASE_URL = "http://localhost:8000"
$ErrorActionPreference = "Continue"

Write-Host "`n========== TESTING TASK 3.2: STRIPE PAYMENTS ==========" -ForegroundColor Cyan
Write-Host "Backend: $BASE_URL" -ForegroundColor Gray
Write-Host ""

# Step 1: Register test user
Write-Host "[1] Registering test user..." -ForegroundColor Yellow
try {
    $timestamp = [int](Get-Date -UFormat %s)
    $testEmail = "stripetest$timestamp@test.com"
    
    $registerBody = @{
        email = $testEmail
        username = "stripetest$timestamp"
        password = "TestPassword123!"
        displayName = "Stripe Test User"
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/register" `
        -Method POST `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $registerBody
    
    $USER_TOKEN = $registerResponse.idToken
    Write-Host "✓ Test user registered" -ForegroundColor Green
    Write-Host "  Email: $testEmail" -ForegroundColor Gray
} catch {
    Write-Host "✗ User registration failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Test Payment Intent Creation (Small Amount)
Write-Host "`n[2] Creating payment intent (Small: $10.00)..." -ForegroundColor Yellow
try {
    $paymentBody = @{
        amount = 1000  # $10.00 in cents
        currency = "usd"
    } | ConvertTo-Json

    $headers = @{
        Authorization = "Bearer $USER_TOKEN"
        "Content-Type" = "application/json"
    }

    $paymentResponse = Invoke-RestMethod -Uri "$BASE_URL/payments/create-payment-intent" `
        -Method POST `
        -Headers $headers `
        -Body $paymentBody
    
    Write-Host "✓ Payment intent created successfully" -ForegroundColor Green
    Write-Host "  Amount: $10.00" -ForegroundColor Gray
    Write-Host "  Currency: usd" -ForegroundColor Gray
    Write-Host "  Client Secret: $($paymentResponse.clientSecret.Substring(0, 30))..." -ForegroundColor Gray
    Write-Host "  Customer ID: $($paymentResponse.customer)" -ForegroundColor Gray
    Write-Host "  Ephemeral Key: $($paymentResponse.ephemeralKey.Substring(0, 30))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Payment intent creation failed: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Test Payment Intent Creation (Medium Amount)
Write-Host "`n[3] Creating payment intent (Medium: $50.00)..." -ForegroundColor Yellow
try {
    $paymentBody = @{
        amount = 5000  # $50.00 in cents
        currency = "usd"
    } | ConvertTo-Json

    $paymentResponse = Invoke-RestMethod -Uri "$BASE_URL/payments/create-payment-intent" `
        -Method POST `
        -Headers @{
            Authorization = "Bearer $USER_TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $paymentBody
    
    Write-Host "✓ Payment intent created successfully" -ForegroundColor Green
    Write-Host "  Amount: $50.00" -ForegroundColor Gray
    Write-Host "  Currency: usd" -ForegroundColor Gray
    Write-Host "  Customer ID: $($paymentResponse.customer)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Payment intent creation failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Test Payment Intent Creation (Large Amount)
Write-Host "`n[4] Creating payment intent (Large: $199.99)..." -ForegroundColor Yellow
try {
    $paymentBody = @{
        amount = 19999  # $199.99 in cents
        currency = "usd"
    } | ConvertTo-Json

    $paymentResponse = Invoke-RestMethod -Uri "$BASE_URL/payments/create-payment-intent" `
        -Method POST `
        -Headers @{
            Authorization = "Bearer $USER_TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $paymentBody
    
    Write-Host "✓ Payment intent created successfully" -ForegroundColor Green
    Write-Host "  Amount: $199.99" -ForegroundColor Gray
    Write-Host "  Currency: usd" -ForegroundColor Gray
} catch {
    Write-Host "✗ Payment intent creation failed: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Test with different currency (EUR)
Write-Host "`n[5] Creating payment intent (EUR: €25.00)..." -ForegroundColor Yellow
try {
    $paymentBody = @{
        amount = 2500  # €25.00 in cents
        currency = "eur"
    } | ConvertTo-Json

    $paymentResponse = Invoke-RestMethod -Uri "$BASE_URL/payments/create-payment-intent" `
        -Method POST `
        -Headers @{
            Authorization = "Bearer $USER_TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $paymentBody
    
    Write-Host "✓ Payment intent created successfully" -ForegroundColor Green
    Write-Host "  Amount: €25.00" -ForegroundColor Gray
    Write-Host "  Currency: eur" -ForegroundColor Gray
} catch {
    Write-Host "✗ Payment intent creation failed: $_" -ForegroundColor Red
    exit 1
}

# Step 6: Test without authentication (should fail)
Write-Host "`n[6] Testing without authentication (should fail)..." -ForegroundColor Yellow
try {
    $paymentBody = @{
        amount = 1000
        currency = "usd"
    } | ConvertTo-Json

    $paymentResponse = Invoke-RestMethod -Uri "$BASE_URL/payments/create-payment-intent" `
        -Method POST `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $paymentBody
    
    Write-Host "✗ Expected 401 but request succeeded" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401 -or $_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "✓ Correctly rejected unauthenticated request (401)" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected error: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Test user registered" -ForegroundColor Green
Write-Host "  ✓ Payment intent $10.00 USD created" -ForegroundColor Green
Write-Host "  ✓ Payment intent $50.00 USD created" -ForegroundColor Green
Write-Host "  ✓ Payment intent $199.99 USD created" -ForegroundColor Green
Write-Host "  ✓ Payment intent €25.00 EUR created" -ForegroundColor Green
Write-Host "  ✓ Unauthenticated request rejected" -ForegroundColor Green
Write-Host "`n✅ Task 3.2 BACKEND VALIDATED: Stripe Payment Intent API working!" -ForegroundColor Green
Write-Host ""
Write-Host "Next Step: Test Android UI" -ForegroundColor Cyan
Write-Host "  1. Build and run Android app" -ForegroundColor Gray
Write-Host "  2. Add items to cart" -ForegroundColor Gray
Write-Host "  3. Navigate to Payment Screen" -ForegroundColor Gray
Write-Host "  4. Click 'Pay with Stripe'" -ForegroundColor Gray
Write-Host "  5. Use test card: 4242 4242 4242 4242" -ForegroundColor Gray
Write-Host "  6. Any future date + any 3-digit CVC" -ForegroundColor Gray
Write-Host "  7. Verify payment completes and order is created" -ForegroundColor Gray
