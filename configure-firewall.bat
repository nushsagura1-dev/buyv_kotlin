@echo off
echo ========================================
echo Configuration Pare-feu pour BuyV Backend
echo ========================================
echo.
echo Ce script va autoriser le port 8000 dans le pare-feu Windows
echo (Necessaire pour que le telephone puisse se connecter au backend)
echo.
pause

netsh advfirewall firewall add rule name="BuyV Backend Port 8000 (TCP)" dir=in action=allow protocol=TCP localport=8000
netsh advfirewall firewall add rule name="BuyV Backend Port 8000 (UDP)" dir=in action=allow protocol=UDP localport=8000

echo.
echo ========================================
echo Configuration terminee avec succes!
echo ========================================
echo.
echo Le port 8000 est maintenant autorise dans le pare-feu.
echo Votre telephone peut maintenant se connecter au backend.
echo.
pause
