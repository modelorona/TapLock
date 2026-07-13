#!/usr/bin/env bash
#
# Capture a "hero" screenshot of the TapLock widget on the launcher home screen.
#
# This is a MANUAL one-off helper, not part of the deterministic `fastlane screenshots`
# lane. It drives the real launcher's widget-pin flow via adb + UI Automator, which is
# inherently launcher- and timing-dependent (it matches button text and the Pixel/Nexus
# launcher pin dialog). Expect to eyeball the result and occasionally re-run.
#
# Prerequisites:
#   - An emulator/device already booted and visible to `adb devices`.
#   - The debug APK installed (e.g. run `./gradlew installDebug` or the screenshots lane first).
#
# Usage:
#   ANDROID_HOME=~/Library/Android/sdk ./scripts/capture-home-widget.sh [output.png]
#
set -euo pipefail

PKG="com.ah.taplock"
OUT="${1:-fastlane/metadata/android/en-US/images/phoneScreenshots/00_home_widget.png}"
# In-app button label differs first-time vs. when a widget already exists.
ADD_WIDGET_LABEL="Add Widget to Home Screen"
ADD_ANOTHER_LABEL="Add Another Widget"
PIN_CONFIRM_LABEL="Add to home screen"          # launcher pin-dialog confirm button

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
command -v "$ADB" >/dev/null 2>&1 || ADB="adb"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

die() { echo "error: $*" >&2; exit 1; }

"$ADB" get-state >/dev/null 2>&1 || die "no device/emulator connected (check 'adb devices')"
"$ADB" shell pm path "$PKG" >/dev/null 2>&1 || die "$PKG not installed; build/install the debug APK first"

# Center coordinates of a uiautomator node whose text matches $1, from the dumped XML in $2.
tap_text() {
  local label="$2" xml="$3"
  local bounds
  bounds="$("$ADB" pull "$xml" "$TMP/ui.xml" >/dev/null 2>&1; \
    grep -oE "text=\"$label\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$TMP/ui.xml" \
      | grep -oE '[0-9]+' | tr '\n' ' ' || true)"
  [ -n "$bounds" ] || return 1
  # shellcheck disable=SC2086
  set -- $bounds
  local cx=$(( ($1 + $3) / 2 )) cy=$(( ($2 + $4) / 2 ))
  echo "  tapping '$label' at $cx,$cy"
  "$ADB" shell input tap "$cx" "$cy"
}

echo "==> Allowing $PKG to bind app widgets"
"$ADB" shell appwidget grantbind --package "$PKG" --user current >/dev/null 2>&1 || true

# Dim the wallpaper to a clean solid dark backdrop. Besides looking tidier, this works
# around an emulator GPU bug that smears the gradient wallpaper on some home pages.
# Restored on exit.
echo "==> Dimming wallpaper for a clean backdrop"
"$ADB" shell cmd wallpaper set-dim-amount 1.0 >/dev/null 2>&1 || true
trap '"$ADB" shell cmd wallpaper set-dim-amount 0.0 >/dev/null 2>&1 || true; rm -rf "$TMP"' EXIT

echo "==> Launching app (fresh, scrolled to top)"
"$ADB" shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 1
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 3

echo "==> Locating the in-app add-widget button"
"$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
tap_text tap "$ADD_WIDGET_LABEL" /sdcard/ui.xml \
  || tap_text tap "$ADD_ANOTHER_LABEL" /sdcard/ui.xml \
  || die "could not find the add-widget button ('$ADD_WIDGET_LABEL' / '$ADD_ANOTHER_LABEL') — did the label change?"
sleep 3

echo "==> Confirming the launcher pin dialog"
"$ADB" shell uiautomator dump /sdcard/ui2.xml >/dev/null 2>&1
tap_text tap "$PIN_CONFIRM_LABEL" /sdcard/ui2.xml \
  || die "could not find '$PIN_CONFIRM_LABEL' pin-dialog button"
sleep 2

echo "==> Going to the home screen and repainting"
# Restart the launcher so it repaints cleanly with the dimmed wallpaper before capture.
"$ADB" shell input keyevent KEYCODE_HOME; sleep 1
"$ADB" shell am force-stop com.google.android.apps.nexuslauncher >/dev/null 2>&1 || true
sleep 2
"$ADB" shell input keyevent KEYCODE_HOME; sleep 4

echo "==> Capturing $OUT"
mkdir -p "$(dirname "$OUT")"
"$ADB" exec-out screencap -p > "$OUT"
echo "Done: $OUT ($(wc -c < "$OUT" | tr -d ' ') bytes)"
echo "Review the image — the widget should appear top-left on the home screen."
