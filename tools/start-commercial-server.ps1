$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectDir "target\commercial\gaokao-commercial.jar"
$sourceDb = Join-Path $projectDir "data\gaokao_recommend_db.mv.db"
$commercialDb = Join-Path $projectDir "data\gaokao_commercial_db.mv.db"
$outLog = Join-Path $projectDir "target\gaokao-commercial.out.log"
$errLog = Join-Path $projectDir "target\gaokao-commercial.err.log"

if (-not (Test-Path $jarPath)) {
    throw "Commercial jar not found: $jarPath"
}

if (-not (Test-Path $commercialDb) -and (Test-Path $sourceDb)) {
    Copy-Item -LiteralPath $sourceDb -Destination $commercialDb -Force
}

$running = Get-CimInstance Win32_Process |
    Where-Object { $_.Name -like "java*" -and $_.CommandLine -like "*gaokao-commercial.jar*" } |
    Select-Object -First 1

if ($running) {
    Write-Host "Commercial server is already running on port 8082."
    return
}

Start-Process `
    -FilePath "C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe" `
    -ArgumentList "-Xms48m", "-Xmx192m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m", "-Xss512k", "-jar", $jarPath, "--spring.profiles.active=commercial" `
    -WorkingDirectory $projectDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden

Write-Host "Commercial server started: http://localhost:8082/"
