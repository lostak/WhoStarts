# Pool Tracker (Android)

A simple Android app to track who won the last game of pool.

## Features
- Scrollable list of matchups (e.g. "Alice vs Bob" or "Alice & Carl vs Bob & Dana")
- Add a new matchup by entering comma-separated player names for each team
- A switch on each matchup row marks who won the last game
- Tap "History" on a matchup to see every past result and the overall win count

## How data is stored
Everything is saved locally on-device using SharedPreferences (JSON), so it
persists between app launches. No account or internet connection needed.

## How to build it
This is a standard Gradle-based Android Studio project.

1. Install [Android Studio](https://developer.android.com/studio) (this also
   installs the Android SDK and Gradle).
2. Unzip this project.
3. Open Android Studio → **File > Open** → select the unzipped `PoolTracker` folder.
4. Let Gradle sync (first sync downloads dependencies, needs internet).
5. Plug in an Android phone (with USB debugging on) or start an emulator.
6. Click the green **Run ▶** button.

To produce an installable APK without a phone plugged in:
**Build > Build Bundle(s) / APK(s) > Build APK(s)**, then find the `.apk`
under `app/build/outputs/apk/debug/` and transfer it to a phone to install
(you'll need to allow "install from unknown sources" if not using the Play Store).

## Project structure
- `MainActivity.kt` — the whole UI (Jetpack Compose): matchup list, add-matchup
  dialog, history dialog, and the win-toggle switch.
- `Models.kt` — `Matchup` and `GameResult` data classes.
- `Storage.kt` — saves/loads matchups as JSON in SharedPreferences.
- `PoolViewModel.kt` — holds app state and talks to `Storage`.

## Extending it later
- Swap `Storage` for a Room database if the list grows large.
- Add editing of existing matchups (currently: delete and re-add).
- Add a stats screen aggregating wins across all matchups.
