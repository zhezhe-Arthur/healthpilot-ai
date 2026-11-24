
# HealthPilot AI 本地测试登录脚本

# . "D:\AI\healthpilot-ai\scripts\login.ps1"

$baseUrl = "http://localhost:8080"

# 读取项目根目录中的测试账号文件
$accountPath = Join-Path $PSScriptRoot "..\account_test.json"

if (-not (Test-Path $accountPath)) {
    throw "未找到测试账号文件：$accountPath"
}

$account = Get-Content $accountPath -Raw -Encoding UTF8 |
    ConvertFrom-Json

if ([string]::IsNullOrWhiteSpace($account.email) -or
    [string]::IsNullOrWhiteSpace($account.password)) {
    throw "测试账号文件缺少 email 或 password"
}

# 清除旧Token，防止登录失败后继续使用过期凭证
$access = $null
$headers = $null

# 将登录信息转换为UTF-8请求体
$loginBody = @{
    email = $account.email
    password = $account.password
} | ConvertTo-Json

$utf8Body = [System.Text.Encoding]::UTF8.GetBytes($loginBody)

# 调用登录接口
$login = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/auth/login" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body $utf8Body `
    -ErrorAction Stop

if ($login.code -ne 0 -or
    [string]::IsNullOrWhiteSpace($login.data.accessToken)) {
    throw "登录失败：$($login.message)"
}

# 保存到当前PowerShell会话
$access = $login.data.accessToken
$headers = @{
    Authorization = "Bearer $access"
}

Write-Host "HealthPilot AI 登录成功" -ForegroundColor Green
Write-Host "账号：$($account.email)"
Write-Host "Access Token有效期：$($login.data.expiresIn) 秒"
Write-Host "已更新变量：access、headers"

