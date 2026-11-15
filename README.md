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

## Getting started

Clone and build:

```bash
git clone https://github.com/NebulousPrime26/mileage-tracker.git
cd mileage-tracker
chmod +x ./kotlin
./kotlin build