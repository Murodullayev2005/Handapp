@echo off
title "Hand Motion Trajectory & Distance Warning - Windows Desktop"
echo ======================================================================
echo   Launching Hand Trajectory ^& Distance Warning App for Windows
echo ======================================================================
echo.

cd /d "%~dp0"

echo [1/2] Checking Python dependencies...
python -m pip install -r requirements.txt
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to install Python packages. Please check Python installation.
    pause
    exit /b 1
)

echo.
echo [2/2] Starting Hand Tracking ^& Distance Warning Application...
echo.
python main_windows.py

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application closed with code %ERRORLEVEL%.
)

pause
