@echo off
set "PATH=%APPDATA%\npm;C:\Program Files\nodejs;%PATH%"
cd /d "D:\Projects\Ozamiz"
echo ===================================================
echo Deploying ONLY analyzeFood (europe-west1)
echo ===================================================
call npx firebase deploy --only functions:analyzeFood --force
echo ===================================================
echo Finished!
echo ===================================================
pause
