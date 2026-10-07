#!/usr/bin/env bash
# Only the Play-specific download/verification gap; release creation uses actions.
set -euo pipefail
: "${VERSION:?}" "${VERSION_CODE:?}" "${PLAY_ACCESS_TOKEN:?}"
: "${PLAY_APP_SIGNING_SHA256:?}" "${ANDROID_HOME:?}" "${ANDROID_BUILD_TOOLS:?}"
[[ "$VERSION" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]]
[[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]]
CERT=$(printf '%s' "$PLAY_APP_SIGNING_SHA256" | tr -d ':' | tr '[:upper:]' '[:lower:]')
[[ "$CERT" =~ ^[0-9a-f]{64}$ ]]
BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.ah.taplock/generatedApks/$VERSION_CODE"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
ID=''
for ((attempt = 0; attempt < 60; attempt++)); do
  STATUS=$(curl --silent --show-error --max-time 60 -H "Authorization: Bearer $PLAY_ACCESS_TOKEN" \
    -o "$TMP/generated.json" -w '%{http_code}' "$BASE")
  case "$STATUS" in
    200)
      ID=$(jq -er --arg cert "$CERT" '
        [.generatedApks[]? | select((.certificateSha256Hash | ascii_downcase | gsub(":"; "")) == $cert)
          | .generatedUniversalApk.downloadId // empty]
        | if length > 1 then error("Ambiguous universal APK") else .[0] // "" end' "$TMP/generated.json")
      if [[ -n "$ID" ]]; then break; fi
      ;;
    404|429|500|502|503|504) ;;
    *) echo "Play generated APK API returned HTTP $STATUS" >&2; cat "$TMP/generated.json" >&2; exit 1 ;;
  esac
  sleep 15
done
if [[ -z "$ID" ]]; then
  echo 'No universal APK for the expected signer yet. Use Re-run failed jobs after Play finishes processing.' >&2
  exit 1
fi
ID=$(jq -rn --arg id "$ID" '$id | @uri')
HTTP_STATUS=$(curl --fail --silent --show-error --location --max-time 180 \
  -H "Authorization: Bearer $PLAY_ACCESS_TOKEN" \
  -o "$TMP/release.apk" -w '%{http_code}' \
  "$BASE/downloads/$ID:download?alt=media")
if [[ "$HTTP_STATUS" != 200 || ! -s "$TMP/release.apk" ]]; then
  echo "Play APK download returned HTTP $HTTP_STATUS without an APK." >&2
  exit 1
fi
TOOLS="$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS"
"$TOOLS/apksigner" verify --verbose --print-certs "$TMP/release.apk" > "$TMP/signature.txt"
ACTUAL=$(sed -nE 's/^(V[0-9.]+ Signer:|Signer #[0-9]+) certificate SHA-256 digest: ([[:xdigit:]:]+)$/\2/p' "$TMP/signature.txt" | tr -d ':' | tr '[:upper:]' '[:lower:]' | sort -u)
if [[ "$ACTUAL" != "$CERT" ]]; then
  echo 'Downloaded APK signer does not match the configured Play signing certificate.' >&2
  exit 1
fi
BADGING=$("$TOOLS/aapt" dump badging "$TMP/release.apk")
EXPECTED="package: name='com.ah.taplock' versionCode='$VERSION_CODE' versionName='$VERSION'"
if ! grep -Fqx "$EXPECTED" <(printf '%s\n' "$BADGING" | sed -nE "s/^(package: name='[^']+' versionCode='[^']+' versionName='[^']+').*/\1/p"); then
  echo 'Downloaded APK package or version does not match the requested release.' >&2
  exit 1
fi
mkdir -p dist
cp "$TMP/release.apk" "dist/TapLock-$VERSION.apk"
