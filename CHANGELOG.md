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
