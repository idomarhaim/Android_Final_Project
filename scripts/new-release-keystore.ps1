<#
.SYNOPSIS
    Create the ONE release signing key for GoalPilot and register its credentials
    in the git-ignored local.properties.

.DESCRIPTION
    Android identifies an app by its signing key, not by its package name. Once a
    build signed with this key is installed on somebody's phone, every future
    update must be signed with the SAME key - Android refuses a signature change
    outright, and the only recovery is "everybody uninstalls first".

    So this key is permanent. Back the .jks file up somewhere that is not this
    machine, and keep the password. Losing it means every existing installation
    is stranded on its current version forever.

    The script refuses to overwrite an existing keystore for exactly that reason.

    The generated password is written straight into local.properties (git-ignored)
    and is never printed, so it does not end up in a terminal scrollback or an
    agent transcript. Read it from local.properties when you need it - for
    example when adding the GitHub Actions secrets (see docs/RELEASING.md).

    NOTE ON ENCODING: this file is deliberately pure ASCII. Windows PowerShell
    5.1 decodes a BOM-less .ps1 as ANSI, so a UTF-8 em dash inside a
    double-quoted string arrives as three CP1252 characters - the last of which
    is a double quote that terminates the string early and breaks the parse.
    Keep it ASCII.

.PARAMETER Password
    Use a password you choose instead of a generated one. Omit it and a 32-char
    random password is generated.

.PARAMETER Alias
    Key alias inside the keystore. Default 'goalpilot'.

.EXAMPLE
    .\scripts\new-release-keystore.ps1

.EXAMPLE
    .\scripts\new-release-keystore.ps1 -Password 'my own passphrase'
#>
[CmdletBinding()]
param(
    [string]$Password = '',
    [string]$Alias = 'goalpilot',
    [string]$KeystorePath = 'app/goalpilot-release.jks',
    # Baked into the certificate. Not security-relevant for sideloaded APKs, but
    # it is permanent, so it may as well be right.
    [string]$Dname = 'CN=Ido Marhaim, OU=GoalPilot, O=GoalPilot, L=Tel Aviv, C=IL'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $repoRoot $KeystorePath
$localProps = Join-Path $repoRoot 'local.properties'

# --- Refuse to destroy an existing key ------------------------------------
if (Test-Path $keystore) {
    Write-Host "A keystore already exists at $KeystorePath." -ForegroundColor Yellow
    Write-Host "Refusing to overwrite it: replacing a release key strands every" -ForegroundColor Yellow
    Write-Host "existing installation. Delete it by hand if you are certain." -ForegroundColor Yellow
    exit 1
}

# --- Locate keytool from the pinned JDK 21 --------------------------------
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\keytool.exe'))) {
    # gradle.properties pins the same JDK; reuse it rather than guessing.
    $pinned = Select-String -Path (Join-Path $repoRoot 'gradle.properties') `
        -Pattern '^org\.gradle\.java\.home=(.+)$' | Select-Object -First 1
    if ($pinned) { $javaHome = $pinned.Matches[0].Groups[1].Value.Trim() }
}
$keytool = Join-Path $javaHome 'bin\keytool.exe'
if (-not (Test-Path $keytool)) {
    throw "keytool not found. Set JAVA_HOME to a JDK 21 install and re-run. Looked in: $javaHome"
}

# --- Password -------------------------------------------------------------
if (-not $Password) {
    # Alphanumeric only: a keystore password travels through Gradle properties,
    # a GitHub secret and a base64 round-trip, and every quoting layer in that
    # chain is a chance for a punctuation character to be mangled.
    $chars = 'abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'.ToCharArray()
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $Password = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

# --- Generate -------------------------------------------------------------
Write-Host "Generating a 4096-bit RSA release key (valid ~27 years)..." -ForegroundColor Cyan
& $keytool -genkeypair -noprompt `
    -keystore $keystore `
    -alias $Alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $Password -keypass $Password `
    -dname $Dname
if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

# --- Record the credentials where app/build.gradle.kts reads them ---------
$block = @"

# --- Release signing (written by scripts/new-release-keystore.ps1) --------
# This file is git-ignored. The keystore itself is git-ignored too.
# BACK BOTH UP OFF THIS MACHINE - see docs/RELEASING.md.
RELEASE_STORE_FILE=$KeystorePath
RELEASE_STORE_PASSWORD=$Password
RELEASE_KEY_ALIAS=$Alias
RELEASE_KEY_PASSWORD=$Password
"@
Add-Content -Path $localProps -Value $block -Encoding utf8

# --- Report the fingerprints (public information - safe to print) ---------
Write-Host ""
Write-Host "Keystore written to $KeystorePath" -ForegroundColor Green
Write-Host "Credentials appended to local.properties (git-ignored)." -ForegroundColor Green
Write-Host ""
& $keytool -list -v -keystore $keystore -alias $Alias -storepass $Password |
    Select-String -Pattern 'SHA1:|SHA256:|Valid from'

Write-Host ""
Write-Host "NEXT - Google Sign-In will FAIL for release builds until this SHA-1" -ForegroundColor Yellow
Write-Host "is registered on the Firebase Android app. Run:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  firebase apps:android:sha:create com.idomarhaim.goalpilot <SHA-1 above> --project goalpilot-56e30" -ForegroundColor White
Write-Host ""
Write-Host "then re-download google-services.json. Details: docs/RELEASING.md" -ForegroundColor Yellow
