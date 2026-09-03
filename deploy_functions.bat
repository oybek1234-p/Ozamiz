@echo off
set "PATH=%APPDATA%\npm;C:\Program Files\nodejs;%PATH%"
cd /d "D:\Projects\Ozamiz"
echo ===================================================
echo Deploying Cloud Functions for Ozamiz (europe-west4)
echo ===================================================
call npx firebase deploy --only functions --force
echo ===================================================
echo Finished!
echo ===================================================
pause
