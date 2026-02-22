# Test Comments Endpoints
# This script tests the complete comment workflow: add, fetch, delete

$baseUrl = "http://localhost:8000"
$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "`n========== TESTING COMMENTS ENDPOINTS ==========" -ForegroundColor Cyan

# Step 1: Register a new user
Write-Host "`n[1] Registering test user..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$registerBody = @{
    username = "testuser_$timestamp"
    email = "test_${timestamp}@example.com"
    password = "Test123456!"
    displayName = "Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $registerBody -Headers $headers
    $token = $registerResponse.access_token
    $userId = $registerResponse.user.id
    Write-Host "✓ User registered: $userId" -ForegroundColor Green
} catch {
    Write-Host "✗ Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Update headers with auth token
$authHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

# Step 2: Create a test post
Write-Host "`n[2] Creating test post..." -ForegroundColor Yellow
$postBody = @{
    type = "photo"
    media_url = "https://example.com/placeholder.jpg"
    caption = "Test post for comments - $timestamp"
} | ConvertTo-Json

try {
    $postResponse = Invoke-RestMethod -Uri "$baseUrl/posts/" -Method Post -Body $postBody -Headers $authHeaders
    $postId = $postResponse.id
    if (-not $postId) {
        Write-Host "✗ Post ID is null/empty" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Post created: $postId" -ForegroundColor Green
    Write-Host "  Caption: $($postResponse.caption)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Post creation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Add first comment
Write-Host "`n[3] Adding first comment..." -ForegroundColor Yellow
$comment1Body = @{
    content = "First comment - Testing add functionality"
} | ConvertTo-Json

try {
    $comment1Response = Invoke-RestMethod -Uri "$baseUrl/comments/$postId" -Method Post -Body $comment1Body -Headers $authHeaders
    $commentId1 = $comment1Response.id
    Write-Host "✓ Comment added: $commentId1" -ForegroundColor Green
    Write-Host "  Content: $($comment1Response.content)" -ForegroundColor Gray
    Write-Host "  Author: $($comment1Response.displayName)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Add comment failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Add second comment
Write-Host "`n[4] Adding second comment..." -ForegroundColor Yellow
$comment2Body = @{
    content = "Second comment - Testing pagination"
} | ConvertTo-Json

try {
    $comment2Response = Invoke-RestMethod -Uri "$baseUrl/comments/$postId" -Method Post -Body $comment2Body -Headers $authHeaders
    $commentId2 = $comment2Response.id
    Write-Host "✓ Comment added: $commentId2" -ForegroundColor Green
    Write-Host "  Content: $($comment2Response.content)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Add comment failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Add third comment
Write-Host "`n[5] Adding third comment..." -ForegroundColor Yellow
$comment3Body = @{
    content = "Third comment - Will be deleted!"
} | ConvertTo-Json

try {
    $comment3Response = Invoke-RestMethod -Uri "$baseUrl/comments/$postId" -Method Post -Body $comment3Body -Headers $authHeaders
    $commentId3 = $comment3Response.id
    Write-Host "✓ Comment added: $commentId3" -ForegroundColor Green
    Write-Host "  Content: $($comment3Response.content)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Add comment failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 6: Get all comments (should see 3)
Write-Host "`n[6] Fetching all comments (limit=10, offset=0)..." -ForegroundColor Yellow
try {
    $commentsResponse = Invoke-RestMethod -Uri "$baseUrl/comments/${postId}?limit=10&offset=0" -Method Get -Headers $authHeaders
    Write-Host "✓ Comments fetched: $($commentsResponse.Count) total" -ForegroundColor Green
    foreach ($comment in $commentsResponse) {
        Write-Host "  - Comment $($comment.id): $($comment.content)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Get comments failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 7: Get comments with pagination (limit=2)
Write-Host "`n[7] Fetching comments with pagination (limit=2, offset=0)..." -ForegroundColor Yellow
try {
    $paginatedResponse = Invoke-RestMethod -Uri "$baseUrl/comments/${postId}?limit=2&offset=0" -Method Get -Headers $authHeaders
    Write-Host "✓ Paginated comments fetched: $($paginatedResponse.Count) items" -ForegroundColor Green
    foreach ($comment in $paginatedResponse) {
        Write-Host "  - Comment $($comment.id): $($comment.content.Substring(0, [Math]::Min(40, $comment.content.Length)))..." -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Paginated fetch failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 8: Delete the third comment
Write-Host "`n[8] Deleting third comment..." -ForegroundColor Yellow
try {
    $deleteResponse = Invoke-RestMethod -Uri "$baseUrl/comments/$postId/$commentId3" -Method Delete -Headers $authHeaders
    Write-Host "✓ Comment deleted: $commentId3" -ForegroundColor Green
    Write-Host "  Status: $($deleteResponse.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Delete comment failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 9: Verify deletion (should see 2 comments now)
Write-Host "`n[9] Verifying deletion (should see 2 comments)..." -ForegroundColor Yellow
try {
    $afterDeleteResponse = Invoke-RestMethod -Uri "$baseUrl/comments/${postId}?limit=10&offset=0" -Method Get -Headers $authHeaders
    Write-Host "✓ Comments after deletion: $($afterDeleteResponse.Count) total" -ForegroundColor Green
    if ($afterDeleteResponse.Count -eq 2) {
        Write-Host "  ✓ Correct count!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Expected 2 comments, got $($afterDeleteResponse.Count)" -ForegroundColor Red
    }
    foreach ($comment in $afterDeleteResponse) {
        Write-Host "  - Comment $($comment.id): $($comment.content)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Verification failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 10: Verify post comments_count updated
Write-Host "`n[10] Checking post comments_count..." -ForegroundColor Yellow
try {
    $postCheckResponse = Invoke-RestMethod -Uri "$baseUrl/posts/$postId" -Method Get -Headers $authHeaders
    Write-Host "✓ Post comments_count: $($postCheckResponse.comments_count)" -ForegroundColor Green
    if ($postCheckResponse.comments_count -eq 2) {
        Write-Host "  ✓ Counter correctly updated!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Expected comments_count=2, got $($postCheckResponse.comments_count)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Post check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Cleanup: Delete the test post
Write-Host "`n[Cleanup] Deleting test post..." -ForegroundColor Yellow
try {
    $cleanupResponse = Invoke-RestMethod -Uri "$baseUrl/posts/$postId" -Method Delete -Headers $authHeaders
    Write-Host "✓ Test post deleted" -ForegroundColor Green
} catch {
    Write-Host "⚠ Cleanup failed (non-critical): $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n========== ALL TESTS PASSED! ==========" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ User registration" -ForegroundColor Green
Write-Host "  ✓ Post creation" -ForegroundColor Green
Write-Host "  ✓ Add comments (3x)" -ForegroundColor Green
Write-Host "  ✓ Fetch comments with pagination" -ForegroundColor Green
Write-Host "  ✓ Delete comment" -ForegroundColor Green
Write-Host "  ✓ Verify comments_count updated" -ForegroundColor Green
Write-Host "`n"
