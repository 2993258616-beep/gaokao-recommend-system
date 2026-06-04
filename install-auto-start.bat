@echo off
chcp 65001 >nul
cd /d "%~dp0"
schtasks /Create /F /TN "GaokaoRecommendSystem" /SC ONLOGON /TR "powershell.exe -WindowStyle Hidden -ExecutionPolicy Bypass -File \"%~dp0tools\start-gaokao-server.ps1\""
echo.
echo Auto start has been configured for this Windows user.
pause
