# Test fetching articles and verify author information is displayed correctly

Write-Host "Testing article list with author information..." -ForegroundColor Cyan

try {
    $articles = Invoke-RestMethod -Uri "http://localhost:8082/api/articles?page=0&size=5" `
        -Method GET
    
    Write-Host "`n✓ Retrieved article list successfully" -ForegroundColor Green
    Write-Host "Total articles: $($articles.total)" -ForegroundColor Gray
    Write-Host "`nRecent articles:" -ForegroundColor Cyan
    
    foreach ($article in $articles.records) {
        Write-Host "`n  Article ID: $($article.id)" -ForegroundColor White
        Write-Host "  Title: $($article.title)" -ForegroundColor White
        Write-Host "  Author ID: $($article.authorId)" -ForegroundColor White
        
        if ($article.authorName) {
            Write-Host "  Author Name: $($article.authorName)" -ForegroundColor Green
        } else {
            Write-Host "  Author Name: Unknown Author" -ForegroundColor Red
        }
        
        if ($article.authorNickname) {
            Write-Host "  Author Nickname: $($article.authorNickname)" -ForegroundColor Green
        } else {
            Write-Host "  Author Nickname: Not available" -ForegroundColor Yellow
        }
    }
    
    Write-Host "`n✓ Test completed!" -ForegroundColor Green
    
} catch {
    Write-Host "✗ Failed to fetch articles" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Yellow
    exit 1
}
