package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.common.transcription.TempoEstimator
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import com.example.wavefret.practice.songimport.LessonSynchronizer

/**
 * Makes a play-along lesson from a video lesson: the notes come from the
 * tab shown in the video (reliable), and their timing from the song's
 * sound, lined up note by note. When the sound can't be lined up, the
 * times each part of the tab appeared on screen are used instead.
 *
 * @property tuning Tuning of the bass. Type: BassTuning
 * @property minimumNotes Fewer notes read from the video don't make a lesson. Type: Int
 */
class VideoTabLessonBuilder(private val tuning: BassTuning, private val minimumNotes: Int = DEFAULT_MINIMUM_NOTES) {

    private val fretboardPositionFinder = FretboardPositionFinder(tuning)
    private val lessonSynchronizer = LessonSynchronizer(fretboardPositionFinder)

    /**
     * @param title Lesson title. Type: String
     * @param videoTabCollector Tab read from the video's frames. Type: VideoTabCollector
     * @param heardNotes Notes transcribed from the video's sound, in video time. Type: List<NoteEvent>
     * @param mediaSource The video. Type: LessonMediaSource
     * @return The lesson, or null if too little tab was read. Type: Exercise?
     */
    fun build(title: String, videoTabCollector: VideoTabCollector, heardNotes: List<NoteEvent>, mediaSource: LessonMediaSource): Exercise? {
        val columns = videoTabCollector.columns
        if (columns.size < minimumNotes) return null
        val openStrings = fretboardPositionFinder.openStringMidiNumbers()
        val notes = columns.map { column -> column.minBy { openStrings[it.stringIndex] + it.fret } }
        val pageOnsets = videoTabCollector.columnOnsetsMillis()
        val tabLesson = Exercise(
            title = title,
            tuning = tuning,
            tempoBpm = TempoEstimator().estimateBpm(pageOnsets),
            notes = notes,
            noteOnsetsMillis = pageOnsets,
            mediaSource = mediaSource
        )
        val syncedLesson = runCatching { lessonSynchronizer.synchronize(tabLesson, heardNotes, mediaSource) }.getOrNull()
            ?: return tabLesson
        return syncedLesson.copy(tempoBpm = TempoEstimator().estimateBpm(syncedLesson.noteOnsetsMillis.orEmpty()))
    }

    private companion object {
        const val DEFAULT_MINIMUM_NOTES = 3
    }
}
