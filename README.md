# WaveFret

An Android app for practicing bass guitar: record yourself, review, and
(eventually) get real-time feedback on pitch against a chosen
tab/tablature — see [Roadmap](#roadmap).

## Status: v2 — Practice engine complete

The current build lets you:

- record practice clips, browse, play back and delete them (v0);
- tune your bass with a live needle tuner (v1);
- practice built-in exercises against a scrolling tab (v2), either in
  **wait mode** (the tab only advances once you play the right note) or
  **tempo mode** (a metronome counts you in and each note is scored as
  hit or missed on its beat).

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

Top-level packages under `com.example.wavefret`:

- **`common`** — generic infrastructure with no knowledge of any one
  feature: `audio` (PCM source, YIN pitch detection, low-pass filter,
  silence gate, metronome), `music` (notes, cents, fret frequencies,
  bass tunings), `time` (Clock, BeatClock, formatters), `permission`,
  `storage`.
- **`recording`** — recording, listing, playback and deletion of clips.
- **`tuner`** — the live tuner pipeline and needle display.
- **`practice`** — the tab model and text-tab parser, built-in
  exercises, wait/tempo practice sessions, the practice pipeline and the
  scrolling tab display.

The dependency direction is one-way: feature packages depend on
`common`, never the reverse, and never on each other. This keeps generic pieces reusable by future
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
- **v1** — Tuner (done): pitch detection via YIN algorithm
- **v2** — Practice engine (done): follow a tab note by note in wait
  mode, or in time with a metronome in tempo mode
- **v3** — Song import: text tabs, MusicXML, and (longer-term) audio
  transcription from YouTube via Demucs + basic-pitch
- **v4** — Difficulty scoring and progressive song ordering
