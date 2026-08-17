# Install-GitHooks.ps1 - copy version-controlled hook sources (scripts\git-hooks\*)
# into .git\hooks\ (which git does not version). Re-run after cloning or after a
# hook source changes. Existing hooks with different content are overwritten with
# a note - hook sources in scripts\git-hooks\ are the single source of truth.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts\Install-GitHooks.ps1 [-RepoPath <path>]
# ASCII-only on purpose - Windows PowerShell 5.1 misparses BOM-less UTF-8.

param(
    [string]$RepoPath
)

$ErrorActionPreference = 'Stop'
if (-not $RepoPath) { $RepoPath = Split-Path $PSScriptRoot -Parent }
$repo = (Resolve-Path $RepoPath).Path
$srcDir = Join-Path $repo 'scripts\git-hooks'
$dstDir = Join-Path $repo '.git\hooks'

if (-not (Test-Path $srcDir)) { Write-Output "no scripts\git-hooks\ directory - nothing to install."; exit 0 }
if (-not (Test-Path $dstDir)) { Write-Output "no .git\hooks\ directory - is $repo a git repo?"; exit 1 }

$installed = 0
foreach ($f in Get-ChildItem $srcDir -File) {
    $dst = Join-Path $dstDir $f.Name
    if ((Test-Path $dst) -and ((Get-FileHash $dst).Hash -eq (Get-FileHash $f.FullName).Hash)) {
        Write-Output "up to date: $($f.Name)"
        continue
    }
    if (Test-Path $dst) { Write-Output "overwriting existing: $($f.Name)" }
    Copy-Item $f.FullName $dst -Force
    Write-Output "installed: $($f.Name)"
    $installed++
}
Write-Output "Install-GitHooks: $installed hook(s) installed/updated in .git\hooks\."
exit 0
