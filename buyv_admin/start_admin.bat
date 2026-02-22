@echo off
title Buyv Admin Panel
color 0A

echo ================================================
echo    BUYV ADMIN PANEL - Starting...
echo ================================================
echo.

REM Change to admin directory
cd /d "%~dp0"

REM Check if virtual environment exists
if not exist "venv\" (
    echo [1/3] Creating virtual environment...
    python -m venv venv
    echo.
)

REM Activate virtual environment
echo [2/3] Activating virtual environment...
call venv\Scripts\activate.bat

REM Install dependencies
echo [3/3] Installing/updating dependencies...
pip install -r requirements.txt --quiet
echo.

echo ================================================
echo    Admin Panel is starting...
echo ================================================
echo.
echo    Dashboard: http://localhost:5000/admin/
echo    Login: admin / admin123
echo.
echo    Press Ctrl+C to stop the server
echo ================================================
echo.

REM Start the admin panel
python admin_app.py

pause
