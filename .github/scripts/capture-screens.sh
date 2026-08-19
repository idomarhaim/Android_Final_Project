#!/usr/bin/env bash
# Install GoalPilot on the cloud emulator, launch it, and photograph what a person
# would see. Called by .github/workflows/instrumented-tests.yml from inside
# reactivecircus/android-emulator-runner, so a booted emulator is already on adb.
#
# WHY A SCRIPT AND NOT INLINE YAML: the shots have to be spaced in time (splash ->
# auth gate -> settled), and a multi-step shell block inside a workflow `script:`
# key is where quoting goes to die.
#
# This is deliberately NOT a test. It proves nothing and asserts nothing; it exists
# so that a developer with no RAM for a local AVD can still LOOK at the app. The
# assertions live in app/src/androidTest/.
set -uo pipefail

PKG=com.idomarhaim.goalpilot.debug
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=screenshots

mkdir -p "$OUT"

echo "::group::Device"
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model
echo "::endgroup::"

echo "::group::Install"
# -r so a re-run over a cached AVD snapshot does not fail on "already exists".
# -g pre-grants runtime permissions so no system dialog is sitting on top of the
# first screenshot — but it aborts the whole install if any declared permission is
# not grantable that way, and this manifest declares Health Connect permissions
# that are not. So it is an attempt, not a requirement.
adb install -r -g "$APK" || adb install -r "$APK" || {
  echo "::error::Could not install $APK"
  exit 1
}
echo "::endgroup::"

# Launch through the launcher intent rather than a hard-coded component: the debug
# build carries applicationIdSuffix ".debug" while MainActivity keeps the unsuffixed
# class name, and that mismatch is the classic `am start` "Activity not found".
echo "::group::Launch"
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
echo "::endgroup::"

shoot() {
  local label="$1"
  adb exec-out screencap -p > "$OUT/${label}.png"
  echo "captured $OUT/${label}.png ($(stat -c%s "$OUT/${label}.png") bytes)"
}

# Three moments, because one screenshot of a cold-started app is usually a splash.
sleep 5;  shoot 01-launch
sleep 10; shoot 02-after-15s
sleep 15; shoot 03-after-30s

echo "::group::Foreground activity"
# Names the screen each PNG is actually showing, which a reviewer cannot always
# tell from the pixels alone.
adb shell dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity' || true
echo "::endgroup::"

echo "::group::App logcat (crashes and Firebase init)"
adb logcat -d -v brief | grep -iE "goalpilot|AndroidRuntime|FirebaseApp|GoogleApi" \
  | tail -n 200 > "$OUT/logcat.txt" || true
cat "$OUT/logcat.txt"
echo "::endgroup::"

# A crash is not a failure of THIS job — the artifacts are still worth having, and
# the logcat above is how you find out. But say so loudly, because three identical
# screenshots of a blank screen otherwise read as "the app has no UI".
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt" 2>/dev/null; then
  echo "::warning::The app logged a FATAL EXCEPTION. Read screenshots/logcat.txt."
fi

ls -la "$OUT"
