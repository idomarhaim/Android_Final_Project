@echo off
REM ============================================================================
REM  Double-click me.
REM  Builds and installs onto a PHYSICAL phone connected over USB (or adb over
REM  Wi-Fi). Fails fast with a clear message if no authorized phone is attached,
REM  instead of silently falling back to the emulator.
REM ============================================================================
title Run GoalPilot on phone
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-goalpilot.ps1" -Target device %*
set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [FAILED] exit code %EXITCODE% - read the output above.
) else (
    echo [DONE]
)
echo.
pause
