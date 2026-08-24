#!/usr/bin/env bash
#
# record-tour.sh — one continuous screen recording of the whole GoalPilot app,
# with a MEASURED beat map, for the explainer video (issue #62).
#
# Why this file exists at all
# ---------------------------
# The first tour (2026-08-22, session `tour-video`) was choreographed in a
# session scratchpad, and the scratchpad died with the session. Re-recording it
# therefore became a whole ticket instead of one command. Everything that
# session learned the hard way is encoded here, in the helpers, so the take
# after this one is `bash scripts/record-tour.sh`.
#
# Usage
# -----
#   bash scripts/record-tour.sh                 record, pull, encode, write the beat map
#   bash scripts/record-tour.sh --dry-run       walk the app and log beats, record nothing
#   bash scripts/record-tour.sh --no-writes     skip every step that creates or moves real data
#   bash scripts/record-tour.sh --post-only     re-encode and regenerate the beat map from
#                                               an existing raw file; needs no device
#
# `--dry-run --no-writes` together are the REHEARSAL: it walks every screen, proves
# every selector still resolves against the shipped build, and leaves the account
# exactly as it found it. Run that before a take rather than discovering on the
# take that a label moved. The three things it skips are the three that write:
# the smart-add sentence, the task added to a goal, and the calendar drag.
#
# Preconditions, all checked by `preflight` rather than assumed:
#   * exactly one device or emulator attached, signed in, with real data on it
#   * the app installed FROM THE CURRENT SOURCE TREE (see `assert_build_is_current`)
#   * ffmpeg and ffprobe reachable (installed 2026-08-22, see CLAUDE.md)
#
# Four traps this script exists in order not to fall into
# ------------------------------------------------------
# 1. THE BACK KEY EXITS THE APP. This AVD reports a hardware keyboard, so Gboard
#    draws a small floating toolbar instead of an IME window. There is no IME for
#    Back to dismiss, so `input keyevent 4` after `input text` is consumed as
#    NAVIGATION, and from a root screen it leaves the app. The 2026-08-22 take
#    lost a dry run to exactly this: the next tap landed on the launcher and
#    opened YouTube, and nine screenshots later the run still reported success.
#    Use `commit_text`. Back is used in this file only from two screens deep and
#    only where the comment says so.
# 2. NEVER LOCATE A CONTROL BY A FIXED SWIPE COUNT. A Compose scroll flings, so
#    the same swipe lands on a different offset run to run. `scroll_to_text`
#    bottoms out with short swipes and stops when the node is actually visible.
#    `Replay tutorial` was missed entirely by a count-based tap on 2026-08-22.
# 3. GIT BASH REWRITES A DEVICE PATH INTO A WINDOWS PATH. `adb shell uiautomator
#    dump /sdcard/ui.xml` silently writes to `/Files/Git/sdcard/ui.xml`, and the
#    pull then reads a stale file, which looks exactly like a UI that did not
#    change. Every device-side path here goes through `sh_` or `out_`, which set
#    MSYS_NO_PATHCONV=1. Observed 2026-08-24, session `62-tour-video-v2`.
# 4. `-vsync 0` IS A HARD ERROR in current ffmpeg, which removed it. It is
#    `-fps_mode passthrough` now, whatever every recipe on the internet says.
# 5. `screenrecord` AT THE NATIVE SIZE FAILS, AND FALLS BACK TO 720x1280 ON ITS
#    OWN. On this AVD (1344 x 2992) it prints
#        ERROR: unable to configure video/avc codec at 1344x2992 (err=-22)
#        WARNING: failed at 1344x2992, retrying at 720x1280
#    and then RECORDS ANYWAY, exit code 0, at a quarter of the pixels. That is
#    the dangerous shape: nothing fails, and what you get back is a marketing
#    film at 720p. Observed 2026-08-24 -- the first take of this session ran two
#    and a half minutes before the log was read.
#    Measured the same day by binary search against the emulator's encoder:
#        1344x2992  ERROR      1280x2848  ERROR      1216x2704  ERROR
#        1152x2560  OK  <-- the ceiling, and the default here
#        1080x2400  OK  (what the 2026-08-22 cut used)
#    So `--size` is passed explicitly and `start_recording` REFUSES to continue
#    if the requested size did not survive. Re-measure on a different AVD; do not
#    assume this number travels.
#
# What is deliberately NOT here: `adb uninstall` and `connectedDebugAndroidTest`,
# both of which take the Firebase sign-in with them. The recording needs a real
# signed-in account on screen and getting it back costs a manual sign-in.

set -uo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

APP_ID="${APP_ID:-com.idomarhaim.goalpilot.debug}"

# The recording size, and it MUST be set explicitly. See trap 5.
REC_SIZE="${REC_SIZE:-1152x2560}"
MAIN_ACTIVITY="${MAIN_ACTIVITY:-com.idomarhaim.goalpilot.MainActivity}"
OUT_DIR="${OUT_DIR:-/c/Users/namei/Videos/GoalPilot-Tour}"
DEVICE_RAW="/sdcard/goalpilot-tour-raw.mp4"
DEVICE_DUMP="/sdcard/window_dump.xml"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAW="$OUT_DIR/GoalPilot-full-tour-raw.mp4"
CFR="$OUT_DIR/GoalPilot-full-tour.mp4"
BEATS_TSV="$OUT_DIR/beats.tsv"
TIMECODES_MD="$REPO_ROOT/docs/marketing/tour-timecodes.md"

ADB="${ADB:-/c/Users/namei/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
FF_DIR="${FF_DIR:-/c/Users/namei/AppData/Local/Programs/ffmpeg/bin}"
FFMPEG="$FF_DIR/ffmpeg"
FFPROBE="$FF_DIR/ffprobe"

TMP="${TMPDIR:-/tmp}/record-tour.$$"
mkdir -p "$TMP"

DRY_RUN=0
POST_ONLY=0
WRITES=1
for arg in "$@"; do
  case "$arg" in
    --dry-run)   DRY_RUN=1 ;;
    --post-only) POST_ONLY=1 ;;
    --no-writes) WRITES=0 ;;
    *) echo "unknown argument: $arg" >&2; exit 2 ;;
  esac
done

T0=""            # wall-clock second at which the recording's first byte was written
BEAT_N=0
REC_PID=""

# ---------------------------------------------------------------------------
# adb plumbing. Every device-side path goes through these, so trap 3 is closed
# in one place instead of at forty call sites.
# ---------------------------------------------------------------------------

sh_()  { MSYS_NO_PATHCONV=1 "$ADB" shell "$@"; }
out_() { MSYS_NO_PATHCONV=1 "$ADB" exec-out "$@"; }

die() { echo "FATAL: $*" >&2; exit 1; }
say() { printf '  %s\n' "$*" >&2; }

# ---------------------------------------------------------------------------
# The beat log, which is the whole point of the rewrite
#
# The 2026-08-22 take was shot BLIND: one continuous screenrecord with no
# per-beat clock, so every timecode in tour-timecodes.md had to be RECONSTRUCTED
# from the choreography's own sleeps plus a measured per-adb-call overhead, then
# scaled so the model landed on the real duration. It came out 3.4 % out before
# scaling, which is respectable and is still a reconstruction.
#
# `beat` removes the exercise: it writes the real elapsed second at the moment
# the thing happens. Accuracy is bounded by how quickly we notice screenrecord's
# first byte (see `start_recording`) rather than by a model of the script.
# ---------------------------------------------------------------------------

beat() {
  local label="$1" now elapsed
  BEAT_N=$((BEAT_N + 1))
  now=$(date +%s.%N)
  if [ -n "$T0" ]; then
    elapsed=$(awk -v a="$now" -v b="$T0" 'BEGIN{printf "%.3f", a-b}')
  else
    elapsed="-1"
  fi
  printf '%d\t%s\t%s\n' "$BEAT_N" "$elapsed" "$label" >> "$BEATS_TSV"
  printf '  [%3d] %9ss  %s\n' "$BEAT_N" "$elapsed" "$label" >&2
}

# ---------------------------------------------------------------------------
# Finding things on screen
#
# The app does NOT set `testTagsAsResourceId`, so Compose test tags are invisible
# to uiautomator: a dump of this app carries exactly one resource-id, and it is
# `android:id/content`. Everything here therefore matches on `text=` or
# `content-desc=`, which is also what a person reading the screen would use.
# ---------------------------------------------------------------------------

# dump_ui refuses to hand back a STALE hierarchy.
#
# `uiautomator` can wedge -- a previous instance that was killed mid-run leaves
# its accessibility service registered, and the next dump dies with
# `IllegalStateException: UiAutomationService ... already registered!`. The dump
# command then writes nothing, `cat` returns the PREVIOUS run's file, and every
# lookup afterwards resolves against a screen that is no longer there. Observed
# 2026-08-24: it read as "the app is stuck on the launcher" for several minutes
# while the app was in fact running normally.
#
# So the device-side file is deleted first: if the dump fails, `cat` returns
# nothing and the caller gets a miss, which is true, instead of a stale hit,
# which is worse than a miss.
# dump_ok tests for a hierarchy with NODES in it, not for a non-empty file.
#
# A wedged `uiautomator` writes a well-formed but EMPTY document -- 56 bytes of
# XML header and a bare root. That is not empty, so `[ -s ]` passes it, and every
# lookup afterwards returns nothing while the run reports no error at all. This
# is the difference between a check that fires and one that does not: measured
# 2026-08-24, a take burned fifty seconds scrolling for a card that was on screen
# the whole time, because the instrument was returning 56 bytes and the guard was
# asking the wrong question.
dump_ok() { [ -s "$TMP/ui.xml" ] && grep -q '<node' "$TMP/ui.xml" 2>/dev/null; }

# kill_uiautomator clears a wedged instance BY PID.
#
# `killall -9 uiautomator` and `pkill -9 -f uiautomator` both return silently
# without killing it on this image; `kill -9 <pid>` works. The wedge itself comes
# from an instance that was terminated mid-run and left its accessibility service
# registered, so the next dump dies with
# `IllegalStateException: UiAutomationService ... already registered!`.
kill_uiautomator() {
  local pid
  for pid in $(sh_ "ps -A -o PID,NAME | grep -i uiautom | awk '{print \$1}'" 2>/dev/null | tr -d '
'); do
    sh_ "kill -9 $pid" >/dev/null 2>&1
  done
  sleep 1.0
}

# dump_ui retries; it does NOT kill anything. That distinction cost a take.
#
# Killing a `uiautomator` instance is precisely what leaves its accessibility
# service registered, so the NEXT dump dies with `UiAutomationService ... already
# registered!`. A recovery built on `kill` is therefore a death spiral: the first
# wedge triggers a kill, the kill guarantees the second wedge, and the run never
# comes back. Observed 2026-08-24 -- take 4 wedged at beat 7, recovered by
# killing, and then failed every dump for the remaining 27 minutes while the app
# sat there working perfectly.
#
# Waiting works, because the service clears on its own. `kill_uiautomator` is
# kept for PREFLIGHT only, where a wedge inherited from a previous run has to be
# broken once before anything starts.
dump_ui() {
  local attempt
  for attempt in 1 2 3; do
    sh_ rm -f "$DEVICE_DUMP" >/dev/null 2>&1
    sh_ uiautomator dump "$DEVICE_DUMP" >/dev/null 2>&1
    out_ cat "$DEVICE_DUMP" > "$TMP/ui.xml" 2>/dev/null
    dump_ok && return 0
    sleep "$attempt"
  done
  say "uiautomator gave no hierarchy after 3 attempts"
  return 1
}

# node_center <attr> <value> prints "X Y" for the first matching node, or nothing.
# Substring matching, because Compose composes a label out of several strings and
# an exact match breaks the moment somebody adds a word to it.
node_center() {
  python - "$1" "$2" "$TMP/ui.xml" "${3:-0}" "${4:-999999}" <<'PY'
import io, re, sys
attr, value, path = sys.argv[1], sys.argv[2], sys.argv[3]
ymin, ymax = int(sys.argv[4]), int(sys.argv[5])
try:
    s = io.open(path, encoding='utf-8', errors='replace').read()
except OSError:
    sys.exit(0)
for node in re.findall(r'<node[^>]*?/?>', s):
    m = re.search(attr + r'="([^"]*)"', node)
    if not m or value.lower() not in m.group(1).lower():
        continue
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    if not b:
        continue
    x1, y1, x2, y2 = map(int, b.groups())
    if x2 <= x1 or y2 <= y1:          # a zero-area node is not tappable
        continue
    cy = (y1 + y2) // 2
    if not (ymin <= cy <= ymax):      # outside the band the caller asked for
        continue
    print((x1 + x2) // 2, cy)
    break
PY
}

# tap_tab restricts the search to the bottom navigation bar.
#
# This exists because `node_center` takes the FIRST match and the tab words are
# not unique on screen: on 2026-08-24 a rehearsal tapped `Goals` and hit the
# friends feed's "avg 24% across 7 goals" at y=1735 instead of the tab at
# y=2848, and every act after it ran on the wrong screen. A substring match over
# a whole hierarchy is only safe when it is also bounded by WHERE the thing is.
tap_tab() {
  local name="$1" h pos
  read -r _ h <<< "$(screen_size)"
  if dump_ui; then
    pos=$(node_center text "$name" $((h * 88 / 100)) "$h")
    if [ -n "$pos" ]; then tap_xy $pos; say "tap tab '$name' at $pos"; return 0; fi
  fi
  say "MISS tab '$name'"; return 1
}

# go_root gets back to a screen that HAS the bottom bar, whatever is open.
#
# Every act after Act 5 failed in the 2026-08-24 rehearsal for one reason: Act 5
# ends on the goal detail screen, which is a full screen with a Back arrow and no
# tabs, and nothing brought the app back. An act that assumes it starts at a root
# is an act that has to be put there.
go_root() {
  local i h pos
  read -r _ h <<< "$(screen_size)"
  for ((i = 0; i < 6; i++)); do
    if dump_ui; then
      pos=$(node_center text "Home" $((h * 88 / 100)) "$h")
      if [ -n "$pos" ]; then
        [ "${1:-}" = "stay" ] || { tap_xy $pos; sleep 1.2; }
        return 0
      fi
    fi
    # Prefer the screen's OWN back affordance. Every non-root screen here draws a
    # top-bar arrow with content-desc "Back", and pressing it is ordinary
    # navigation. KEYCODE_BACK is the fallback and it is a worse instrument: on a
    # screen with a focused text field it is swallowed dismissing the field, and
    # from a root screen it leaves the app entirely (trap 1). On 2026-08-24 six
    # Back presses failed to escape a half-filled add-task form and every act
    # after it ran on the wrong screen.
    if ! tap_desc "Back" 3; then
      sh_ input keyevent KEYCODE_BACK >/dev/null 2>&1
    fi
    sleep 1.2
  done
  say "WARNING: could not get back to a root screen"
  return 1
}

# The avatar is `content-desc="Your account"`, and it opens a SHEET carrying two
# entries: `Your profile` and `Settings`. Analytics, Challenges and Life areas
# all hang off Profile, not off a tab, so this is the only door to three acts.
open_profile()  { go_root; tap_desc "Your account" 8 && sleep 1.6 && tap_text "Your profile" 6 && sleep 2.2; }
open_settings() { go_root; tap_desc "Your account" 8 && sleep 1.6 && tap_text "Settings" 6 && sleep 2.2; }

# wait_for <attr> <value> [timeout_s] prints "X Y" once the node exists.
wait_for() {
  local attr="$1" value="$2" timeout="${3:-12}" deadline pos
  deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -le "$deadline" ]; do
    if dump_ui; then
      pos=$(node_center "$attr" "$value")
      if [ -n "$pos" ]; then printf '%s' "$pos"; return 0; fi
    fi
    sleep 0.4
  done
  return 1
}

tap_xy() { sh_ input tap "$1" "$2" >/dev/null 2>&1; }

# node_center_exact matches the WHOLE label, not a substring.
#
# Substring matching is the right default -- Compose composes labels out of
# several strings and an exact match breaks when somebody adds a word. But it has
# bitten three times in one session, always the same way: a short label that is a
# substring of a longer one on the same screen.
#   `Goals`  matched the friends feed's "avg 24% across 7 goals"
#   `Count`  matched the section header "What does this goal count?"
#   `Soft`   matches "Soft dark"
# Use the exact form for short generic words; keep the substring form for
# sentences and for anything a designer might extend.
node_center_exact() {
  python - "$1" "$2" "$TMP/ui.xml" <<'PY'
import io, re, sys
attr, value, path = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    s = io.open(path, encoding='utf-8', errors='replace').read()
except OSError:
    sys.exit(0)
for node in re.findall(r'<node[^>]*?/?>', s):
    m = re.search(attr + r'="([^"]*)"', node)
    if not m or m.group(1).strip() != value:
        continue
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    if not b:
        continue
    x1, y1, x2, y2 = map(int, b.groups())
    if x2 <= x1 or y2 <= y1:
        continue
    print((x1 + x2) // 2, (y1 + y2) // 2)
    break
PY
}

tap_text_exact() {
  local pos deadline
  deadline=$(( $(date +%s) + ${2:-10} ))
  while [ "$(date +%s)" -le "$deadline" ]; do
    if dump_ui; then
      pos=$(node_center_exact text "$1")
      if [ -n "$pos" ]; then tap_xy $pos; say "tap exact text='$1' at $pos"; return 0; fi
    fi
    sleep 0.4
  done
  say "MISS exact text='$1'"; return 1
}

tap_text() {
  local pos; pos=$(wait_for text "$1" "${2:-12}") || { say "MISS text='$1'"; return 1; }
  tap_xy $pos; say "tap text='$1' at $pos"
}
tap_desc() {
  local pos; pos=$(wait_for content-desc "$1" "${2:-12}") || { say "MISS desc='$1'"; return 1; }
  tap_xy $pos; say "tap desc='$1' at $pos"
}

screen_size() {
  sh_ wm size 2>/dev/null | tr -d '\r' | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1
}

# scroll_to_text is trap 2's remedy: short swipes, re-checking after every one,
# never a count. Returns the node's centre so a caller can tap it, and returns
# non-zero if the text never appeared, which is a finding rather than a crash.
scroll_to_text() {
  local needle="$1" max="${2:-14}" i pos w h y nudged=0
  read -r w h <<< "$(screen_size)"
  for ((i = 0; i < max; i++)); do
    if dump_ui; then
      pos=$(node_center text "$needle")
      if [ -n "$pos" ]; then
        # A needle that has only just appeared is sitting ON the bottom edge, and
        # whatever belongs with it -- the rest of its card, the button under it --
        # is still off screen. Nudge once and re-locate.
        #
        # This is what put `MISS text='Sort'` in the 2026-08-24 take: the smart-add
        # field was found at y=1567 with its Sort button below the fold, so the
        # sentence was typed and never filed. The same shape cost four material
        # cards in the rehearsal before it, where the header appeared and the cards
        # did not. One nudge fixes the whole class.
        y=${pos#* }
        if [ "$nudged" -eq 0 ] && [ "$y" -gt $((h * 70 / 100)) ]; then
          nudged=1
          sh_ input swipe $((w / 2)) $((h * 62 / 100)) $((w / 2)) $((h * 45 / 100)) 320 >/dev/null 2>&1
          sleep 0.6
          dump_ui && pos=$(node_center text "$needle")
          [ -z "$pos" ] && continue
        fi
        printf '%s' "$pos"; return 0
      fi
    fi
    sh_ input swipe $((w / 2)) $((h * 62 / 100)) $((w / 2)) $((h * 38 / 100)) 320 >/dev/null 2>&1
    sleep 0.55
  done
  return 1
}

# scroll_to_top exists because scroll_to_text only ever swipes ONE WAY.
#
# The dashboard draws `Filed nowhere` ABOVE the smart-add card, so once Act 2 has
# scrolled down to type, no amount of further scrolling will ever reach it -- and
# the run reports `no Filed nowhere card on screen`, which reads as "the account
# has none" rather than "you are looking the wrong way". Observed 2026-08-24: the
# #67 beat was silently dropped from a whole take that way.
scroll_to_top() {
  local i w h
  read -r w h <<< "$(screen_size)"
  for ((i = 0; i < "${1:-6}"; i++)); do
    sh_ input swipe $((w / 2)) $((h * 35 / 100)) $((w / 2)) $((h * 70 / 100)) 300 >/dev/null 2>&1
    sleep 0.45
  done
}
tap_after_scroll() {
  local pos; pos=$(scroll_to_text "$1" "${2:-14}") || { say "MISS after scroll: '$1'"; return 1; }
  tap_xy $pos; say "tap (scrolled) text='$1' at $pos"
}

swipe_up() {
  local w h; read -r w h <<< "$(screen_size)"
  sh_ input swipe $((w/2)) $((h*62/100)) $((w/2)) $((h*38/100)) "${1:-320}" >/dev/null 2>&1
}

# ---------------------------------------------------------------------------
# Typing, and the one thing you must not do afterwards
# ---------------------------------------------------------------------------

type_text() {
  # `input text` wants %s for a space and chokes on shell metacharacters.
  local escaped; escaped=$(printf '%s' "$1" | sed 's/ /%s/g')
  sh_ input text "$escaped" >/dev/null 2>&1
  say "type '$1'"
}

# commit_text is the ONLY sanctioned way to finish typing. See trap 1.
#
# Gboard's floating toolbar carries a tick that commits. It is located by
# content-desc where the toolbar exposes one; the fallback is the position
# measured on 2026-08-22 (107, 1436 on a 1344 x 2992 screen), written as a
# FRACTION so it survives a different geometry. `input keyevent 4` is never an
# option here, and this function exists so that nobody reaches for it.
commit_text() {
  local pos w h d
  if dump_ui; then
    for d in "Insert" "commit" "done" "Enter"; do
      pos=$(node_center content-desc "$d")
      if [ -n "$pos" ]; then tap_xy $pos; say "commit via desc='$d' at $pos"; return 0; fi
    done
  fi
  read -r w h <<< "$(screen_size)"
  tap_xy $(( w * 107 / 1344 )) $(( h * 1436 / 2992 ))
  say "commit via the measured fallback position"
}

# ---------------------------------------------------------------------------
# Preflight: everything that would waste a whole take if it were wrong
# ---------------------------------------------------------------------------

preflight() {
  command -v python >/dev/null || die "python is needed for the hierarchy parser"
  [ -x "$ADB" ] || die "adb not found at $ADB"

  local n; n=$("$ADB" devices | tr -d '\r' | grep -cE '[[:space:]]device$')
  [ "$n" -eq 1 ] || die "expected exactly one attached device, found $n"

  sh_ pm list packages 2>/dev/null | grep -q "$APP_ID" || die "$APP_ID is not installed"

  local w h; read -r w h <<< "$(screen_size)"
  [ -n "${w:-}" ] || die "could not read the screen size"
  say "device screen ${w}x${h}"
  # A device left at another session's geometry is a trap: the S25 reproduction
  # of 2026-08-24 forced 1080x2340 and had to reset it afterwards. Say what was
  # found rather than silently shooting at somebody else's size.
  say "if that is not this AVD's native size: adb shell wm size reset; adb shell wm density reset"

  [ -x "$FFMPEG" ]  || say "WARNING: no ffmpeg at $FFMPEG, the CFR encode will be skipped"
  [ -x "$FFPROBE" ] || say "WARNING: no ffprobe at $FFPROBE, the beat map will lack a duration"

  # Prove the INSTRUMENT works before spending a take on it, on the hardest
  # thing it has to do -- a real hierarchy from the app, not a bare `adb devices`.
  kill_uiautomator
  if dump_ui; then
    # Bytes, not a node count: uiautomator writes the whole hierarchy on ONE
    # line, so `grep -c '<node'` always reports 1 and reads like a broken dump.
    say "uiautomator returns a live hierarchy ($(wc -c < "$TMP/ui.xml") bytes)"
  else
    die "uiautomator will not produce a hierarchy. Nothing downstream can find a control; fix this before recording."
  fi

  mkdir -p "$OUT_DIR"
}

# assert_build_is_current is the check that saves the take rather than the build.
#
# The failure it catches is real and recent. On 2026-08-24 the emulator carried a
# debug APK built five minutes before two dashboard files were last written, so
# it was the shipped app in every visible respect EXCEPT the screen the tour
# opens on. Nothing on screen says which build you are looking at, because the
# app never renders BuildConfig.VERSION_NAME anywhere, so this cannot be
# eyeballed and has to be a file-time comparison.
assert_build_is_current() {
  local apk="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk" newer
  [ -f "$apk" ] || { say "no debug APK has been built yet; run :app:assembleDebug"; return 1; }
  newer=$(find "$REPO_ROOT/app/src/main" -newer "$apk" -type f \( -name '*.kt' -o -name '*.xml' \) | head -5)
  if [ -n "$newer" ]; then
    say "SOURCE IS NEWER THAN THE BUILT APK:"
    printf '    %s\n' $newer >&2
    return 1
  fi
  say "the built APK is newer than every source file under app/src/main"
}

# ---------------------------------------------------------------------------
# Recording
# ---------------------------------------------------------------------------

clean_status_bar() {
  # SystemUI demo mode: a fixed 9:00 clock, full wifi, full battery, no
  # notification icons. Without it the status bar dates the video and clutters
  # every single frame of it.
  sh_ settings put global sysui_demo_allowed 1 >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command enter >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900 >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100 >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4 >/dev/null 2>&1
  sh_ am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null 2>&1
  say "status bar in demo mode"
}
restore_status_bar() {
  sh_ am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1
  say "status bar restored"
}

start_recording() {
  if [ "$DRY_RUN" -eq 1 ]; then T0=$(date +%s.%N); say "dry run, nothing is being recorded"; return 0; fi
  sh_ rm -f "$DEVICE_RAW" >/dev/null 2>&1
  # --time-limit 0 means no limit: one continuous take, so nothing is stitched.
  MSYS_NO_PATHCONV=1 "$ADB" shell screenrecord --time-limit 0 --size "$REC_SIZE"       --bit-rate 12000000 "$DEVICE_RAW" > "$TMP/screenrecord.log" 2>&1 &
  REC_PID=$!
  # T0 is pinned the moment the output file first has bytes in it, so the beat
  # clock and the video clock share an origin to within one poll (50 ms) rather
  # than to within a guess. That is the whole difference between a measured beat
  # map and a reconstructed one.
  local i sz
  for ((i = 0; i < 200; i++)); do
    sz=$(sh_ "stat -c %s $DEVICE_RAW 2>/dev/null || echo 0" | tr -d '\r')
    case "$sz" in ''|*[!0-9]*) sz=0 ;; esac
    [ "$sz" -gt 0 ] && break
    sleep 0.05
  done
  T0=$(date +%s.%N)
  # Trap 5: screenrecord downgrades silently and still exits 0. Read its own
  # stderr and stop, rather than discovering the resolution in the finished file.
  if grep -qi 'retrying at' "$TMP/screenrecord.log" 2>/dev/null; then
    sh_ killall -2 screenrecord >/dev/null 2>&1
    say "$(tr -d '
' < "$TMP/screenrecord.log" | tr '
' ' ')"
    die "screenrecord refused $REC_SIZE and silently downgraded. Set REC_SIZE to a size this encoder accepts and re-run."
  fi
  say "recording started (pid $REC_PID) at $REC_SIZE, T0 pinned after $i polls"
}

stop_recording() {
  [ "$DRY_RUN" -eq 1 ] && return 0
  # SIGINT, so screenrecord finalises the MP4 container instead of leaving a
  # truncated one. killall is the form that exists on Android; pkill is the
  # fallback for images that carry it instead.
  sh_ killall -2 screenrecord >/dev/null 2>&1 || sh_ pkill -2 screenrecord >/dev/null 2>&1
  sleep 3
  wait "$REC_PID" 2>/dev/null
  # `adb pull` on /sdcard can fail where `exec-out cat` succeeds -- the device
  # shell's view of scoped storage and adb's do not always agree, and after a
  # reboot they disagreed here while the dump written to the same directory read
  # back fine. So: try pull, fall back to a byte-for-byte cat, and NEVER abort --
  # by this point the take has already happened, and losing it to a transfer is
  # the one failure with nothing to salvage.
  if ! MSYS_NO_PATHCONV=1 "$ADB" pull "$DEVICE_RAW" "$RAW" >/dev/null 2>&1; then
    say "adb pull failed; falling back to exec-out cat"
    out_ cat "$DEVICE_RAW" > "$RAW" 2>/dev/null
  fi
  if [ ! -s "$RAW" ]; then
    say "COULD NOT RETRIEVE THE RECORDING. It may still be on the device at $DEVICE_RAW"
    sh_ "ls -l $DEVICE_RAW" >&2 2>&1
    return 1
  fi
  say "pulled $(du -h "$RAW" | cut -f1) to $RAW"
}

# ---------------------------------------------------------------------------
# THE CHOREOGRAPHY
#
# One function per act, and the act boundaries are the narration's act
# boundaries in docs/marketing/explainer-video-brief.md section 3, so a beat map
# row lands under the line that describes it without anyone scrubbing a video.
#
# Every `sleep` is a HOLD: how long the viewer looks at that screen. They are the
# only numbers in this file a human should tune.
#
# Every optional beat is guarded by an `if`, and a miss says so rather than
# aborting: a tour that loses one card is worth more than no tour at all, and the
# log names exactly which beat was lost.
# ---------------------------------------------------------------------------

HOLD_SHORT=2.0
HOLD=3.0
HOLD_LONG=4.5

# The two things the choreography types, and the goal it opens. Hoisted here so a
# re-take can change them without going hunting through the acts, and so that the
# smart-add sentence and the task both name something that exists on the account
# being shot.
SMART_ADD_SENTENCE="${SMART_ADD_SENTENCE:-Practice saxophone for twenty minutes on Sunday}"
GOAL_TO_OPEN="${GOAL_TO_OPEN:-saxophone}"
TASK_TO_ADD="${TASK_TO_ADD:-Long tones and scales}"

# writes_ok gates the three steps that change the account: the smart-add
# sentence, the task added to a goal, and the calendar drag. A rehearsal
# (--no-writes) walks past them, logs a `skipped` beat so the map still shows
# where they would have been, and leaves the data alone. This exists because a
# rehearsal that creates two junk tasks and reschedules a real calendar block is
# a rehearsal that costs a cleanup pass, and the cleanup is a deletion.
writes_ok() {
  if [ "$WRITES" -eq 1 ]; then return 0; fi
  say "SKIPPED (--no-writes): $1"
  beat "skipped in rehearsal: $1"
  return 1
}

act1_home() {
  say "--- Act 1: the home screen"
  sh_ am force-stop "$APP_ID" >/dev/null 2>&1
  sh_ input keyevent KEYCODE_HOME >/dev/null 2>&1
  sleep 1.5;                     beat "launcher"
  # `am start -W` rather than `monkey`: it names the component, waits for the
  # activity to be displayed, and reports a status. `monkey` immediately after a
  # `force-stop` was observed on 2026-08-24 to inject its event and leave the
  # launcher in front, with nothing in its output saying so.
  sh_ am start -W -n "$APP_ID/$MAIN_ACTIVITY" >/dev/null 2>&1
  sleep 2.0;                     beat "splash"
  sleep 5.0;                     beat "dashboard lands"
  sleep "$HOLD";                 beat "points and level"
  sleep "$HOLD";                 beat "overall progress, averaged across all your goals"
}

act2_smart_add() {
  say "--- Act 2: smart add, the headline feature"
  tap_after_scroll "e.g. Run 5 km" 8 || tap_after_scroll "Smart add a task" 8
  sleep 1.2;                     beat "the smart-add field, focused"
  writes_ok "the smart-add sentence" || { sleep "$HOLD"; return 0; }
  type_text "$SMART_ADD_SENTENCE"
  sleep 1.0;                     beat "a sentence, in plain words"
  commit_text
  sleep 0.8;                     beat "keyboard away"
  tap_after_scroll "Sort" 6
  sleep 1.0;                     beat "the AI reads it"
  sleep 5.0;                     beat "filed under the right goal, with points and an estimate"
  sleep "$HOLD";                 beat "nothing is saved until you have seen where it went"
}

act3_rest_of_home() {
  say "--- Act 3: the rest of the home screen"
  # #67 shipped `Filed nowhere`: the card that surfaces a task filed under no
  # goal and no date, which until that ticket no screen could list and nothing
  # could delete. It is the only place in the app where a delete IS the point of
  # the card, so the confirmation sheet is the shot worth having.
  # Back to the top first: `Filed nowhere` is drawn ABOVE the smart-add card that
  # Act 2 just scrolled down to, and scroll_to_text only searches downward.
  scroll_to_top 6
  if scroll_to_text "Filed nowhere" 4 >/dev/null; then
    beat "Filed nowhere: the tasks no other screen could list"
    sleep "$HOLD"
    if tap_desc "Delete " 5; then
      sleep 1.5;                 beat "what goes, and what stays, asked separately"
      sleep "$HOLD_LONG"
      # DELIBERATELY CANCELLED. This card holds real data, and a deletion is
      # always-ask whether or not a script is the thing doing it.
      tap_text "Cancel" 5;       beat "cancelled: nothing was deleted"
      sleep 1.0
    fi
  else
    say "no 'Filed nowhere' card on screen, skipping the #67 beat"
  fi
  swipe_up; sleep 1.2;           beat "the coach answers with something you could do today"
  sleep "$HOLD_LONG"
  swipe_up; sleep 1.2;           beat "your goals, on the home screen"
  sleep "$HOLD"
  if scroll_to_text "Share your weekly progress" 6 >/dev/null; then
    beat "share this week's progress"
    sleep "$HOLD_SHORT"
  fi
}

act4_goals() {
  say "--- Act 4: goals, grouped by life area"
  go_root stay
  tap_tab "Goals"
  sleep 2.2;                     beat "the goals tab, grouped by life area"
  sleep "$HOLD_LONG"
  # #65 and #66 are one story on screen: a goal with no measure now says
  # `No number` where a percentage used to be, and states the entries it has
  # instead of inventing a fraction. They are shot together for that reason.
  if scroll_to_text "No number" 8 >/dev/null; then
    beat "No number: a goal without a measure says so instead of printing 0 per cent"
    sleep "$HOLD_LONG"
  fi
  swipe_up; sleep 1.0;           beat "more goals, more areas"
  sleep "$HOLD"
}

act5_one_goal() {
  say "--- Act 5: one goal, and how work actually gets scheduled"
  tap_text "$GOAL_TO_OPEN" 8 || tap_text "Submit" 8
  sleep 2.4;                     beat "goal detail: the measure, the progress, the life area, the tasks"
  sleep "$HOLD_LONG"
  # #65's offer card. The app proposes a measure for a goal that has none rather
  # than leaving its owner to invent one, and the proposal names the word it
  # would count in.
  if scroll_to_text "Measure it in" 8 >/dev/null; then
    beat "the app offers a measure rather than leaving the goal blank"
    sleep "$HOLD_LONG"
  fi
  if writes_ok "adding a task to this goal"; then
    # Back to the top first. The `Measure it in` hunt above scrolls to the bottom
    # of the goal screen, and the add-task row is at the TOP of it -- so on
    # 2026-08-24 this missed, and with it went the AI estimate, the scheduling
    # picker and the submit. Same shape as Act 3's `Filed nowhere`: a
    # downward-only search cannot find what it has already gone past.
    scroll_to_top 6
    tap_after_scroll "Add a task" 6
    sleep 1.2;                   beat "the task field"
    type_text "$TASK_TO_ADD"
    commit_text
    sleep 0.8;                   beat "a task, typed"
    # These three are ICON BUTTONS, so their only handle is content-desc --
    # `tap_text` can never find them, and on 2026-08-24 it put four MISSes in a
    # take and left the form half-filled on screen for the rest of the run.
    # Measured off the live hierarchy rather than read off the source:
    #   desc "Estimate difficulty and duration with AI"   the AI estimate
    #   text "When?"                                      the scheduling picker
    #   desc "Add task"                                   the submit
    if tap_desc "Estimate difficulty and duration with AI" 8; then
      sleep 1.2;                 beat "the AI estimates it"
      sleep 4.5;                 beat "how demanding, how long, and what it is worth"
    fi
    # The scheduling sub-beats are deliberately each guarded and each optional.
    # They are the deepest part of the choreography -- a picker inside a row
    # inside a scrolling screen -- and losing one of them costs a beat, where
    # letting a miss abort the act costs the whole second half of the film.
    if tap_text "When?" 8; then
      sleep 1.8;                 beat "the date picker"
      sleep "$HOLD"
      if tap_text "Add a time" 5; then
        sleep 1.6;               beat "and an hour, which promotes it to a deadline"
        sleep "$HOLD"
      fi
      if tap_text "Set" 5; then
        sleep 1.4;               beat "late and still owed is a different thing from the day passed"
      elif tap_text "Cancel" 4; then
        sleep 1.0
      fi
    fi
    tap_desc "Add task" 8
    sleep 2.2;                   beat "the task, added, with its points"
    sleep "$HOLD"
  else
    sleep "$HOLD"
  fi
  # Act 5 ends on a full screen with no bottom bar. Everything after it assumes a
  # root, so it is put back here rather than in each of the seven acts that
  # follow. In the 2026-08-24 rehearsal this single missing line cost every beat
  # from Act 6 to the end of the run.
  go_root
}

act6_calendar() {
  say "--- Act 6: the calendar surface (#60) and drag to move (#68)"
  tap_tab "Calendar"
  sleep 2.4;                     beat "the calendar: your goals, as time"
  sleep "$HOLD_LONG"
  # The load bar reads `free` on an empty day and fills as hours are promised
  # away. Either word is a legitimate shot; which one appears depends on the
  # account, so the beat names what is actually on screen rather than asserting.
  if dump_ui && [ -n "$(node_center text "booked")" ]; then
    beat "the load bar: how full the day already is, before you add to it"
    sleep "$HOLD"
  elif dump_ui && [ -n "$(node_center text "free")" ]; then
    beat "the load bar, on a day with room left in it"
    sleep "$HOLD"
  fi
  beat "the all-day strip: what is due today with no hour on it"
  sleep "$HOLD_SHORT"
  # The calendar's own controls carry no visible label -- they are icon buttons,
  # so they are addressed by content-desc: New, Next, Previous, Today.
  if tap_desc "Next" 6;  then sleep 1.6; beat "forward three days"; sleep "$HOLD_SHORT"; fi
  if tap_desc "Today" 6; then sleep 1.6; beat "back to today"; fi
  if tap_text "Week" 5;  then sleep 1.8; beat "the whole week at once"; sleep "$HOLD"; fi
  if tap_text "3 days" 5; then sleep 1.6; beat "back to three days"; fi
  if writes_ok "the drag-to-move gesture (it reschedules a real block)"; then
    # A long press picks a block up; the drag moves it. The 1400 ms swipe
    # duration is what makes the press long enough for
    # detectDragGesturesAfterLongPress to claim the gesture at all -- a normal
    # 300 ms swipe is read as a scroll and nothing is picked up.
    local w h; read -r w h <<< "$(screen_size)"
    sh_ input swipe $((w / 2)) $((h * 55 / 100)) $((w / 2)) $((h * 43 / 100)) 1400 >/dev/null 2>&1
    sleep 1.8;                   beat "press and hold a block to pick it up, then drag it where it should have been"
    sleep "$HOLD_LONG"
    # The scope sheet appears ONLY where a recurrence rule exists: this one, or
    # this and every one after it. Where the moved block is a one-off there is no
    # sheet, and nothing is missing when this beat does not fire.
    if dump_ui && [ -n "$(node_center text "Move")" ]; then
      beat "and when the thing repeats, it asks which of them you meant"
      sleep "$HOLD_LONG"
      tap_text "Cancel" 5
    fi
  fi
  sleep 1.0
}

act7_life_areas_and_run() {
  say "--- Act 7: life areas, and C19's success and failure run (#64)"
  open_profile
  sleep 1.4;                     beat "the avatar opens your profile"
  tap_text "Life areas" 8
  sleep 2.2;                     beat "life areas: the parts of your life you decided are worth investing in"
  sleep "$HOLD_LONG"
  # The area names are the user's own and may be in any language, so the first
  # area row is found by the subtitle every row carries rather than by a name
  # this script cannot know.
  local pos
  if dump_ui; then
    pos=$(node_center text "goals ·")
    [ -z "$pos" ] && pos=$(node_center text "goal ·")
    if [ -n "$pos" ]; then tap_xy $pos; say "open first life area at $pos"; fi
  fi
  sleep 2.4;                     beat "one life area"
  # #64 draws the run by FORM (filled, hollow, dashed with a pip, dotted, a
  # dashed ring with a plus) and never by hue. There is no red in it and no ratio
  # anywhere. If the narration calls this a score or a rate, the narration is
  # wrong, and that is a correction to make in the script rather than here.
  if scroll_to_text "Window by window" 10 >/dev/null; then
    if dump_ui && [ -n "$(node_center text "Nothing has been due here yet")" ]; then
      # The empty state is honest and is also not a shot. Say so in the log so
      # nobody scrubs the video looking for a run that was never on screen.
      beat "the run, in its EMPTY state -- nothing has been due in this area yet"
      say "NOTE: #64's run has no windows on this account; the beat is its empty state"
    else
      beat "kept, missed, still owed: window by window, and not a score"
      sleep "$HOLD_LONG"
    fi
    sleep "$HOLD"
  fi
  go_root
}

act8_analytics() {
  say "--- Act 8: analytics"
  open_profile
  tap_text "Analytics" 8
  sleep 2.6;                     beat "where your time goes"
  sleep "$HOLD_LONG"
  # Year, not Week or Month.
  #
  # Measured 2026-08-24 on this account: Week says `Nothing completed in this
  # week yet`, and Month is empty for the same reason, while Year carries
  # 67 per cent / 20 per cent / 13 per cent over 3h 45m -- which is the exact
  # split the narration in section 3 Act 9 describes. Shooting Month would put an
  # empty donut under a line about two thirds of a year.
  if tap_text "Year" 6; then
    sleep 2.4;                   beat "by year: the share of a life, per area"
    sleep "$HOLD_LONG"
    beat "and the thirteen per cent that was never filed at all"
    sleep "$HOLD"
  fi
  # #59's repair. The previous cut had `Strength Training` reading 245613 per
  # cent on screen at 1:23, which on its own made it unusable as marketing.
  # #66 is the same frame's other half: a goal with no measure is left OUT of the
  # ranking and the chart says so, rather than being charted as a zero.
  if scroll_to_text "not charted here" 8 >/dev/null; then
    beat "goals with no number are named, not charted as zero"
    sleep "$HOLD"
  fi
  swipe_up; sleep 1.2;           beat "how it moves"
  sleep "$HOLD"
  go_root
}

act9_social() {
  say "--- Act 9: social and challenges"
  tap_tab "Social"
  sleep 2.4;                     beat "PRIVACY -- the leaderboard: a friend's real name is on screen from here"
  sleep "$HOLD_LONG"
  if tap_text "Everyone" 5; then sleep 1.8; beat "and everyone, not only friends"; sleep "$HOLD_SHORT"; fi
  if tap_text "Friends" 5;  then sleep 1.6; fi
  swipe_up; sleep 1.2;           beat "the friends feed, with a photo if the week deserved one"
  sleep "$HOLD_LONG"
  if tap_text "Challenges" 8; then
    sleep 2.2;                   beat "challenges"
    sleep "$HOLD_LONG"
  fi
  go_root
}

act10_profile() {
  say "--- Act 10: profile and the friend code"
  open_profile
  sleep 1.6;                     beat "PRIVACY -- profile: the email address and the friend code are legible from here"
  sleep "$HOLD_LONG"
  beat "PRIVACY ENDS -- everything after this beat is safe to publish"
  go_root
}

act11_settings() {
  say "--- Act 11: settings"
  open_settings
  sleep 1.6;                     beat "settings, and Help is the first thing in it now"
  sleep "$HOLD"
  # The two sync cards moved Home -> Settings on 2026-08-24 (s25-layout-and-tour,
  # Ido's own placement call), so this is where Google Tasks and Health Connect
  # are shot now. Act 3 of the narration was written when they were on Home and
  # has been rewritten for this.
  if scroll_to_text "Connected apps" 8 >/dev/null; then
    beat "connected apps: Google Tasks and Health Connect live here now"
    sleep "$HOLD_LONG"
  fi
  # Anchor on the Glass card's TAGLINE, not on the `Material` section header.
  #
  # `scroll_to_text` stops the instant its needle is visible, and `Material` is a
  # header that becomes visible while all four cards are still below the fold --
  # so anchoring on it scrolled to a screen where every card tap missed. Measured
  # 2026-08-24: anchoring on `Material` put four MISSes in the rehearsal log;
  # anchoring on `Frosted panels` puts all four cards on screen. The taglines are
  # also the only unambiguous handles here, because `Soft` is a substring of
  # `Soft dark`.
  if scroll_to_text "Frosted panels" 10 >/dev/null; then
    beat "four materials"
    sleep "$HOLD_SHORT"
    for pair in "Frosted panels|Glass" "Glossy, lit|Liquid glass" "One flat surface|Soft" "Charcoal, with one|Soft dark"; do
      if tap_text "${pair%%|*}" 5; then sleep 2.6; beat "material: ${pair##*|}"; fi
    done
  fi
  if scroll_to_text "Background" 8 >/dev/null; then beat "background treatments"; sleep "$HOLD"; fi
  if scroll_to_text "Language" 8 >/dev/null;   then beat "language and region"; sleep "$HOLD_SHORT"; fi
  if scroll_to_text "Your day" 8 >/dev/null;   then
    beat "your waking hours, so it knows when your day is genuinely full"
    sleep "$HOLD"
  fi
}

act12_the_tour() {
  say "--- Act 12: the app's own guided tour (TUTORIAL_VERSION 3)"
  # Reached from inside Settings, where Act 11 left us. Help is the FIRST section
  # now, so this scrolls UP rather than down -- and scroll_to_text only swipes
  # one way, which is why the screen is re-entered from the top instead.
  open_settings
  tap_after_scroll "Replay tutorial" 16
  sleep 2.6;                     beat "tour step 1 of 7: welcome to GoalPilot"
  tap_text "Start" 6
  sleep 2.2;                     beat "step 2: your progress, at a glance"
  tap_text "Next" 6; sleep 2.2;  beat "step 3: add anything, instantly"
  tap_text "Next" 6; sleep 2.2;  beat "step 4: everything you are working on, and it asks you to tap Goals"
  tap_tab "Goals"
  sleep 2.4;                     beat "the tour follows you to the tab you pressed"
  tap_text "Next" 6; sleep 2.2;  beat "step 5: every goal starts here"
  tap_text "Next" 6; sleep 2.2;  beat "step 6: your goals, as time"
  # The step-6 hole became LIVE on 2026-08-24: tapping the ringed Calendar tab
  # opens the Calendar and the tour WAITS there with its card on screen, instead
  # of steering straight back to Home. Before that fix the spotlight ringed a
  # control and then ate the press it had invited. This is the one beat where the
  # tour demonstrates itself rather than describing itself, and it is most of the
  # reason this act is worth re-shooting at all.
  tap_tab "Calendar"
  sleep 3.0;                     beat "the ringed tab actually opens, and the tour stays put while you look at it"
  sleep "$HOLD"
  tap_text "Next" 6; sleep 2.4;  beat "step 7: your profile, and this tour"
  tap_text "Done" 6 || tap_text "Finish" 6
  sleep 2.5;                     beat "tour done, home again"
  sleep "$HOLD_LONG";            beat "END"
}

# ---------------------------------------------------------------------------
# Post-production
# ---------------------------------------------------------------------------

# archive_previous keeps the cut that is already there.
#
# The Exit criterion names `GoalPilot-full-tour.mp4` as the destination, so a new
# take writes over the old one -- and the brief ALSO lists the previous cut under
# "what you inherit". Both are true, and on 2026-08-24 the second lost: the
# 2026-08-22 cut and its raw were overwritten before this function existed. They
# are stamped with their own mtime and kept now, because a video is not in git
# and there is no other copy of it anywhere.
archive_previous() {
  local f stamp
  for f in "$CFR" "$RAW"; do
    [ -s "$f" ] || continue
    stamp=$(date -r "$f" +%Y%m%d-%H%M 2>/dev/null || echo prev)
    mv "$f" "${f%.mp4}-$stamp.mp4" && say "archived $(basename "$f") as $(basename "${f%.mp4}-$stamp.mp4")"
  done
}

encode_cfr() {
  [ -x "$FFMPEG" ] || { say "no ffmpeg, skipping the CFR encode"; return 1; }
  [ -s "$RAW" ]    || { say "no raw file at $RAW"; return 1; }
  # screenrecord output is VARIABLE frame rate: it emits a frame only when the
  # screen changes, so a 400 s take can hold 1,676 frames. Most editors and
  # essentially every AI video tool mishandle that, so the deliverable is CFR.
  "$FFMPEG" -y -i "$RAW" -fps_mode cfr -r 30 -c:v libx264 -preset slow -crf 20 \
            -pix_fmt yuv420p -movflags +faststart "$CFR" 2>"$TMP/ffmpeg.log"
  if [ $? -ne 0 ]; then
    say "ffmpeg failed, see $TMP/ffmpeg.log"; tail -5 "$TMP/ffmpeg.log" >&2; return 1
  fi
  say "encoded $(du -h "$CFR" | cut -f1) to $CFR"
}

probe() {
  [ -x "$FFPROBE" ] || return 1
  "$FFPROBE" -v error -select_streams v:0 \
    -show_entries stream=width,height,nb_frames:format=duration \
    -of default=nw=1 "$1" 2>/dev/null
}

write_timecodes() {
  local dur frames raw_frames w h
  dur=$(probe "$CFR" | sed -n 's/^duration=//p')
  frames=$(probe "$CFR" | sed -n 's/^nb_frames=//p')
  w=$(probe "$CFR" | sed -n 's/^width=//p')
  h=$(probe "$CFR" | sed -n 's/^height=//p')
  raw_frames=$(probe "$RAW" | sed -n 's/^nb_frames=//p')
  python - "$BEATS_TSV" "$TIMECODES_MD" "${dur:-0}" "${frames:-0}" "${raw_frames:-0}" \
           "${w:-0}" "${h:-0}" <<'PY'
import io, sys, datetime
tsv, out, dur, frames, raw_frames, w, h = sys.argv[1:8]
rows = []
for line in io.open(tsv, encoding='utf-8'):
    parts = line.rstrip('\n').split('\t')
    if len(parts) == 3:
        rows.append((int(parts[0]), float(parts[1]), parts[2]))
def tc(s):
    return '%d:%04.1f' % (int(s // 60), s % 60)
L = []
A = L.append
A('<!-- Written %s by session `62-tour-video-v2`. GENERATED BY scripts/record-tour.sh -->'
  % datetime.date.today().isoformat())
A('<!-- Every timecode here is MEASURED at the moment the beat happened, not reconstructed. -->')
A('')
A('# `GoalPilot-full-tour.mp4` -- beat timecodes')
A('')
A('Every beat in the recording and the second it happens, so a narration line from')
A('[`explainer-video-brief.md`](explainer-video-brief.md) can be placed without scrubbing.')
A('')
A('| | |')
A('|---|---|')
A('| **Duration** | `%s` (%s s) |' % (tc(float(dur or 0)), dur))
A('| **Picture** | %s x %s, H.264, **constant 30 fps**, %s frames |' % (w, h, frames))
A('| **Audio** | none -- the recording is silent by design; the narration is added on the timeline |')
A('| **Source** | `GoalPilot-full-tour-raw.mp4` beside it is the untouched `screenrecord` output, '
  '**variable frame rate** (%s frames over the same duration). Use the CFR file. |' % raw_frames)
A('')
A('## How these numbers were produced')
A('')
A('`scripts/record-tour.sh` logs a wall clock reading inside its own `beat` function, against a')
A('`T0` pinned the moment `screenrecord`\'s output file first had bytes in it. Each row below is')
A('therefore an **observed** elapsed second, accurate to the polling interval (50 ms) plus one')
A('frame -- not a model of the choreography scaled to fit the duration, which is what the')
A('2026-08-22 map was, and what it cost that session a section of its changelog to admit.')
A('')
A('`Untested:` the residual offset between `screenrecord`\'s own frame zero and the first byte')
A('reaching the file. It is bounded by one frame at the capture rate and has not been measured')
A('directly. Spot-check three rows by extracting that exact frame and looking at it before')
A('cutting on a single frame.')
A('')
A('| # | timecode | on-screen beat |')
A('|---|---|---|')
for n, t, label in rows:
    A('| %d | `%s` | %s |' % (n, tc(t), label))
A('')
privacy = [(n, t, l) for n, t, l in rows if l.startswith('PRIVACY')]
if privacy:
    A('## The beats to look at before this is published anywhere')
    A('')
    A('The recording is of a real account, and these rows are where that shows:')
    A('')
    for n, t, l in privacy:
        A('- **`%s`** -- %s' % (tc(t), l.split('-- ', 1)[-1]))
    A('')
    A('Fine for a course submission. For anything public, trim or blur those ranges: one pass in')
    A('any editor, and these are the timecodes for it.')
io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(L) + '\n')
sys.stderr.write('wrote %s with %d beats\n' % (out, len(rows)))
PY
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

main() {
  cd "$REPO_ROOT"

  if [ "$POST_ONLY" -eq 1 ]; then
    encode_cfr; write_timecodes; exit 0
  fi

  preflight
  assert_build_is_current || say "CONTINUING ANYWAY, but the recording will not be of the shipped app"

  mkdir -p "$OUT_DIR"
  : > "$BEATS_TSV"
  clean_status_bar
  trap 'restore_status_bar; rm -rf "$TMP"' EXIT

  archive_previous
  start_recording
  act1_home
  act2_smart_add
  act3_rest_of_home
  act4_goals
  act5_one_goal
  act6_calendar
  act7_life_areas_and_run
  act8_analytics
  act9_social
  act10_profile
  act11_settings
  act12_the_tour
  stop_recording

  if [ "$DRY_RUN" -eq 0 ]; then
    encode_cfr && write_timecodes
  fi
  say "done: $BEAT_N beats"
}

# Guard so the helpers can be sourced by a probe or a one-off repair script
# without the whole choreography running. `bash scripts/record-tour.sh` still
# does exactly what it did.
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
  main "$@"
fi
