@echo off
setlocal enabledelayedexpansion

set OUTPUT_FILE=all_code.txt
del %OUTPUT_FILE%

set BASE_PATH=app\src\main

for /R %BASE_PATH% %%f in (*.kt *.xml) do (
    echo %%f | findstr /C:"\build\" >nul
    if errorlevel 1 (
        echo --------- %%f --------- >> %OUTPUT_FILE%
        type "%%f" >> %OUTPUT_FILE%
        echo. >> %OUTPUT_FILE%
    )
)
