<#
  shoot.ps1 — render a prototype page (or one component of it) to a PNG so an
  agent can LOOK at its own output between revisions.

  Built during C12 (#31) after three revisions of the raised-3D chart were
  argued in prose and rejected on sight. With this, ten rounds ran in one turn
  and eight of the nine defects found had been invisible in the source.

  Usage
    ./shoot.ps1 -Page ..\2026-08-11-visual-styles\index.html -Out r1.png
    ./shoot.ps1 -Page ..\2026-08-11-visual-styles\index.html -Out page.png `
                -Query "style=darkneo&arc=raised&canvas=native" -Width 520 -Height 1180

    # component close-up: -Probe injects JS that replaces the body with one call
    ./shoot.ps1 -Page ..\2026-08-11-visual-styles\index.html -Out donut.png -Width 740 -Height 740 `
      -Probe "S='darkneo';THEME='dark';RAISED=true;EMPTY=true;
              document.body.style.cssText='margin:0;background:#1B1F26';
              document.body.innerHTML='<div class=\"st st-darkneo\" data-theme=\"dark\" id=p
                style=\"width:700px;height:700px;display:grid;place-items:center\"></div>';
              p.innerHTML=donut(660,195,54,'darkneo','',false);"

  Notes learned the hard way:
   * `--headless=new` produced no file here; plain `--headless` works.
   * A stale msedge.exe makes the run exit 0 and write nothing — retry, or kill it.
   * The probe works because top-level `let` in a classic script is visible to a
     later <script> tag, so the page's own functions and state are reachable.
#>
param(
  [Parameter(Mandatory=$true)][string]$Page,
  [Parameter(Mandatory=$true)][string]$Out,
  [string]$Query = "",
  [string]$Probe = "",
  [int]$Width = 900,
  [int]$Height = 900
)

$ErrorActionPreference = "Stop"
$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path $edge)) { throw "Edge not found at $edge" }

$src = (Resolve-Path $Page).Path
$target = $src

if ($Probe -ne "") {
  # write a throwaway copy with the probe appended, so the prototype is untouched.
  # -Encoding UTF8 on the READ is load-bearing: a prototype saved as UTF-8 without a
  # BOM is otherwise read in the ANSI codepage and written back out as UTF-8, which
  # double-encodes every non-ASCII character — every Hebrew label in the probe render
  # came out as mojibake, i.e. the one instrument for judging Hebrew close up could
  # not show Hebrew. Found by looking at a probe render, 2026-08-12.
  $html = Get-Content $src -Raw -Encoding UTF8
  $target = Join-Path ([System.IO.Path]::GetTempPath()) ("probe-" + [guid]::NewGuid().ToString("N") + ".html")
  ($html -replace '</body>', "<script>$Probe</script>`n</body>") | Out-File $target -Encoding utf8
}

$url = "file:///" + ($target -replace '\\','/')
if ($Query -ne "") { $url += "?$Query" }

$outPath = if ([System.IO.Path]::IsPathRooted($Out)) { $Out } else { Join-Path (Get-Location) $Out }
if (Test-Path $outPath) { Remove-Item $outPath -Force }

foreach ($attempt in 1..3) {
  & $edge --headless --disable-gpu --no-sandbox --hide-scrollbars `
          --window-size=$Width,$Height --screenshot="$outPath" $url | Out-Null
  if (Test-Path $outPath) { break }
  Start-Sleep -Seconds 2
}

if ($Probe -ne "") { Remove-Item $target -Force -ErrorAction SilentlyContinue }

if (Test-Path $outPath) { Write-Output $outPath }
else { throw "no screenshot produced - is another msedge.exe running?" }
