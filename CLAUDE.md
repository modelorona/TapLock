# TapLock

Android screen-lock utility using accessibility services and widgets. Single-module Kotlin/Compose app.

## Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew testDebugUnitTest      # Unit tests (DoubleTapDetector)
./gradlew connectedAndroidTest   # Instrumented UI tests (requires device/emulator)
./gradlew lintDebug              # Lint checks
```

## Architecture

Single `:app` module. All source in `app/src/main/java/com/ah/taplock/`:

- **MainActivity.kt** — Compose settings UI, onboarding flow, permission management
- **TapLockAccessibilityService.kt** — Core service: overlay management, double-tap detection on status bar and lock screen
- **TapLockWidgetProvider.kt** — RemoteViews-based 1x1 widget
- **FloatingButtonService.kt** — Draggable overlay button (foreground service)
- **TapLockTileService.kt** — Quick Settings tile
- **DoubleTapDetector.kt** — Time-window tap detection with injectable clock
- **VibrationHelper.kt** — API-level-aware vibration (Android 13+ vs older)
- **Utils.kt** — Accessibility state check utility

## Build Config

- Min SDK 31 (Android 12), Target/Compile SDK 36
- Java 21, Kotlin 2.3.20, AGP 9.2.0
- Dependencies managed via `gradle/libs.versions.toml`
- R8 minification enabled for release builds

## Code Style

Kotlin official style (`kotlin.code.style=official`). No detekt/ktlint — use `./gradlew lintDebug`.

## Gotchas

- `TapLockAccessibilityService.instance` is intentionally static (`@Suppress("StaticFieldLeak")`). Cleared in `onUnbind()`/`onDestroy()`. Don't add another static reference.
- Overlay touch handling: must call `performClick()` after ACTION_UP. Overlay height changes reset the double-tap detector to prevent false positives.
- Lock screen tap zone detection uses depth-first traversal to find clickable nodes — touches near interactive elements are forwarded, not counted.
- Widget uses RemoteViews (no Compose). Custom icon requires manual cache invalidation via ACTION_APPWIDGET_UPDATE broadcast.
- VibrationHelper branches on Build.VERSION for Android 13+. Both paths must be maintained.
- SharedPreferences are synchronous throughout — changes immediately trigger recomposition via preference listener.
