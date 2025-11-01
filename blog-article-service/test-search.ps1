# 搜索功能测试脚本
# 测试文章搜索 API

$baseUrl = "http://localhost:8082/api/articles"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   文章搜索功能测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 测试1: 搜索包含 "Spring" 的文章
Write-Host "[1] 搜索包含 'Spring' 的文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=Spring&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 'Spring' 的文章" -ForegroundColor Green
    
    if ($count -gt 0) {
        Write-Host "`n  前 3 篇文章:" -ForegroundColor Cyan
        $response.records | Select-Object -First 3 | ForEach-Object {
            Write-Host "    - [$($_.id)] $($_.title)" -ForegroundColor White
            if ($_.summary) {
                $summaryPreview = if ($_.summary.Length -gt 50) { $_.summary.Substring(0, 50) + "..." } else { $_.summary }
                Write-Host "      摘要: $summaryPreview" -ForegroundColor Gray
            }
        }
    }
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试2: 搜索包含 "Vue" 的文章
Write-Host "`n[2] 搜索包含 'Vue' 的文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=Vue&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 'Vue' 的文章" -ForegroundColor Green
    
    if ($count -gt 0) {
        Write-Host "`n  示例文章:" -ForegroundColor Cyan
        $response.records | Select-Object -First 2 | ForEach-Object {
            Write-Host "    - [$($_.id)] $($_.title)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试3: 搜索包含 "微服务" 的文章
Write-Host "`n[3] 搜索包含 '微服务' 的文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=微服务&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 '微服务' 的文章" -ForegroundColor Green
    
    if ($count -gt 0) {
        Write-Host "`n  示例文章:" -ForegroundColor Cyan
        $response.records | Select-Object -First 2 | ForEach-Object {
            Write-Host "    - [$($_.id)] $($_.title)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试4: 搜索包含 "Docker" 的文章
Write-Host "`n[4] 搜索包含 'Docker' 的文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=Docker&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 'Docker' 的文章" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试5: 搜索不存在的关键词
Write-Host "`n[5] 搜索不存在的关键词 'xyz123'..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=xyz123&page=0&size=5" -Method Get
    $count = $response.records.Count
    if ($count -eq 0) {
        Write-Host "  ✓ 正确返回空结果" -ForegroundColor Green
    } else {
        Write-Host "  ! 意外找到 $count 篇文章" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试6: 搜索中文关键词 "开发"
Write-Host "`n[6] 搜索中文关键词 '开发'..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=开发&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 '开发' 的文章" -ForegroundColor Green
    
    if ($count -gt 0) {
        Write-Host "`n  示例文章:" -ForegroundColor Cyan
        $response.records | Select-Object -First 3 | ForEach-Object {
            Write-Host "    - [$($_.id)] $($_.title)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试7: 搜索空格和特殊字符
Write-Host "`n[7] 搜索包含 'API' 的文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl`?keyword=API&page=0&size=5" -Method Get
    $count = $response.records.Count
    Write-Host "  ✓ 找到 $count 篇包含 'API' 的文章" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 搜索失败: $_" -ForegroundColor Red
}

# 测试8: 分页测试
Write-Host "`n[8] 测试搜索结果分页..." -ForegroundColor Yellow
try {
    $page1 = Invoke-RestMethod -Uri "$baseUrl`?keyword=开发&page=0&size=3" -Method Get
    $page2 = Invoke-RestMethod -Uri "$baseUrl`?keyword=开发&page=1&size=3" -Method Get
    
    Write-Host "  ✓ 第1页: $($page1.records.Count) 篇文章" -ForegroundColor Green
    Write-Host "  ✓ 第2页: $($page2.records.Count) 篇文章" -ForegroundColor Green
    Write-Host "  ✓ 总计: $($page1.total) 篇文章" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 分页测试失败: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   测试完成!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
