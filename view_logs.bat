@echo off
REM Script to view Android debug logs for the CURATOR app

echo Starting logcat for MapDebug logs...
REM Try using Android Studio's bundled ADB first
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat *:S MapDebug:D
    exit /b
)

REM Try common Android SDK path
if exist "C:\Android\sdk\platform-tools\adb.exe" (
    "C:\Android\sdk\platform-tools\adb.exe" logcat *:S MapDebug:D
    exit /b
)

REM If neither is found, display a message
echo ADB not found. Please:
echo 1. Open Android Studio
echo 2. Open View - Tool Windows - Logcat
echo 3. Filter by: MapDebug
echo 4. Run the app and click the Show button
pause
