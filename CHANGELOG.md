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