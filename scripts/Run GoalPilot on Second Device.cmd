@echo off
REM ============================================================================
REM  Double-click me.
REM  Same as 'Run GoalPilot', but targets the SECOND emulator Pixel_10_Pro_XL_B
REM  and boots it alongside the first one instead of adopting whatever is up.
REM  This is the device for the spec section 7 two-account demo: sign in here as
REM  rachil751@gmail.com while the first emulator stays on name.iddo@gmail.com.
REM  Both emulators talk to the same live Firebase project, so keep them on
REM  different accounts - see docs\OPERATIONS.md.
REM ============================================================================
title Run GoalPilot - second device
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-goalpilot.ps1" -Target emulator -Avd Pixel_10_Pro_XL_B %*
set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [FAILED] exit code %EXITCODE% - read the output above.
) else (
    echo [DONE]
)
echo.
pause
