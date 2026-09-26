# Mileage

A personal mileage tracker for Android. Log trips, track distance and travel time, and see the totals broken down by year and by private vs. business use.

Built with the Kotlin Toolchain and Jetpack Compose.

## Disclaimer

This project was written entirely with AI assistance for prototyping and the initial release. It exists as a learning vehicle: from version 0.1.0 onwards, the code will be written by hand, with AI used only for direction and feedback. The goal is to learn Kotlin by building and maintaining this app myself.

## Features

- **Trip log** — record each trip with start and end date-time, postal codes, odometer readings, license plate, and notes. Mark trips as private or business.
- **Auto-fill** — new trips default to where the last one finished: start postal code, odometer reading, and license plate carry over. Optionally assume a return leg and pre-fill the end postal code too.
- **Drafts** — save an incomplete trip and finish it later. Drafts are marked in the list and excluded from the auto-fill defaults.
- **Statistics** — line charts and a yearly table showing mileage or travel time, split into private, business, and total. Filter by date range, and switch between per-period and cumulative (mileage) or per-trip average (time).
- **Encrypted storage** — the trip database is encrypted with SQLCipher, and the key is wrapped by the Android Keystore so it never exists in plaintext on disk.
- **Settings** — theme (system / light / dark), layout positions, and behaviour toggles for the auto-fill features.

## Tech stack

| Component | Choice |
| :--- | :--- |
| Language | Kotlin |
| UI | Jetpack Compose with Material 3 |
| Build | Kotlin Toolchain (Amper) |
| Database | Room + SQLCipher |
| Preferences | Preferences DataStore |
| Async | Coroutines and Flow |
| Min SDK | 26 |

## Prerequisites

- **JDK 21** — the Kotlin Toolchain requires it
- **Android SDK** — with `platform-tools` for `adb`, and a platform matching the compile SDK
- **Kotlin Toolchain** — the `./kotlin` wrapper in this repo downloads the toolchain on first run

On Fedora with an ARM64 machine (e.g. Asahi Linux), the Android build tools need QEMU emulation for the x86-64 `aapt2` binary. See [Building on ARM64](#building-on-arm64) below.

## Getting started

Clone and build:

```bash
git clone https://github.com/<your-username>/mileage-tracker.git
cd mileage-tracker
chmod +x ./kotlin
./kotlin build
```

That produces a debug APK. Install it on a connected device:

```bash
find build -name "*.apk" -type f | head -1
adb install -r <path-to-apk>
```

Or build and launch in one step, if a device is connected:

```bash
./kotlin run
```

## Project structure

```
mileage-tracker/
├─ module.yaml                 # build config: dependencies, namespace, signing
├─ keystore.properties         # signing credentials (gitignored)
├─ proguard-rules.pro          # R8 keep rules for release builds
├─ res/
│  └─ values/
│     └─ strings.xml           # app name and other resources
├─ src/
│  ├─ AndroidManifest.xml
│  ├─ MainActivity.kt          # navigation between screens
│  ├─ data/
│  │  ├─ Trip.kt               # entity, DAO, computed properties
│  │  ├─ Database.kt           # Room database, migrations, SQLCipher wiring
│  │  ├─ SqlCipherKeyManager.kt # Android Keystore-backed key management
│  │  └─ SettingsRepository.kt # DataStore preferences
│  └─ ui/
│     ├─ LandingScreen.kt
│     ├─ TripsScreen.kt
│     ├─ TripEntryScreen.kt
│     ├─ StatsScreen.kt
│     ├─ SettingsScreen.kt
│     ├─ TripViewModel.kt
│     ├─ YearlyStats.kt
│     └─ Theme.kt
└─ .github/
   └─ workflows/
      └─ build.yml             # CI: debug APK on push, release APK on demand
```

## Build commands

| Command | Output |
| :--- | :--- |
| `./kotlin build` | Debug APK |
| `./kotlin package` | Signed release AAB |
| `./kotlin run` | Build and launch on a connected device |
| `./kotlin tool generate-keystore` | Create a signing keystore from `keystore.properties` |

## Signing and release

Release builds are signed with a keystore whose path and passwords live in `keystore.properties` at the project root:

```properties
storeFile=/home/<user>/.keystores/release.keystore
storePassword=<password>
keyAlias=mileage
keyPassword=<password>
```

The keystore itself is **not** in the repository. Create it with:

```bash
keytool -genkeypair -v \
  -keystore ~/.keystores/release.keystore \
  -alias mileage \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

Since PKCS12 uses a single password, `storePassword` and `keyPassword` hold the same value.

**Back the keystore up somewhere safe.** If it's lost after the app has been distributed, the app can never be updated — Android identifies apps by their signing certificate, and there is no recovery mechanism.

To produce a release build:

```bash
./kotlin package
```

The Kotlin Toolchain outputs an **AAB**, not an APK. To get an installable release APK, convert it with `bundletool`:

```bash
java -jar bundletool.jar build-apks \
  --bundle=build/tasks/.../gradle-project-release.aab \
  --output=mileage-release.apks \
  --mode=universal \
  --ks=~/.keystores/release.keystore \
  --ks-pass=pass:<storePassword> \
  --ks-key-alias=mileage \
  --key-pass=pass:<keyPassword>

unzip -p mileage-release.apks universal.apk > mileage-release.apk
```

## Continuous integration

`.github/workflows/build.yml` defines two jobs:

- **Debug APK** — runs on every push to `main` and on pull requests. No secrets required. Uploads a debug APK artifact.
- **Release APK** — runs only on manual trigger. Builds a signed AAB, converts it to a universal APK with `bundletool`, and uploads the result.

The release job needs four repository secrets:

| Secret | Value |
| :--- | :--- |
| `KEYSTORE_BASE64` | `base64 -w 0 ~/.keystores/release.keystore` |
| `KEYSTORE_PASSWORD` | The keystore password |
| `KEY_ALIAS` | `mileage` |
| `KEY_PASSWORD` | The same password |

To run a release build: **Actions → Build APK → Run workflow**.

## Data and privacy

All trip data is stored in a Room database encrypted with SQLCipher. The encryption key is generated once on first launch, wrapped with a master key held in the Android Keystore, and stored in the app's private preferences. The key material never exists in plaintext outside the app's process.

Nothing is sent off the device. There is no network access, no analytics, and no cloud sync.

Because the database is encrypted, standard SQLite tools cannot read it. The file at `/data/data/<package>/databases/mileage.db` begins with a random salt rather than the `SQLite format 3` header.

## Building on ARM64

On ARM64 Linux (Fedora Asahi, Debian ARM, etc.), the Android Gradle Plugin expects an x86-64 `aapt2` binary and fails under native execution. The fix is a QEMU wrapper:

```bash
curl -sLO https://raw.githubusercontent.com/IgnacioLD/aapt2-qemu/main/aapt2-qemu-setup.sh
sh aapt2-qemu-setup.sh
```

The setup script FUSE-mounts an x86-64 sysroot at `/tmp/aapt2-x86root`, writes a wrapper to `~/.local/share/aapt2-qemu/aapt2`, and points Gradle at it via `android.aapt2FromMavenOverride`. The mount is ephemeral — after a reboot, re-run the script.

If a build fails with `AAPT2 aapt2-qemu Daemon #0: Daemon startup failed`, that's almost always the lost mount. Re-running the script fixes it.

## License

Personal project. Not currently licensed for redistribution.