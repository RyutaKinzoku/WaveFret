# WaveFret quality model (ISO/IEC 25010:2023)

This document maps each product-quality characteristic of ISO/IEC 25010:2023
to what WaveFret does about it, how that is verified, and what is still
open. It is updated with every release; the current state is **0.14.2**.

Verification legend: **UT** = JUnit unit test, **RT** = Robolectric screen
test (`ScreenRenderTest`, renders every screen in light/dark),
**LINT** = Android lint (must be clean), **DEV** = manual on-device check.

---

## 1. Functional suitability

| Sub-characteristic | How WaveFret meets it | Verified by |
|---|---|---|
| Functional completeness | Every roadmap feature v0-v4 ships: recorder, tuner, practice (wait/tempo), song import (ASCII tab, MusicXML/.mxl), difficulty and progression. | UT per feature package |
| Functional correctness | Pitch matching ±40 cents, YIN with harmonic-lock rejection, hit confirmation that refuses to count a sustained note twice, beat windows of ±½ beat, streaks computed in local time. | UT (300+ tests, all green at every GREEN commit) |
| Functional correctness (transcription) | Note decoding, windowing, resampling and tempo estimation are unit-tested against synthetic signals and synthetic model outputs; a harmonic-rich bass line is transcribed end to end. Real-song accuracy is not yet measured. | UT, DEV pending |
| Functional correctness (0.11) | A note on the same string as the previous one, or a repeated note, only counts after a detected pluck attack, so ringing strings and fading tails can't play the lesson for you. | UT (`AttackDetector`, `HitConfirmer`, sessions, `PracticeController`) |
| Functional correctness (0.12) | Finished lessons are never recommended again; the next suggestion is the easiest unfinished one. Re-plucked notes stay separate notes, quiet recordings are transcribed, full songs keep only the bass line. Pasted, photographed and web tabs are normalised before parsing. | UT (`ExerciseProgression`, `PracticeSummaryBuilder`, `MonophonicTranscriber`, `BassLineExtractor`, `AsciiTabParser`, `TextGridBuilder`, `TabLineGapFiller`) |
| Functional correctness (0.13) | Drawn tab staffs and bass-clef sheet music are read from page images; video tabs are merged across frames; a tab lesson is lined up with a recording note by note (exact pitch, then octave) so play-along uses the tab's notes. Checked with drawn test pages, synthetic frames and note sequences, and the staff finder against a real screenshot of a Guitar Pro PDF. | UT (`StaffFinder`, `StaffLineEraser`, `DrawnTabReader`, `NotationStaffReader`, `ScoreImageReader`, `ScannedSongAssembler`, `VideoTabCollector`, `VideoTabLessonBuilder`, `LessonSynchronizer`, `AsciiTabParser`) |
| Functional correctness (0.14) | Clefs are told apart and only bass-clef staffs of piano/vocal scores are read; the clef, key signature and time signature are skipped and key signatures follow the standard order; light-on-dark video tabs are read; the synthesized bass sounds at the exact pitch of each note. Checked with drawn test pages, synthetic frames, YIN on the synthesized notes, and the clef and note reading against a real 100-dpi piano/vocal PDF (all 12 bass staffs found, header symbols no longer read as notes). | UT (`ClefDetector`, `StaffHeaderFinder`, `NotationStaffReader`, `ScoreImageReader`, `LuminanceImage`, `VideoFrameTabReader`, `PluckedStringSynthesizer`, `LessonPlaybackSchedule`) |
| Functional appropriateness | The app compares against a *known* expected note instead of open transcription, the simplest reliable approach for "wait until I play it right". | Design (charter) |

## 2. Performance efficiency

| Sub-characteristic | How | Verified by |
|---|---|---|
| Time behaviour (0.10) | Tuner readings every 23 ms over 93 ms windows, with the pitch search done on a 4×-decimated signal to keep CPU low; practice readings every 46 ms. | UT (`SlidingWindowAudioSource`, `DecimatingPitchDetector`), DEV pending |
| Time behaviour | Transcription runs on a background dispatcher behind a cancellable progress dialog. Audio capture and pitch detection run on `Dispatchers.IO`; UI only renders. Practice uses 4096-sample buffers (~93 ms) so tempo mode can keep up; the tab staff redraws per frame from the beat clock, not per buffer. | UT (`PracticeController`, `Metronome` virtual time), DEV |
| Resource utilization | Decoding downmixes and resamples chunk by chunk and stops at 6 minutes (~32 MB of samples); the model file is memory-mapped and the interpreter closed after use. `onDraw` allocates nothing (layouts cached on size change); the microphone is released in `onPause`; metronome tone generator released on pause. | LINT (`DrawAllocation`), DEV |
| Capacity | Exercises and history are small text files; parsing is linear. | UT |

## 3. Compatibility

| Sub-characteristic | How | Verified by |
|---|---|---|
| Co-existence | Mic is held only while Tuner/Practice/Recorder are visible; playback stops when leaving the recorder. | DEV |
| Interoperability | Plays YouTube videos through YouTube's official IFrame Player API (no downloading, as YouTube's terms require). Imports ASCII tabs, MusicXML 4.0 (plain or `.mxl`), Guitar Pro 7+ (`.gp`) and MIDI; receives files and text from any app through the share sheet; shares takes out through the share sheet. Exports its own human-readable tab text. | UT (all parsers, `ZipArchiveReader`, `SongFileImporter`, `TabTextWriter` round trip), DEV (share sheet) |

## 4. Interaction capability (usability)

| Sub-characteristic | How | Verified by |
|---|---|---|
| Appropriateness recognizability | Home shows progress, the two tools with one-line explanations, and the next exercise. | RT, DEV |
| Learnability | New players see onboarding copy ("Tune up, then play your first exercise") and a "Start here" recommendation; each mode explains itself under the toggle; empty states explain what to do. | RT |
| Operability (0.10) | Playing softly is enough: an adaptive noise gate and a detector that listens for the expected note hear quiet plucks, so lessons don't require force. The tuner never guesses the string: you pick it, or it goes lowest first. | UT (`AdaptiveSilenceGate`, `TargetedPitchDetector`, `GuidedTuner`) |
| Operability (0.12) | Settings let the player choose light, dark or system theme and one of four text sizes on top of the phone's font size. Find a song, photo/PDF tabs and YouTube learning all happen inside the app; Listen mode tells the player not to play during step 1. | UT (`ThemeMode`, `TextSize`), RT (settings, song finder), DEV |
| Operability | Primary actions in the thumb zone (Practise, Start/Stop, Record); all tap targets ≥ 48 dp; edge-to-edge insets keep every button above the navigation bar; screen stays on while playing. | RT (primary view shown), LINT, DEV |
| User error protection | Leaving the lesson editor with unsaved edits asks first; a lesson can't lose its last note or its title. Deleting a take or song asks first; import validates input and explains failures; denied microphone access explains why and links to Settings; leaving mid-take saves the take. | DEV |
| User engagement | Play-along turns any song into a stop-and-go game with unlimited tries. |
| User engagement (progress) | Streak, weekly runs and mastery progress; end-of-run summary with rating, score, "New best" and "Mastered" badges and a one-tap next exercise (peak-end design). | UT (`PracticeStatsCalculator`, `PracticeSummaryBuilder`), RT |
| Inclusivity | Text in `sp`; colour roles meet M3 contrast in light and dark; every icon button has a spoken label; section titles are headings for TalkBack (API 28+); live regions announce feedback; tuner status is said in words, not only colour. | LINT, RT |
| User assistance | Plain-language tuner guidance ("Too low · tighten the string"); hints on every input. | DEV |
| Self-descriptiveness | Takes are titled by date instead of file names; exercises show level, tempo, note count and best score. | UT (`RecordingDisplayFormatter`) |

## 5. Reliability

| Sub-characteristic | How | Verified by |
|---|---|---|
| Faultlessness | TDD: every logic class has failing-first tests; full suite green at each commit. | UT |
| Availability | Everything except YouTube play-along works offline; no account needed. | Design |
| Fault tolerance | Corrupt imported files and history lines are skipped, not fatal; unparsable tabs/scores raise a readable message. | UT |
| Recoverability | Microphone denial is recoverable via Settings; an interrupted take is saved; history is append-only text. | DEV, UT |

## 6. Security

| Sub-characteristic | How | Verified by |
|---|---|---|
| Confidentiality (0.13) | Reading the tab in a video uses the same approved capture session as hearing it: pictures of the screen are read in memory about once a second and never saved. PDFs and photos are read on the phone. | Manifest review, DEV |
| Confidentiality (0.12) | Text recognition for photos/PDFs runs on the device (bundled ML Kit model). Hearing a video directly needs the player's approval in Android's capture prompt each time, runs only while Listen mode listens (with an ongoing notification), and the captured sound is analysed in memory and never saved. The song finder is an ordinary in-app browser; WaveFret sends nothing to the sites it opens. | Manifest review, DEV |
| Confidentiality | `RECORD_AUDIO` and `CAMERA` are requested only when a screen needs them. `INTERNET` (since 0.9) is used only to load YouTube's embedded player for a video the player chose; no WaveFret data is sent anywhere, there are no accounts, analytics or ads, and transcription runs on the device. Takes leave the device only when the player shares one: a FileProvider exposes just that file, read-only, to the app they pick. | Manifest review |
| Integrity | YouTube video ids are checked against YouTube's id format before they are placed in the player page, and the page's JavaScript bridge only receives player events. All XML (MusicXML, Guitar Pro) goes through `SafeXmlDocumentParser` (no entity expansion, no external DTDs); file names are URL-encoded so titles can't escape the storage folder; shared files are parsed, never executed. | UT |
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
- Play-along and Listen mode with YouTube have only been checked with rendered screens and unit tests, not on a phone with network access.
- Tablet / landscape layouts are not specialised yet (content stretches full width).
- English only; strings are externalized, so translation is possible but not done.
- Rhythm is not part of the tab model, so it doesn't count towards difficulty.
- Sheet music reading is experimental: rhythm isn't read; on a real 100-dpi piano/vocal PDF some rests still read as notes and one page's key signature was missed.
- Reading tabs from videos depends on the video showing a clear tab staff; tab pages that repeat exactly are only counted once.
- The synthesized bass has only been checked by measuring its pitch, not listened to on a phone.
- Text recognition, playback capture and the song finder have only been checked with unit tests and rendered screens, not on a phone.
- Transcription accuracy on real songs is still unmeasured; the new rules were checked against synthetic audio and synthetic model output.
