# WaveFret

An Android app for practicing bass guitar: record yourself, review, and
(eventually) get real-time feedback on pitch against a chosen
tab/tablature — see [Roadmap](#roadmap).

## Status: 0.14.0 — hear any lesson, piano scores, light-on-dark video tabs

The current build lets you:

- record practice clips, browse, play back and delete them (v0);
- tune your bass with a guided tuner (v1, rebuilt in 0.10): a real-time
  needle, automatic lowest-string-first or a string you pick, a chime and
  a ✓ when each string is in tune;
- practice built-in exercises against a scrolling tab (v2), either in
  **wait mode** (the tab only advances once you play the right note) or
  **tempo mode** (a metronome counts you in and each note is scored as
  hit or missed on its beat);
- add the songs you want to learn (v3, 0.7): paste an ASCII bass tab,
  or open/share a Guitar Pro 7+ (`.gp`), MIDI (`.mid`) or MusicXML
  (`.musicxml`, `.xml`, `.mxl`) file, or read a photo or PDF (0.13): drawn
  tab staffs, tabs written as text, and sheet music (experimental; on
  piano and vocal scores only the bass-clef staff is read, 0.14). Songs live in a searchable library sorted by difficulty;
- turn audio into a lesson (0.8): an audio file, shared audio, or one of
  your takes. Pick "just my bass" (YIN, accurate) or "full song"
  (on-device basic-pitch model, rough), then fix any wrong notes in the
  lesson editor;
- find a song by name (0.12): a built-in browser searches YouTube bass
  covers or web bass tabs; use the open video, or the tab text on the
  page, without leaving the app;
- play along with the original song (0.9): it stops at each note until
  you play it right. Works with lessons made from audio files, YouTube
  links (YouTube's own player; nothing is downloaded; on Android 10+
  WaveFret hears the video's sound directly) and Listen mode (WaveFret
  listens to a song played on a speaker);
- choose the theme and text size in Settings (0.12);
- read the tab a YouTube bass cover shows while WaveFret listens (0.13),
  dark on light or light on dark (0.14);
- hear any lesson on a synthesized bass, from practice or from the
  selected note in the lesson editor, to check its notes by ear (0.14);
- add a YouTube video or audio file to any lesson for play-along: the
  lesson's notes stay, their timing comes from the song (0.13);
- record audio or video takes and share them with any app (0.7);
- follow a learning path (v4): exercises are rated Beginner /
  Intermediate / Advanced by a heuristic, listed easiest first with your
  best score, and the easiest one not yet finished is recommended
  (0.12); mastery (90%+ in tempo mode) earns a trophy. Every completed run is saved to
  `practice_results.tsv`, a personal hit/miss dataset.

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

## Design and quality

0.6.0 redesigns every screen with Material 3: a home hub (progress, tools,
next exercise), a dedicated Recorder, a gauge-based Tuner and a guided
Practice flow with an end-of-run summary. Light and dark themes, a
four-size type scale, an 8 dp grid, edge-to-edge insets on every screen
and screen-reader labels throughout. Quality is tracked against
ISO/IEC 25010:2023 in [docs/QUALITY.md](docs/QUALITY.md).

`ScreenRenderTest` (Robolectric) launches every screen and writes
renders to `app/build/screenshots/` on each test run.

## Architecture

Top-level packages under `com.example.wavefret`:

- **`common`** — generic infrastructure with no knowledge of any one
  feature: `audio` (PCM sources, YIN pitch detection, low-pass filter,
  silence gate, metronome, playback capture), `music` (notes, cents, fret
  frequencies, bass tunings), `transcription`, `textrecognition`,
  `appearance` (theme, text size), `ui`, `time`, `permission`, `storage`.
- **`settings`** — the Settings screen.
- **`recording`** — recording, listing, playback and deletion of clips.
- **`tuner`** — the live tuner pipeline and needle display.
- **`practice`** — the tab model and text-tab parser, built-in
  exercises, wait/tempo practice sessions, the practice pipeline and the
  scrolling tab display; `practice.songimport` holds the ASCII-tab and
  MusicXML importers and the imported-songs repository;
  `practice.progression` holds difficulty scoring, practice history and
  recommendations.

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
- **v3** — Song import (done): text tabs and MusicXML. Audio
  transcription from YouTube via Demucs + basic-pitch is deferred: both
  are Python ML models that need a desktop/server step, not on-device code.
- **v4** — Difficulty scoring and progressive song ordering (done):
  heuristic over tempo, position shifts, string crossings, neck height
  and fret jumps. Rhythm isn't in the tab model yet, so it isn't scored.
