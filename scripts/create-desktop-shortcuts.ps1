#Requires -Version 5.1
<#
.SYNOPSIS
    Puts the four GoalPilot launchers on your Desktop as real shortcut icons.

.DESCRIPTION
    Creates (or refreshes) four .lnk files on the Desktop pointing at the .cmd
    launchers in this folder. Each shortcut starts in the repo root and borrows an
    icon from a Windows system DLL so they are distinguishable at a glance.

    Run once. Re-running just overwrites the same four shortcuts.
    Remove them by deleting the .lnk files from the Desktop; nothing else is touched.

.PARAMETER Destination
    Where to write the shortcuts. Defaults to the current user's Desktop.

.EXAMPLE
    .\scripts\create-desktop-shortcuts.ps1
#>
[CmdletBinding()]
param(
    [string]$Destination = [Environment]::GetFolderPath('Desktop')
)

$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath $Destination)) {
    throw "Destination folder not found: $Destination"
}

$shortcuts = @(
    @{ Name = 'GoalPilot - Run'; Cmd = 'Run GoalPilot.cmd'; Icon = "$env:SystemRoot\System32\shell32.dll,137"; Desc = 'Boot emulator or use phone, build, install, launch GoalPilot' },
    @{ Name = 'GoalPilot - Emulator only'; Cmd = 'Start Emulator Only.cmd'; Icon = "$env:SystemRoot\System32\imageres.dll,109"; Desc = 'Start the Pixel_10_Pro_XL emulator without building' },
    @{ Name = 'GoalPilot - Run on phone'; Cmd = 'Run On Phone.cmd'; Icon = "$env:SystemRoot\System32\imageres.dll,96"; Desc = 'Build and install onto the USB-connected phone' },
    @{ Name = 'GoalPilot - Second device'; Cmd = 'Run GoalPilot on Second Device.cmd'; Icon = "$env:SystemRoot\System32\imageres.dll,104"; Desc = 'Boot Pixel_10_Pro_XL_B alongside the first emulator and install (two-account demo)' }
)

$shell = New-Object -ComObject WScript.Shell
try {
    foreach ($s in $shortcuts) {
        $target = Join-Path $PSScriptRoot $s.Cmd
        if (-not (Test-Path -LiteralPath $target)) {
            Write-Warning "Skipping '$($s.Name)': $target not found."
            continue
        }

        $linkPath = Join-Path $Destination ($s.Name + '.lnk')
        $link = $shell.CreateShortcut($linkPath)
        $link.TargetPath = $target
        $link.WorkingDirectory = $RepoRoot
        $link.IconLocation = $s.Icon
        $link.Description = $s.Desc
        $link.Save()

        Write-Host "Created: $linkPath" -ForegroundColor Green
    }
}
finally {
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell)
}

Write-Host ''
Write-Host 'Done. You can also right-click a shortcut > Pin to Start / Pin to taskbar.' -ForegroundColor Cyan
