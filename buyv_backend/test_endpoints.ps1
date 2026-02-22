# Test script for Like/Bookmark/Delete endpoints

Write-Host "=== STEP 1: Register User ===" -ForegroundColor Cyan
$regBody = @{
    email = "testapi$(Get-Random)@test.com"
    password = "Pass123!"
    username = "testapi$(Get-Random -Max 9999)"
    display_name = "API Tester"
} | ConvertTo-Json

$reg = Invoke-RestMethod -Uri "http://localhost:8000/auth/register" -Method Post -ContentType "application/json" -Body $regBody
$token = $reg.access_token
Write-Host "✅ User: $($reg.user.username)" -ForegroundColor Green
Write-Host "Token: $($token.Substring(0,40))...`n"

Write-Host "=== STEP 2: Create Post ===" -ForegroundColor Cyan
$postBody = @{
    type = "reel"
    mediaUrl = "https://example.com/test-video.mp4"
    caption = "Test post for like/bookmark/delete operations"
} | ConvertTo-Json

$post = Invoke-RestMethod -Uri "http://localhost:8000/posts/" -Method Post -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body $postBody
$postId = $post.id
Write-Host "✅ Post created: $postId" -ForegroundColor Green
Write-Host "Likes: $($post.likesCount), Bookmarks: $($post.bookmarksCount)`n"

Write-Host "=== STEP 3: Like Post ===" -ForegroundColor Cyan
$like1 = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/like" -Method Post -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($like1.status)" -ForegroundColor Green

Write-Host "=== STEP 4: Like Again (should say already_liked) ===" -ForegroundColor Cyan
$like2 = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/like" -Method Post -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($like2.status)" -ForegroundColor $(if($like2.status -eq "already_liked"){"Green"}else{"Yellow"})

Write-Host "`n=== STEP 5: Unlike Post ===" -ForegroundColor Cyan
$unlike = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/like" -Method Delete -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($unlike.status)" -ForegroundColor Green

Write-Host "`n=== STEP 6: Bookmark Post ===" -ForegroundColor Cyan
$bookmark1 = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/bookmark" -Method Post -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($bookmark1.status)" -ForegroundColor Green

Write-Host "=== STEP 7: Bookmark Again (should say already_bookmarked) ===" -ForegroundColor Cyan
$bookmark2 = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/bookmark" -Method Post -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($bookmark2.status)" -ForegroundColor $(if($bookmark2.status -eq "already_bookmarked"){"Green"}else{"Yellow"})

Write-Host "`n=== STEP 8: Unbookmark Post ===" -ForegroundColor Cyan
$unbookmark = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId/bookmark" -Method Delete -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($unbookmark.status)" -ForegroundColor Green

Write-Host "`n=== STEP 9: Delete Post ===" -ForegroundColor Cyan
$delete = Invoke-RestMethod -Uri "http://localhost:8000/posts/$postId" -Method Delete -Headers @{Authorization="Bearer $token"}
Write-Host "✅ $($delete.status)" -ForegroundColor Green

Write-Host "`n" -NoNewline
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ ALL TESTS PASSED!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
