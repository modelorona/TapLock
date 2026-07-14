# Repository Guidelines

## Project Structure & Module Organization
`TapLock` is a single-module Android app. Application code lives in `app/src/main/java/com/ah/taplock`; the Compose theme is under `app/src/main/java/com/ah/taplock/ui/theme`. Manifest and XML resources live in `app/src/main/AndroidManifest.xml` and `app/src/main/res`. Local JVM tests live in `app/src/test/...`; device and emulator tests live in `app/src/androidTest/...`. Shared dependency versions are managed in `gradle/libs.versions.toml`. Canonical store screenshots are generated into `fastlane/metadata/android/` (see "Store Screenshots" below); the legacy `screenshots/` directory holds older hand-captured reference images. Treat `app/build/` as generated output and do not edit it by hand.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build the debug APK.
- `./gradlew installDebug`: install the debug build on a connected device or emulator.
- `./gradlew testDebugUnitTest`: run JVM unit tests such as `DoubleTapDetectorTest`.
- `./gradlew connectedDebugAndroidTest`: run Compose and instrumentation tests on a connected device or emulator.
- `./gradlew lint`: run Android Lint for the default variant.

## Coding Style & Naming Conventions
Use Kotlin official style (`kotlin.code.style=official`) with 4-space indentation. Keep classes, services, and composables in `PascalCase`; prefer descriptive function and test names such as `twoTapsWithinTimeout_returnsDoubleTap`. Use `snake_case` for Android resources such as `widget_layout.xml` and `ic_lock_tile.xml`. Keep new UI work aligned with the existing Jetpack Compose and Material 3 stack, and add permissions or services only when strictly necessary.

## Testing Guidelines
Put pure logic tests in `app/src/test` and UI or integration coverage in `app/src/androidTest`. Name test files after the target class (`FooTest.kt`) and keep test methods behavior-first. Run `testDebugUnitTest` for logic changes and `connectedDebugAndroidTest` for UI, widget, accessibility, or preference flows. Lint should pass before submitting.

## Commit & Pull Request Guidelines
Recent history favors short, imperative, lower-case subjects such as `fix lint issues`, `update version`, or `add onboarding flow`; reference issues when relevant. Keep commits focused and avoid mixing UI, build, and refactor work. Pull requests should explain the user-visible change, list the verification commands you ran, and include screenshots for Compose, widget, or settings changes.

## Store Screenshots (fastlane screengrab)
Store screenshots are generated automatically, not captured by hand. They live under `fastlane/metadata/android/<locale>/images/{phone,sevenInch,tenInch}Screenshots/` (locales: `en-US`, `es-ES`).

Setup is project-local: Ruby is pinned via `.ruby-version` (rbenv) and fastlane via the `Gemfile` (`bundle install`). The capture flow builds the debug + androidTest APKs, then `ScreenshotTest.kt` (in `app/src/androidTest`) drives `TapLockScreen` and calls `Screengrab.screenshot(...)`; `fastlane/Screengrabfile` controls locales and output. Stable capture relies on `testTag`s (e.g. `widget_style_preview`, `button_quick_settings_tile`) — keep those when editing the UI. Debug-only permissions for screengrab live in `app/src/debug/AndroidManifest.xml`.

Boot the matching emulator, then run per form factor (device type routes images to the right folder):
- Phone (`Pixel_10`): `bundle exec fastlane screengrab`
- 7-inch (`TapLock_Tablet7`): `bundle exec fastlane screengrab --device_type sevenInch`
- 10-inch (`TapLock_Tablet10`): `bundle exec fastlane screengrab --device_type tenInch`

Screenshots capture whatever the emulator renders, so set light/dark mode (`adb shell cmd uimode night no|yes`) before running. Note `clear_previous_screenshots` is on, so a run replaces the existing set for that device type/locale. `scripts/capture-home-widget.sh` is a separate manual helper for a home-screen widget hero shot (emulator-timing dependent; eyeball the result).

## Security & Configuration Tips
TapLock is privacy-sensitive: avoid adding network permissions, analytics, or broader accessibility or overlay behavior without clear justification in the pull request. Keep SDK and dependency changes in sync across `app/build.gradle.kts` and `gradle/libs.versions.toml`.
