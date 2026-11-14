# Lumi Lite

#### Infinitely Customizable Android Homescreen

[![Android CI](https://github.com/dimmstz/Supernova-glass-redesign/actions/workflows/android.yml/badge.svg)](https://github.com/dimmstz/Supernova-glass-redesign/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/dimmstz/Supernova-glass-redesign?display_name=tag)](https://github.com/dimmstz/Supernova-glass-redesign/releases)

Lumi Lite is an open-source Android launcher focused on deep customization, gestures, widgets, feeds, icon packs and a fast, distraction-free home screen.

## Install

- [F-Droid](https://f-droid.org/packages/com.dimm.lumilite)
- [Latest APK release](https://github.com/dimmstz/Supernova-glass-redesign/releases/latest)
- Play Store: coming soon

The next release line is `v0.3.12`. The app includes a complete Brazilian Portuguese (`pt-BR`) localization, a graphite monochrome dark settings theme inspired by the Lumi Lite reference, the official Lumi Lite icon on the welcome screen, updated first-run defaults for the home screen, Dock and app drawer, smart category groups configurable as separate drawer tabs or category folders, manual drawer groups, a modern Glassmorphism weather-and-clock widget and an organized group-options card beside the drawer tabs. Release APKs are built, signed and attached automatically by GitHub Actions from version tags.

## Build locally

The project requires JDK 17 and Android SDK Platform 36. After installing those prerequisites, run:

```bash
./gradlew testSafeDebug assembleSafeDebug
```

The debug APK is written to `app/build/outputs/apk/safe/debug/`. For a release build, run:

```bash
./gradlew assembleSafeRelease
```

Release builds use the existing `keystore.properties` configuration locally when a production signing key is available. For GitHub Releases, configure all four repository secrets to sign with the same persistent key:

- `LUMI_RELEASE_KEYSTORE_BASE64`
- `LUMI_RELEASE_STORE_PASSWORD`
- `LUMI_RELEASE_KEY_ALIAS`
- `LUMI_RELEASE_KEY_PASSWORD`

If none of these secrets are configured, CI generates a temporary signing key so the release APK can still be installed and tested. That temporary key is different for each release: Android may require uninstalling the previous Lumi Lite installation before installing the APK, and app data can be lost. Do not use temporary-key builds as seamless updates. If only some of the four secrets are configured, the release is stopped instead of silently publishing with the wrong key.

Debug artifacts intentionally use the separate package `com.dimm.lumilite.debug` and are not update-compatible with the release package.

## Continuous integration and releases

Every push to `main` and every pull request targeting `main` runs unit tests and Android lint as required checks, reports Detekt static-analysis findings without blocking the build, and builds the debug APK, validates its package ID and uploads the artifact. A version tag matching `v*` runs unit tests and release lint, builds the release APK, validates the package ID and version against the tag, signs and verifies the APK, uploads the artifact, and creates a GitHub Release with generated release notes and the APK attached.

To publish a new release after updating `versionCode` and `versionName` in `app/build.gradle.kts`:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

The workflow then performs the build and publishes the release automatically.

## Design system update

The current UI includes a performance-conscious Glassmorphism layer for Settings: deep dark surfaces, translucent cards, specular 1px rims, accent glows, shared radius/elevation tokens, focus/press states and a single Android 12+ window backdrop blur with a fallback for older devices. Business logic, launcher gestures, navigation contracts, DataStore persistence and widget behavior remain unchanged. Public Releases use the Safe variant, which omits contacts, calendar, Bluetooth, notification-listener, accessibility and device-admin capabilities to reduce Play Protect warnings for sideloaded APKs.

## Follow

- X/Twitter [@LumiLiteLauncher](https://x.com/supernovalaunch)
- Reddit [r/LumiLiteLauncher](https://www.reddit.com/r/LumiLiteLauncher)

## License

AGPL-3.0. See [LICENSE](LICENSE).

## Developer

- X/Twitter [@tanujnotes](https://x.com/tanujnotes)
- [Website](https://tanujnotes.github.io)

> Xiaomi and a few other brands do not allow navigation gestures with third-party launchers. To fix that, try the free open-source [Ogesture](https://github.com/tanujnotes/Ogesture) app.

<a href="https://www.star-history.com/?repos=tanujnotes%2Fsupernova&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=tanujnotes/supernova&type=date&theme=dark&legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=tanujnotes/supernova&type=date&legend=top-left" />
    <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=tanujnotes/supernova&type=date&legend=top-left" />
  </picture>
</a>
