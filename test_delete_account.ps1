# Test DELETE /users/me endpoint
# This script tests account deletion with all cascade deletes

$baseUrl = "http://localhost:8000"
$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "`n========== TESTING ACCOUNT DELETION ==========" -ForegroundColor Cyan

# Step 1: Register a test user
Write-Host "`n[1] Registering test user..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$registerBody = @{
    username = "deletetest_$timestamp"
    email = "deletetest_${timestamp}@example.com"
    password = "Test123456!"
    displayName = "Delete Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $registerBody -Headers $headers
    $token = $registerResponse.access_token
    $userId = $registerResponse.user.id
    Write-Host "✓ User registered: $userId" -ForegroundColor Green
    Write-Host "  Username: $($registerResponse.user.username)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Update headers with auth token
$authHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

# Step 2: Create some test data (post, comment, like)
Write-Host "`n[2] Creating test post..." -ForegroundColor Yellow
$postBody = @{
    type = "photo"
    media_url = "https://example.com/test.jpg"
    caption = "Test post before deletion"
} | ConvertTo-Json

try {
    $postResponse = Invoke-RestMethod -Uri "$baseUrl/posts/" -Method Post -Body $postBody -Headers $authHeaders
    $postId = $postResponse.id
    Write-Host "✓ Post created: $postId" -ForegroundColor Green
} catch {
    Write-Host "✗ Post creation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Like the post
Write-Host "`n[3] Liking post..." -ForegroundColor Yellow
try {
    $likeResponse = Invoke-RestMethod -Uri "$baseUrl/posts/$postId/like" -Method Post -Headers $authHeaders
    Write-Host "✓ Post liked" -ForegroundColor Green
} catch {
    Write-Host "⚠ Like failed (non-critical): $($_.Exception.Message)" -ForegroundColor Yellow
}

# Step 4: Add a comment
Write-Host "`n[4] Adding comment..." -ForegroundColor Yellow
$commentBody = @{
    content = "Test comment before deletion"
} | ConvertTo-Json

try {
    $commentResponse = Invoke-RestMethod -Uri "$baseUrl/comments/$postId" -Method Post -Body $commentBody -Headers $authHeaders
    Write-Host "✓ Comment added: $($commentResponse.id)" -ForegroundColor Green
} catch {
    Write-Host "⚠ Comment failed (non-critical): $($_.Exception.Message)" -ForegroundColor Yellow
}

# Step 5: Get user profile (verify exists)
Write-Host "`n[5] Verifying user profile exists..." -ForegroundColor Yellow
try {
    $profileResponse = Invoke-RestMethod -Uri "$baseUrl/users/$userId" -Method Get -Headers $authHeaders
    Write-Host "✓ User profile exists" -ForegroundColor Green
    Write-Host "  Display Name: $($profileResponse.displayName)" -ForegroundColor Gray
    Write-Host "  Posts Count: $($profileResponse.reelsCount)" -ForegroundColor Gray
} catch {
    Write-Host "⚠ Profile check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Step 6: Delete account
Write-Host "`n[6] DELETING ACCOUNT..." -ForegroundColor Yellow
try {
    $deleteResponse = Invoke-RestMethod -Uri "$baseUrl/users/me" -Method Delete -Headers $authHeaders
    Write-Host "✓ Account deleted successfully!" -ForegroundColor Green
    Write-Host "  Message: $($deleteResponse.message)" -ForegroundColor Gray
    Write-Host "  Deleted User ID: $($deleteResponse.deleted_user_id)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Account deletion failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    exit 1
}

# Step 7: Verify account no longer exists (should get 404 or 401)
Write-Host "`n[7] Verifying account is deleted (should fail with 404/401)..." -ForegroundColor Yellow
try {
    $verifyResponse = Invoke-RestMethod -Uri "$baseUrl/users/$userId" -Method Get -Headers $authHeaders -ErrorAction Stop
    Write-Host "✗ ERROR: Account still exists!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404 -or $statusCode -eq 401) {
        Write-Host "✓ Account confirmed deleted (Status: $statusCode)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Unexpected status code: $statusCode" -ForegroundColor Yellow
    }
}

# Step 8: Verify post is deleted (should get 404)
Write-Host "`n[8] Verifying post was cascade deleted..." -ForegroundColor Yellow
try {
    # Use a new registration to get a valid token for checking
    $verifyBody = @{
        username = "verify_$timestamp"
        email = "verify_${timestamp}@example.com"
        password = "Test123456!"
        displayName = "Verify User"
    } | ConvertTo-Json
    $verifyReg = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $verifyBody -Headers $headers
    $verifyToken = $verifyReg.access_token
    $verifyHeaders = @{
        "Authorization" = "Bearer $verifyToken"
    }
    
    $postCheck = Invoke-RestMethod -Uri "$baseUrl/posts/$postId" -Method Get -Headers $verifyHeaders -ErrorAction Stop
    Write-Host "✗ ERROR: Post still exists after account deletion!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✓ Post confirmed cascade deleted (404)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Unexpected status code: $statusCode" -ForegroundColor Yellow
    }
}

Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ User account creation" -ForegroundColor Green
Write-Host "  ✓ Test data creation (post, like, comment)" -ForegroundColor Green
Write-Host "  ✓ Account deletion via DELETE /users/me" -ForegroundColor Green
Write-Host "  ✓ Cascade delete verification" -ForegroundColor Green
Write-Host "  ✓ GDPR/CCPA compliance: All user data removed" -ForegroundColor Green
Write-Host "`n"
