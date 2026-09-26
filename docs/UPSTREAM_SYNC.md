# Upstream sync policy

OpenRealm Launcher is a customized fork of ZalithLauncher2.

At the 2026-09-26 sync point, ZalithLauncher2/main was at:

- `718b699e1d4553857bc636e6a66abf8d71039547`

The OpenRealm fork diverges from the upstream history at the clean-start base `044a60a3ab49ab108b387a5cd439a752b6d918ef`.

The upstream changes after that base were reviewed before this sync. Weblate-only translation commits are intentionally not imported because OpenRealm Launcher is English-only. Upstream Guide/festival UI additions were also reviewed separately rather than copied wholesale because OpenRealm has independent package, branding, UI, and performance changes.

Functional OpenRealm-specific behavior remains authoritative for:
- package and application identity
- English-only resources
- global offline accounts
- global mirror UI
- CurseForge fallback behavior
- ARM64 packaging
- release signing and CI
- OpenRealm update feed
- Android refresh-rate/FPS handling
- Minecraft runtime/launch compatibility

Future upstream syncs should compare against the recorded upstream SHA above and selectively port source changes after review.
