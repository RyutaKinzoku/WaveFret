# Changelog

## [0.1.0] - v0: Recorder MVP

- **Session 1** — Project scaffold (Empty Views Activity, Kotlin, minSdk 24)
- **Session 2** — RECORD_AUDIO permission (manifest + runtime request)
- **Session 3** — Toggle Record/Stop button, status text
- **Session 4** — MediaRecorder wired to a testable RecordingSessionController
- **Session 5** — RecyclerView list of past recordings, sorted newest-first
- **Session 6** — Per-recording playback via MediaPlayer, with proper track-switching
- **Session 7** — Duration display, delete with confirmation, locked portrait orientation
- Refactor — split generic infrastructure (Clock, PermissionChecker, storage) into a `common` package, separate from `recording`-specific logic

## [0.2.0] - 2026-09-27

### Added
- Real-time bass tuner: pitch detection via a from-scratch YIN
  implementation, frequency-to-note mapping, and evaluation against
  standard 4-string bass tuning (E-A-D-G).
- Live audio pipeline (`PcmAudioSource`/`AudioRecordPcmSource`) using
  `AudioSource.UNPROCESSED` (falling back to `VOICE_RECOGNITION`) to
  avoid Android's speech noise suppression from destroying the bass
  fundamental.
- Silence gating and median-filter smoothing so the tuner ignores
  background noise and single-frame octave outliers.
- `TunerActivity`: a needle display with string name and cents-offset
  readout, launched from a new button on the main screen.
- Low-pass pre-filter (1 kHz cutoff) and harmonic-lock rejection in
  `YinPitchDetector`, fixing low-E misreads (octave/harmonic confusion
  and noise-driven dropouts) on acoustic bass.
- `TunerDisplayStabilizer`: holds the last reading on screen briefly
  after the signal drops, and only switches strings after several
  consecutive agreeing readings, so the display is readable while
  tuning instead of flickering between notes.

### Architecture
- New `tuner` package (BassString/BassTuning/BassTunings,
  TuningEvaluator, PitchSmoother, SilenceGate, TunerController,
  TunerDisplayFormatter, TunerDisplayStabilizer, TunerActivity)
  alongside `common/audio` (PitchDetector/YinPitchDetector,
  LowPassFilter, LowPassFilteredPitchDetector,
  PcmAudioSource/AudioRecordPcmSource) and `common/music`
  (NoteMapper, Cents) — all reachable only from `recording`/`tuner`,
  never the reverse.

## [0.3.0] - 2026-11-10 - v2: Practice engine

### Added
- Practice screen, opened from a new main-screen button: pick a built-in
  exercise, choose Wait or Tempo mode, and play along on a scrolling tab.
- Wait mode: the expected note is shown and the tab does not advance
  until it is heard (within 40 cents). A sustained note never counts for
  a repeat of the same note — it must be re-plucked — while a legato
  move to a different note registers immediately.
- Tempo mode: a metronome count-in, then one note per beat; each note
  can be hit within half a beat of its beat and is otherwise scored as a
  miss. Final score shown as a percentage of notes hit.
- WaveFret's own plain-text tab format (`title:`, `tempo:`, then tokens
  like `E0 A2 D5`, `#` comments) and five built-in exercises written in it.

### Architecture
- New `practice` package: TabNote, Exercise, TabTextParser,
  BuiltInExercises, NoteMatcher, HitConfirmer, ExpectedNoteJudge,
  PracticeSession with WaitModePracticeSession and
  TempoModePracticeSession, PracticeController, PracticeDisplayFormatter,
  TabStaffLayout, TabStaffView, PracticeActivity.
- New common pieces: `fretFrequencyHz`, `BeatClock`,
  `ElapsedRealtimeClock`, `Metronome`/`MetronomeClickPlayer`/
  `ToneGeneratorClickPlayer`.
- Moved BassString/BassTuning/BassTunings to `common/music` and
  SilenceGate to `common/audio`, so `practice` never depends on `tuner`.
- Moved the audio test doubles out of the shipped source set into
  `test/common/audio`.

## [0.4.0] - 2026-11-10 - v3: Song import

### Added
- "Import tab" on the practice screen: paste an ASCII bass tab as found
  online (`G|--…`, `D|--…`, `A|--…`, `E|--…` systems; surrounding text,
  lyrics and technique marks like `h`, `p`, `/`, `~` are ignored) or
  WaveFret tab text, with an optional title and tempo.
- "Open MusicXML…" in the import dialog: reads the first part of a
  `.musicxml`/`.xml` file or compressed `.mxl` archive. Tab string/fret
  markings are kept; plain notation is placed on the fretboard near the
  previous note's hand position. Rests, chord tones and tied
  continuations are skipped and `<transpose>` is applied.
- Imported songs are saved as WaveFret tab text in `imported_tabs/` and
  listed after the built-in exercises.

### Architecture
- New `practice.songimport` package: AsciiTabParser, SongImporter,
  ImportedExercisesRepository, FretboardPositionFinder,
  MusicXmlBassParser, MusicXmlFileReader.
- New TabTextWriter, the inverse of TabTextParser.

### Not included
- YouTube transcription (Demucs + basic-pitch) is deferred: it needs an
  off-device ML step. PDF/photo OCR remains ruled out.

## [0.5.0] - 2026-11-10 - v4: Difficulty & progression

### Added
- Heuristic difficulty rating (0-100, banded Beginner/Intermediate/
  Advanced) from tempo, hand-position shifts (outside a four-fret span),
  string-crossing distance, neck height and average fret jump.
- Exercise picker lists exercises easiest first, with level, best score,
  a ✓ for mastered ones and → for the recommended next exercise, which
  is selected by default.
- Every completed run (exercise, mode, score, hits, misses, time) is
  appended to `practice_results.tsv` in app storage.

### Architecture
- New `practice.progression` package: DifficultyScorer, DifficultyScore,
  DifficultyLevel, ExerciseProgression, RankedExercise, PracticeMode,
  PracticeResult, PracticeResultsRepository, ExercisePickerItem,
  ExercisePickerItemsBuilder.

## [0.6.0] - 2026-11-10 - UI/UX redesign

### Changed
- New home hub: greeting by player stage, streak / runs this week /
  mastered progress card, Tuner and Recorder cards, and the recommended
  exercise pinned as the primary action in the thumb zone.
- Recorder moved to its own screen: large record button, live timer,
  takes titled by date with play/stop and delete icon buttons, empty state,
  confirmation before delete, and an unfinished take saved when leaving.
- Tuner: cents gauge with an in-tune zone and gliding needle, E A D G
  string badges, plain-language guidance ("Too low · tighten the string").
- Practice: exercise card opening a picker sheet (level, best score,
  mastered, recommended; imported songs deletable), mode explanations,
  tab preview before starting, Start/Stop in the thumb zone, count-in
  feedback, progress bar, and an end-of-run summary sheet with rating,
  score, "New best"/"Mastered" badges, "Try again" and "Next".
- Material 3 colour roles for light and dark, new launcher icon, vector icons.

### Fixed
- Buttons hidden behind the system navigation bar: every screen now pads
  for system bars and display cutouts (edge-to-edge).
- Denied microphone permission now explains itself with a Settings shortcut.

### Added
- `PracticeStatsCalculator`, `PracticeSummaryBuilder`, `PerformanceRating`.
- Robolectric `ScreenRenderTest` (every screen, light and dark).
- `docs/QUALITY.md`: ISO/IEC 25010:2023 quality model.

## [0.7.0] - 2026-11-11 - Song library, more formats, video and sharing

### Added
- Song library (Home → Your songs → Library): search, All / My songs /
  Exercises filters, and sorting by easiest, hardest or A–Z. Every song
  shows its level, tempo, length and best score.
- "Add a song": open a file, paste a tab, or get help with sheet-music
  PDFs and photos (convert to MusicXML with MuseScore, then add it).
- New formats: Guitar Pro 7+ (`.gp`) and MIDI (`.mid`). The bass track is
  picked automatically (named "bass", bass instrument, or lowest part).
- "Share to WaveFret": tabs, MusicXML, MIDI and Guitar Pro files (and
  copied tab text) can be shared into WaveFret from any app.
- Lessons keep each note's original timing (`timing:` line in tab text),
  ready for play-along in a later version.
- Recorder: camera switch for video takes with sound (front or back
  camera, live preview), a video player, and a Share button on every take
  that opens Android's share sheet.

### Fixed
- Guitar Pro archives with unusual zip headers are now read correctly
  (`ZipArchiveReader` reads the central directory).

### Architecture
- `NoteEvent`, `TimedScore`, `NoteEventsLessonBuilder` (shared by every
  timed source), `MidiFileParser`, `GuitarProFileParser`,
  `SongFileFormatDetector`, `SongFileImporter`, `SafeXmlDocumentParser`,
  `SongLibraryQuery`, `RecordingMediaType`, `CameraXVideoRecorder`,
  `RecordingSharer`, `VideoPlayerActivity`, `SongLibraryActivity`.
- New dependency: CameraX 1.6.2.
