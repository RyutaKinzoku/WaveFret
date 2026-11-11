# WaveFret quality model (ISO/IEC 25010:2023)

This document maps each product-quality characteristic of ISO/IEC 25010:2023
to what WaveFret does about it, how that is verified, and what is still
open. It is updated with every release; the current state is **0.6.0**.

Verification legend: **UT** = JUnit unit test, **RT** = Robolectric screen
test (`ScreenRenderTest`, renders every screen in light/dark),
**LINT** = Android lint (must be clean), **DEV** = manual on-device check.

---

## 1. Functional suitability

| Sub-characteristic | How WaveFret meets it | Verified by |
|---|---|---|
| Functional completeness | Every roadmap feature v0-v4 ships: recorder, tuner, practice (wait/tempo), song import (ASCII tab, MusicXML/.mxl), difficulty and progression. | UT per feature package |
| Functional correctness | Pitch matching ±40 cents, YIN with harmonic-lock rejection, hit confirmation that refuses to count a sustained note twice, beat windows of ±½ beat, streaks computed in local time. | UT (300+ tests, all green at every GREEN commit) |
| Functional appropriateness | The app compares against a *known* expected note instead of open transcription, the simplest reliable approach for "wait until I play it right". | Design (charter) |

## 2. Performance efficiency

| Sub-characteristic | How | Verified by |
|---|---|---|
| Time behaviour | Audio capture and pitch detection run on `Dispatchers.IO`; UI only renders. Practice uses 4096-sample buffers (~93 ms) so tempo mode can keep up; the tab staff redraws per frame from the beat clock, not per buffer. | UT (`PracticeController`, `Metronome` virtual time), DEV |
| Resource utilization | `onDraw` allocates nothing (layouts cached on size change); the microphone is released in `onPause`; metronome tone generator released on pause. | LINT (`DrawAllocation`), DEV |
| Capacity | Exercises and history are small text files; parsing is linear. | UT |

## 3. Compatibility

| Sub-characteristic | How | Verified by |
|---|---|---|
| Co-existence | Mic is held only while Tuner/Practice/Recorder are visible; playback stops when leaving the recorder. | DEV |
| Interoperability | Imports the two common bass formats: ASCII tabs as posted online and MusicXML 4.0 (plain or `.mxl`). Exports its own human-readable tab text. | UT (`AsciiTabParser`, `MusicXmlBassParser`, `MusicXmlFileReader`, `TabTextWriter` round trip) |

## 4. Interaction capability (usability)

| Sub-characteristic | How | Verified by |
|---|---|---|
| Appropriateness recognizability | Home shows progress, the two tools with one-line explanations, and the next exercise. | RT, DEV |
| Learnability | New players see onboarding copy ("Tune up, then play your first exercise") and a "Start here" recommendation; each mode explains itself under the toggle; empty states explain what to do. | RT |
| Operability | Primary actions in the thumb zone (Practise, Start/Stop, Record); all tap targets ≥ 48 dp; edge-to-edge insets keep every button above the navigation bar; screen stays on while playing. | RT (primary view shown), LINT, DEV |
| User error protection | Deleting a take or song asks first; import validates input and explains failures; denied microphone access explains why and links to Settings; leaving mid-take saves the take. | DEV |
| User engagement | Streak, weekly runs and mastery progress; end-of-run summary with rating, score, "New best" and "Mastered" badges and a one-tap next exercise (peak-end design). | UT (`PracticeStatsCalculator`, `PracticeSummaryBuilder`), RT |
| Inclusivity | Text in `sp`; colour roles meet M3 contrast in light and dark; every icon button has a spoken label; section titles are headings for TalkBack (API 28+); live regions announce feedback; tuner status is said in words, not only colour. | LINT, RT |
| User assistance | Plain-language tuner guidance ("Too low · tighten the string"); hints on every input. | DEV |
| Self-descriptiveness | Takes are titled by date instead of file names; exercises show level, tempo, note count and best score. | UT (`RecordingDisplayFormatter`) |

## 5. Reliability

| Sub-characteristic | How | Verified by |
|---|---|---|
| Faultlessness | TDD: every logic class has failing-first tests; full suite green at each commit. | UT |
| Availability | Works fully offline; no account or network needed. | Design |
| Fault tolerance | Corrupt imported files and history lines are skipped, not fatal; unparsable tabs/scores raise a readable message. | UT |
| Recoverability | Microphone denial is recoverable via Settings; an interrupted take is saved; history is append-only text. | DEV, UT |

## 6. Security

| Sub-characteristic | How | Verified by |
|---|---|---|
| Confidentiality | Only `RECORD_AUDIO` is requested, and only when a screen needs it; no `INTERNET` permission, so nothing can leave the device; data lives in app-specific storage. | Manifest review |
| Integrity | The MusicXML parser does not expand entities or fetch external DTDs (XXE-safe); file names are URL-encoded so titles can't escape the storage folder. | UT |
| Accountability / authenticity | Not applicable: single-user, no accounts. | — |

## 7. Maintainability

| Sub-characteristic | How | Verified by |
|---|---|---|
| Modularity | `common` has no feature knowledge; `recording`, `tuner`, `practice` depend only on `common`, never on each other. | Code review |
| Reusability | Audio pipeline, pitch detection, beat clock and metronome are shared by tuner and practice. | UT |
| Analysability | KDoc on every function; meaningful names; one class, one job. | Code review |
| Modifiability | Framework classes behind small interfaces (`PcmAudioSource`, `AudioRecorder`, `Clock`, `MetronomeClickPlayer`). | UT with fakes |
| Testability | Logic is framework-free and fake-driven; screens covered by Robolectric. | UT, RT |

## 8. Flexibility (portability)

| Sub-characteristic | How | Verified by |
|---|---|---|
| Adaptability | Light and dark themes; scrollable layouts for small phones and large font sizes; tunings are data (`BassTuning`), so 5-string or drop tunings need no engine change. | RT (light/dark) |
| Installability | Standard APK, minSdk 24. | Build |
| Replaceability | Practice data is plain text (`imported_tabs/*.wavefret.txt`, `practice_results.tsv`). | UT |
| Scalability | Not applicable (local, single user). | — |

## 9. Safety

| Sub-characteristic | How | Verified by |
|---|---|---|
| Operational constraint / risk identification | Metronome at a moderate fixed volume (80% of media volume, short 40 ms clicks) through the media stream, so the system volume controls it. | DEV |
| Fail safe / hazard warning | No hazards beyond audio volume identified. | — |

---

## Known gaps

- No physical device or emulator was available when 0.6.0 was built, so the
  **DEV** rows (audio latency, mic behaviour, metronome timing, TalkBack
  walk-through) still need a manual pass on a phone.
- Tablet / landscape layouts are not specialised yet (content stretches full width).
- English only; strings are externalized, so translation is possible but not done.
- Android Gradle Plugin 9.5.1 is available (lint notice); upgrading is a separate task.
- Rhythm is not part of the tab model, so it doesn't count towards difficulty.
