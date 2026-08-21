# Assert-NoControlChars.ps1 - refuse a commit that carries invisible control
# characters in text files. Layer-1 script per rules\hook-strategy.md: all logic
# lives here, the git hook is a thin caller.
#
# ASCII-only on purpose - Windows PowerShell 5.1 misparses BOM-less UTF-8.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts\Assert-NoControlChars.ps1 [-Staged] [-RepoPath <path>]
#
# WHY THIS EXISTS
# ---------------
# A shell heredoc collapses a doubled backslash before the interpreter sees it,
# so "\\b" arrives as "\b" and becomes U+0008 BACKSPACE - inside a regex, a path,
# or a sentence. See kb\dev\escapes-die-in-transit.md.
#
# It happened NINE times across two sessions on 2026-08-21, twice inside prose
# that was explaining the bug, and three times it reached the remote -- the ninth
# by a DIFFERENT session, in this repo, which is why the gate is here and not only
# in JARVIS. Every instance was caught by a sweep for codepoints below 32; the ones
# that got through are the ones where the sweep was run and its output was not read,
# or where no sweep existed in this repo at all. That is the whole argument for a hook:
#
#     A sweep you do not gate on is a sweep you did not run.
#
# WHY -Staged, AND WHY THAT IS NOT A WEAKER CHECK
# -----------------------------------------------
# A pre-commit gate answers "is THIS COMMIT sound", never "is the working tree
# tidy". The distinction is not theoretical here: this repo deadlocked three
# times in three days when whole-tree gates let a sibling's uncommitted edit
# freeze everyone's commit stream (kb\dev\flows\lease.md 4a/4b). The invariant is
# untouched, because corruption can only enter history through a commit.
#
# WHAT IT DELIBERATELY DOES NOT MATCH
# -----------------------------------
#   TAB (0x09), LF (0x0A), CR (0x0D)  - legitimate in text
#
# AND THAT IS A REAL HOLE, NOT A ROUNDING ERROR. The three legal characters are
# exactly the ones the three COMMONEST escapes collapse into: \\n, \\t, \\r.
# So this gate catches a corrupted regex or a path eaten by \\a / \\b, and is BLIND
# to a path eaten by \\r.
#
# Observed 2026-08-21, one path with BOTH halves, committed by a sibling:
#     C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md
# landed as JARVIS + <newline> + ules + <BEL> + gent-... . This gate found the BEL
# and could not have found the newline - it is a legal character in a Markdown file,
# and no scan can tell a wanted line break from an eaten \\r.
#
# The half that IS covered is the half that is otherwise invisible; the newline half
# at least shows up as a line breaking in a strange place. Do not read a pass here as
# 'the escapes survived'.
#   binary files                       - git's own -I does the detection, so a
#                                        .png / .jks / .apk is never inspected
#
# The pattern is the complement of those three below 0x20, plus DEL is left out
# on purpose: 0x7F appears in some legitimately-encoded content and has never
# been one of the observed failures. Widen it only with an instance in hand.

param(
    [switch]$Staged,
    [string]$RepoPath
)

$ErrorActionPreference = 'Stop'
if (-not $RepoPath) { $RepoPath = Split-Path $PSScriptRoot -Parent }
$repo = (Resolve-Path $RepoPath).Path
Push-Location $repo
try {
    # git's own PCRE engine, against the INDEX when -Staged. Doing this in git
    # rather than in PowerShell is deliberate: git decides what is binary, git
    # reads the staged blob rather than the working tree, and neither of those is
    # something a re-implementation here would get right on the first try.
    $pattern = '[\x00-\x08\x0B\x0C\x0E-\x1F]'
    $args = @('grep', '-I', '-n', '-P')
    if ($Staged) { $args += '--cached' }
    $args += $pattern

    $hits = & git @args 2>$null
    $found = ($LASTEXITCODE -eq 0)

    if (-not $found) {
        $scope = if ($Staged) { 'staged' } else { 'tracked' }
        Write-Output "no control characters in $scope text files."
        exit 0
    }

    Write-Output "CONTROL CHARACTERS FOUND in $(if ($Staged) {'this commit'} else {'the working tree'}):"
    Write-Output ""
    foreach ($h in $hits) {
        # Render the offenders visibly - the whole failure mode is that they are
        # invisible, so echoing the raw line would show nothing.
        $shown = $h -replace '[\x00-\x08\x0B\x0C\x0E-\x1F]', '<CTRL>'
        Write-Output "  $shown"
    }
    Write-Output ""
    Write-Output "These are almost always a heredoc eating an escape: '\\b' -> BACKSPACE,"
    Write-Output "'\\a' -> BEL, '\\n' -> a real newline. See kb\dev\escapes-die-in-transit.md."
    Write-Output ""
    Write-Output "  fix: build the character from a codepoint, never from an escape --"
    Write-Output "       python:  chr(92) + 'b'      (NOT '\\b')"
    Write-Output "       and READ what the byte replaced before deleting it: a BEL standing"
    Write-Output "       in for '\a' inside a path leaves the path broken once removed."
    exit 1
}
finally { Pop-Location }
