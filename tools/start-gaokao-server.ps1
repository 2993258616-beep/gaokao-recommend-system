$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectDir "target\gaokao-recommend-system-1.0.0.jar"
$outLog = Join-Path $projectDir "target\gaokao-server.out.log"
$errLog = Join-Path $projectDir "target\gaokao-server.err.log"

if (-not (Test-Path $jarPath)) {
    throw "Jar not found: $jarPath"
}

$running = Get-CimInstance Win32_Process |
    Where-Object { $_.Name -like "java*" -and $_.CommandLine -like "*gaokao-recommend-system-1.0.0.jar*" } |
    Select-Object -First 1

if ($running) {
    Write-Host "Server is already running on port 8081."
    return
}

Start-Process `
    -FilePath "C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe" `
    -ArgumentList "-Xms32m", "-Xmx128m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=112m", "-Xss384k", "-jar", $jarPath `
    -WorkingDirectory $projectDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden

Write-Host "Server started: http://localhost:8081/"
