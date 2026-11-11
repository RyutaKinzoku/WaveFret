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
