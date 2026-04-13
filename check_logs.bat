@echo off
REM First, reconnect the device
echo Checking device connection...
call "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" kill-server
timeout /t 2 /nobreak
call "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" start-server
timeout /t 2 /nobreak

echo.
echo Checking for connected devices...
call "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" devices

echo.
echo Waiting for you to run the app and click "Show" button...
echo Displaying MapDebug logs in real-time:
echo ===============================================
call "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat -c
call "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat MapDebug:D *:S
