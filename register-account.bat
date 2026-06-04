@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Account Register
echo.
echo Please enter the new username and password below.
echo.
"C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe" -Dfile.encoding=UTF-8 -cp "target\classes;C:\Users\JD\.m2\repository\org\springframework\security\spring-security-crypto\5.7.11\spring-security-crypto-5.7.11.jar;C:\Users\JD\.m2\repository\org\springframework\spring-jcl\5.3.31\spring-jcl-5.3.31.jar;C:\Users\JD\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar" com.gaokao.recommend.tools.RegisterAccountTool
echo.
pause
