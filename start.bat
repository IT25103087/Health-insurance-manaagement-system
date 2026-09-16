@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo   HealthShield Sri Lanka - Health Insurance Management System
echo ====================================================================
echo.

:: 1. Verify Java 17+
echo [*] Checking Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in your system PATH.
    echo Please install JDK 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%g
)
echo [OK] Java detected: %JAVA_VER%
echo.

:: 2. Verify Maven
echo [*] Checking Apache Maven...
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed or not in your system PATH.
    echo Please install Apache Maven 3.8+ or configure your PATH.
    pause
    exit /b 1
)
echo [OK] Maven detected.
echo.

:: 3. Check MySQL Connectivity on default port 3306
echo [*] Checking MySQL service on localhost:3306...
powershell -Command "$t = New-Object Net.Sockets.TcpClient; try { $t.Connect('127.0.0.1', 3306); Write-Host '[OK] MySQL port 3306 is reachable.'; $t.Close(); exit 0 } catch { Write-Host '[WARN] MySQL port 3306 is not reachable. Ensure MySQL service / XAMPP is started.'; exit 1 }"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [NOTE] If your MySQL server is running on a different port or host,
    echo set DB_USERNAME and DB_PASSWORD environment variables or update application.properties.
    echo.
)

:: 4. Build and Run Spring Boot Application
echo [*] Starting HealthShield Spring Boot application on port 8080...
echo Access the portal at: http://localhost:8080
echo Pre-seeded Admin credentials: admin / Admin#Pass2026
echo Pre-seeded Customer credentials: customer / Kasun#Pass2026
echo ====================================================================
echo.

mvn spring-boot:run

pause
