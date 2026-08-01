@echo off
REM ============================================================================
REM  Double-click me.
REM  Boots the Pixel_10_Pro_XL emulator and stops there - no Gradle build, so
REM  nothing can touch app\build\ while you are working in VS Code.
REM ============================================================================
title Start Emulator
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-goalpilot.ps1" -Target emulator -SkipInstall %*
set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [FAILED] exit code %EXITCODE% - read the output above.
) else (
    echo [DONE] The emulator keeps running after this window closes.
)
echo.
pause
