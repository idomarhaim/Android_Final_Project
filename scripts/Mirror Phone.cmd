@echo off
REM ============================================================================
REM  Double-click me.
REM  Puts your PHONE'S SCREEN on this monitor, with mouse and keyboard control
REM  -- what Android Studio's "Running Devices" pane does, without opening
REM  Android Studio.
REM
REM  Builds NOTHING and installs NOTHING. It only mirrors, so it is safe to run
REM  while a build is going on elsewhere.
REM
REM  Needs: the phone plugged in over USB with USB debugging authorised.
REM  Close the mirror window to stop.
REM ============================================================================
title Mirror phone
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0mirror-phone.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [FAILED] exit code %EXITCODE% - read the output above.
) else (
    echo [DONE]
)
echo.
pause
