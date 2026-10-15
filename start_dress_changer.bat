@echo off
title AI Dress & Outfit Changer
cd /d "D:\image mod"

echo ========================================================
echo        Starting AI Dress & Outfit Changer
echo ========================================================
echo.
echo Launching local engine & web app...
start http://127.0.0.1:7860

"D:\Image AI L model\runtime\python.exe" app.py
pause
