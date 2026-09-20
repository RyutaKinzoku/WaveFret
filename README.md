# WaveFret

An Android app for practicing bass guitar: record yourself, review, and
(eventually) get real-time feedback on pitch against a chosen
tab/tablature — see [Roadmap](#roadmap).

## Status: v0 — Recorder MVP complete

The current build lets you record practice clips, browse them in a list
with duration and timestamp, play them back one at a time, and delete
the ones you don't need.

## Requirements

- Android Studio (current stable)
- Android SDK, minSdk 24 (Android 7.0)
- A device or emulator with a working microphone

## Running it

1. Clone the repo and open it in Android Studio.
2. Let Gradle sync.
3. Run on a device or emulator — you'll be prompted for microphone access
   on first launch.

## Running tests

```
./gradlew testDebugUnitTest
```

All business logic (permission decisions, recording/playback
orchestration, file listing, duration/date formatting) is covered by
JUnit tests that run on the JVM — no emulator required. Code that only
wraps a thin Android framework class (`MediaRecorder`, `MediaPlayer`,
`MediaMetadataRetriever`, `ContextCompat`) is deliberately left
untested and verified manually on-device instead, since there's no
meaningful logic left in those wrappers to unit test.

## Architecture

Two top-level packages under `com.example.wavefret`:

- **`common`** — generic infrastructure with no knowledge of recording:
  `time` (Clock, DateFormatter, DurationFormatter), `permission`
  (PermissionChecker), `storage` (AppStorageDirectoryProvider).
- **`recording`** — everything specific to recording bass practice:
  session control, the recordings list, playback, and the
  recording-specific use of permissions.

The dependency direction is one-way: `recording` depends on `common`,
never the reverse. This keeps generic pieces reusable by future
features (e.g. a tuner or practice-tracking module) without those
features reaching into `recording`'s internals.

Within `recording`, framework classes (`MediaRecorder`, `MediaPlayer`,
`MediaMetadataRetriever`) sit behind small interfaces
(`AudioRecorder`, `AudioPlayer`, `AudioDurationReader`), so the actual
decision logic (`RecordingSessionController`, `PlaybackController`,
`RecordingsRepository`) can be unit tested with fakes instead of real
hardware.

## Roadmap

- **v0** — Recorder (done): record, list, play back, delete
- **v1** — Tuner: pitch detection via YIN algorithm
- **v2** — Practice engine: follow a tab, note by note, waiting for the
  correct pitch before advancing
- **v3** — Song import: text tabs, MusicXML, and (longer-term) audio
  transcription from YouTube via Demucs + basic-pitch
- **v4** — Difficulty scoring and progressive song ordering
