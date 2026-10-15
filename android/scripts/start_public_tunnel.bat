@echo off
title Photovance AI - Public Cloudflare HTTPS Tunnel
echo ======================================================================
echo    PHOTOVANCE AI - PUBLIC CLOUDFLARE HTTPS TUNNEL FOR MOBILE APP
echo ======================================================================
echo.
echo Connecting your local AI backend (port 7860) to a secure public HTTPS endpoint...
echo Once connected, copy the "https://....trycloudflare.com" URL and enter it
echo into the Photovance AI Android App Settings (or paste in Google Play Console).
echo.
"C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://127.0.0.1:7860
pause
