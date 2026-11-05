@echo off
set "PATH=%APPDATA%\npm;C:\Program Files\nodejs;%PATH%"
cd /d "D:\Projects\Ozamiz"
echo ===================================================
echo Deploying ONLY analyzeFoodVer2 (europe-west1)
echo ===================================================
call npx firebase deploy --only functions:analyzeFoodVer2 --force
echo ===================================================
echo Finished!
echo ===================================================
pause
