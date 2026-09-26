# OpenRealm Launcher

OpenRealm Launcher is an independent Android launcher for Minecraft: Java Edition, based on the GPL-licensed ZalithLauncher2 upstream project.

This repository is an **unofficial modified fork** and is not affiliated with the original project or its maintainers.

## Project

Repository: https://github.com/Rushabh-00/OpenRealmLauncher

Application ID:

`dev.openrealm.launcher`

The launcher is intentionally English-only.

## Build

### Requirements

- Android Studio with current Android SDK tooling
- Android SDK API 26 or newer
- JDK 21 for the Android build

### Build a debug ARM64 APK

```bash
chmod +x gradlew
./gradlew :OpenRealmLauncher:assembleDebug -Darch=arm64
```

The project produces **arm64-v8a** APKs only.

## Display refresh and FPS

OpenRealm Launcher detects the highest Android display refresh rate available at the current display resolution and requests that display mode through the Android Activity/window configuration.

Before a Minecraft game launch, the launcher synchronizes Minecraft's `maxFps` option with the detected display refresh rate when that value is still launcher-managed. Manual FPS choices are preserved.

This implementation does not modify Minecraft JVM arguments, classpaths, LaunchWrapper, Caciocavallo, Java agents, or the Minecraft launch architecture.

## Accounts and services

Offline/local accounts are available globally and do not require a Microsoft account.

Microsoft authentication remains available for users who choose it.

CurseForge support uses a configured API key when available and retains a mirror fallback path without embedding a private API key in the APK.

## Updates

OpenRealm Launcher uses its own update metadata:

`update/latest_version_md.json`

Release automation updates this metadata from OpenRealm GitHub release assets.

## Continuous integration

The repository uses two GitHub Actions workflows:

- `.github/workflows/build.yml` — push, pull request, and manual ARM64 debug builds.
- `.github/workflows/release_ci.yml` — signed ARM64 release builds and release metadata updates.

Build concurrency cancels obsolete runs when a newer commit supersedes them.

## Upstream and license

OpenRealm Launcher is distributed under the GPL-3.0 license. Upstream copyright notices and legally required attribution are retained.

For the upstream project's source, see the ZalithLauncher2 repository:
https://github.com/ZalithLauncher/ZalithLauncher2
