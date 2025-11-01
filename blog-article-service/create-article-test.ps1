# 创建文章测试脚本
Write-Host "=== 创建文章 API 测试 ===" -ForegroundColor Cyan
Write-Host ""

# 文章数据
$newArticle = @{
    authorId = 1
    title = "如何使用 PowerShell 进行自动化运维"
    summary = "PowerShell 是 Windows 系统管理员的强大工具，本文介绍如何使用 PowerShell 脚本进行日常运维任务的自动化..."
    coverImageUrl = "https://picsum.photos/400/300?random=100"
    content = @"
# 如何使用 PowerShell 进行自动化运维

PowerShell 是微软开发的任务自动化和配置管理框架。

## 基本命令

1. Get-Process - 获取进程列表
2. Get-Service - 获取服务状态
3. Start-Service - 启动服务

## 脚本示例

```powershell
# 检查服务状态
Get-Service -Name "PostgreSQL" | Format-Table -AutoSize
```

## 最佳实践

- 使用参数验证
- 添加错误处理
- 记录日志信息
"@
    status = "PUBLISHED"
} | ConvertTo-Json -Depth 10

Write-Host "文章数据:" -ForegroundColor Yellow
Write-Host $newArticle -ForegroundColor Gray
Write-Host ""

Write-Host "发送 POST 请求到 /api/articles..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/api/articles" `
                                  -Method Post `
                                  -ContentType "application/json" `
                                  -Body $newArticle `
                                  -ErrorAction Stop
    
    Write-Host "✓ 文章创建成功!" -ForegroundColor Green
    Write-Host ""
    Write-Host "创建的文章信息:" -ForegroundColor Cyan
    Write-Host "  ID: $($response.id)" -ForegroundColor White
    Write-Host "  标题: $($response.title)" -ForegroundColor White
    Write-Host "  作者ID: $($response.authorId)" -ForegroundColor White
    Write-Host "  状态: $($response.status)" -ForegroundColor White
    Write-Host "  发布时间: $($response.publishTime)" -ForegroundColor White
    Write-Host ""
    
    # 验证文章列表
    Write-Host "验证文章是否在列表中..." -ForegroundColor Yellow
    $articles = Invoke-RestMethod -Uri "http://localhost:8082/api/articles?page=0&size=1" -ErrorAction Stop
    Write-Host "✓ 当前文章总数: $($articles.total)" -ForegroundColor Green
    Write-Host "  最新文章: $($articles.records[0].title)" -ForegroundColor Gray
    
} catch {
    Write-Host "✗ 创建文章失败" -ForegroundColor Red
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Gray
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "HTTP 状态码: $statusCode" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
