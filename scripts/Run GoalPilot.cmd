@echo off
REM ============================================================================
REM  Double-click me.
REM  Boots the emulator (or uses a plugged-in phone), builds the debug APK,
REM  installs it and launches GoalPilot. Android Studio is never involved.
REM ============================================================================
title Run GoalPilot
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-goalpilot.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [FAILED] exit code %EXITCODE% - read the output above.
) else (
    echo [DONE]
)
echo.
pause
