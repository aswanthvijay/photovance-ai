@echo off
title AI Dress Changer Web Server
cd /d "%~dp0"
echo ========================================================
echo               AI DRESS CHANGER SERVER
echo ========================================================
echo Local URL:   http://127.0.0.1:7860
echo Network URL: http://0.0.0.0:7860
echo.
"D:\Image AI\StabilityMatrix-win-x64\Data\Assets\Python\cpython-3.12.14-windows-x86_64-none\python.exe" app.py
pause
