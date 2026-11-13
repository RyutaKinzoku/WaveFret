package com.example.wavefret.common.imaging

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import java.io.Closeable

/**
 * Takes still pictures of the phone's screen through an approved
 * [MediaProjection], for reading what a video shows (such as a tab).
 * Pictures stay in memory and are never saved.
 *
 * Thin wrapper over the Android display framework, verified on-device.
 *
 * @param mediaProjection The approved capture session. Type: MediaProjection
 * @param widthPixels Screen width. Type: Int
 * @param heightPixels Screen height. Type: Int
 * @param densityDpi Screen density. Type: Int
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ScreenFrameSource(
    mediaProjection: MediaProjection,
    private val widthPixels: Int,
    private val heightPixels: Int,
    densityDpi: Int
) : Closeable {

    @SuppressLint("WrongConstant")
    private val imageReader = ImageReader.newInstance(widthPixels, heightPixels, PixelFormat.RGBA_8888, MAXIMUM_IMAGES)
    private val virtualDisplay: VirtualDisplay? = mediaProjection.createVirtualDisplay(
        DISPLAY_NAME, widthPixels, heightPixels, densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, null
    )

    /**
     * @param region Part of the screen to keep, in screen pixels. Type: Rect
     * @return The latest picture of that part of the screen, or null if none is ready. Type: Bitmap?
     */
    fun latestFrame(region: Rect): Bitmap? {
        val image = imageReader.acquireLatestImage() ?: return null
        return image.use { screenImage ->
            val plane = screenImage.planes[0]
            val rowPaddingPixels = (plane.rowStride - plane.pixelStride * widthPixels) / plane.pixelStride
            val paddedScreen = createBitmap(widthPixels + rowPaddingPixels, heightPixels)
            paddedScreen.copyPixelsFromBuffer(plane.buffer)
            val clampedRegion = Rect(region).apply { if (!intersect(0, 0, widthPixels, heightPixels)) setEmpty() }
            if (clampedRegion.isEmpty) {
                paddedScreen.recycle()
                return@use null
            }
            Bitmap.createBitmap(paddedScreen, clampedRegion.left, clampedRegion.top, clampedRegion.width(), clampedRegion.height())
                .also { if (it != paddedScreen) paddedScreen.recycle() }
        }
    }

    /**
     * @return Unit
     */
    override fun close() {
        virtualDisplay?.release()
        imageReader.close()
    }

    private companion object {
        const val DISPLAY_NAME = "WaveFretVideoFrames"
        const val MAXIMUM_IMAGES = 2
    }
}
