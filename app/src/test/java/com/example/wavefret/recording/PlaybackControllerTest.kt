package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerTest {

    @Test
    fun initialCurrentlyPlayingFilePathIsNull() {
        val controller = PlaybackController(FakeAudioPlayer())
        assertNull(controller.currentlyPlayingFilePath())
    }

    @Test
    fun onItemClickedStartsPlaybackOfClickedFile() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.onItemClicked("/fake/dir/a.m4a")
        assertEquals("/fake/dir/a.m4a", audioPlayer.lastPlayedFilePath)
        assertEquals("/fake/dir/a.m4a", controller.currentlyPlayingFilePath())
        assertTrue(controller.isPlaying("/fake/dir/a.m4a"))
    }

    @Test
    fun onItemClickedTwiceOnSameFileStopsPlayback() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.onItemClicked("/fake/dir/a.m4a")
        controller.onItemClicked("/fake/dir/a.m4a")
        assertEquals(1, audioPlayer.stopPlaybackCallCount)
        assertEquals(1, audioPlayer.releasePlayerCallCount)
        assertNull(controller.currentlyPlayingFilePath())
    }

    @Test
    fun onItemClickedOnDifferentFileStopsOldAndPlaysNew() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.onItemClicked("/fake/dir/a.m4a")
        controller.onItemClicked("/fake/dir/b.m4a")
        assertEquals(1, audioPlayer.stopPlaybackCallCount)
        assertEquals("/fake/dir/b.m4a", audioPlayer.lastPlayedFilePath)
        assertEquals("/fake/dir/b.m4a", controller.currentlyPlayingFilePath())
        assertFalse(controller.isPlaying("/fake/dir/a.m4a"))
    }

    @Test
    fun simulatingPlaybackCompletionClearsCurrentlyPlayingFilePath() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.onItemClicked("/fake/dir/a.m4a")
        audioPlayer.simulatePlaybackCompleted()
        assertNull(controller.currentlyPlayingFilePath())
        assertEquals(1, audioPlayer.releasePlayerCallCount)
    }

    @Test
    fun stopPlaybackWhileIdleHasNoEffect() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.stopPlayback()
        assertEquals(0, audioPlayer.stopPlaybackCallCount)
    }

    @Test
    fun stopPlaybackWhilePlayingStopsAndReleases() {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer)
        controller.onItemClicked("/fake/dir/a.m4a")
        controller.stopPlayback()
        assertEquals(1, audioPlayer.stopPlaybackCallCount)
        assertEquals(1, audioPlayer.releasePlayerCallCount)
        assertNull(controller.currentlyPlayingFilePath())
    }

    @Test
    fun onPlaybackStateChangedCallbackFiresWhenPlaybackStarts() {
        var callbackInvocations = 0
        val controller = PlaybackController(FakeAudioPlayer()) { callbackInvocations++ }
        controller.onItemClicked("/fake/dir/a.m4a")
        assertEquals(1, callbackInvocations)
    }
}