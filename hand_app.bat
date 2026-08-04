::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCyDJGyX8VAjFBZBXguGAE+1EbsQ5+n//NbV9RpIA7ZqNobY1dQ=
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSDk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+JeA==
::cxY6rQJ7JhzQF1fEqQJQ
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCuDJH6L+0U8LVt1QhCLO3m7ELwI8dSqou6GsVkIRt43eYHf5bWeMK4R61GqcI4otg==
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
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