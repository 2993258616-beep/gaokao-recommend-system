$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $PSScriptRoot
$cloudflared = Join-Path $PSScriptRoot "cloudflared.exe"
$urlFile = Join-Path $projectDir "public-url.txt"
$outLog = Join-Path $projectDir "target\public-web.out.log"
$errLog = Join-Path $projectDir "target\public-web.err.log"

& (Join-Path $PSScriptRoot "start-gaokao-server.ps1")

if (-not (Test-Path $cloudflared)) {
    Write-Host "Downloading public tunnel tool..."
    Invoke-WebRequest `
        -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" `
        -OutFile $cloudflared
}

function Test-PublicUrl($url) {
    if (-not $url) { return $false }
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $url -MaximumRedirection 5 -TimeoutSec 12
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

$oldUrl = $null
if (Test-Path $urlFile) {
    $oldUrl = (Get-Content -Path $urlFile -Encoding UTF8 -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
}

$running = @(Get-CimInstance Win32_Process |
    Where-Object { $_.Name -like "cloudflared*" -and $_.CommandLine -like "*localhost:8081*" })

if ($running.Count -gt 0 -and (Test-PublicUrl $oldUrl)) {
    Write-Host "Public URL: $oldUrl"
    Write-Host "Saved to: $urlFile"
    return
}

foreach ($process in $running) {
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2

if (Test-Path $outLog) { Remove-Item -LiteralPath $outLog -Force }
if (Test-Path $errLog) { Remove-Item -LiteralPath $errLog -Force }
Start-Process `
    -FilePath $cloudflared `
    -ArgumentList "tunnel", "--protocol", "http2", "--url", "http://localhost:8081", "--no-autoupdate" `
    -WorkingDirectory $projectDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden

$publicUrl = $null
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    $text = ""
    if (Test-Path $outLog) { $text += Get-Content -Path $outLog -Raw -ErrorAction SilentlyContinue }
    if (Test-Path $errLog) { $text += Get-Content -Path $errLog -Raw -ErrorAction SilentlyContinue }
    if ($text -match "https://[a-zA-Z0-9-]+\.trycloudflare\.com") {
        $publicUrl = $matches[0]
        break
    }
}

if ($publicUrl) {
    Set-Content -Path $urlFile -Value $publicUrl -Encoding UTF8
    Write-Host "Public URL: $publicUrl"
    Write-Host "Saved to: $urlFile"
} else {
    Write-Host "Public URL was not generated. Check log:"
    Write-Host $errLog
}
