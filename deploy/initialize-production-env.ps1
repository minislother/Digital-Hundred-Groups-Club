param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $PSScriptRoot 'env\docker.env.example'
$targetPath = Join-Path $projectRoot '.env'

# The generated .env is local-only and is excluded by .gitignore.
if ((Test-Path $targetPath) -and -not $Force) {
    throw '.env already exists. Use -Force only when you intentionally want to rotate all local secrets.'
}

function New-RandomSecret([int]$ByteLength) {
    $bytes = New-Object byte[] $ByteLength
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($bytes)
    }
    finally {
        $random.Dispose()
    }

    return [Convert]::ToBase64String($bytes)
}

$content = Get-Content -LiteralPath $templatePath -Raw -Encoding UTF8
$content = $content.Replace('replace-with-strong-root-password', (New-RandomSecret 32))
$content = $content.Replace('replace-with-strong-db-password', (New-RandomSecret 32))
$content = $content.Replace('replace-with-strong-redis-password', (New-RandomSecret 32))
$content = $content.Replace('replace-with-at-least-32-random-bytes', (New-RandomSecret 48))

[System.IO.File]::WriteAllText($targetPath, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Generated local production environment file: $targetPath"
Write-Output 'Review domain names and CORS_ALLOWED_ORIGINS before deploying to a public server.'
