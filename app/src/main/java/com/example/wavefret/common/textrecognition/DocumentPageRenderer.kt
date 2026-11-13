package com.example.wavefret.common.textrecognition

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import androidx.core.graphics.createBitmap
import java.io.IOException

/**
 * Turns a PDF or an image file into page bitmaps for text recognition,
 * one page at a time so long PDFs don't fill the memory.
 *
 * Android framework glue, verified on-device.
 *
 * @property contentResolver Opens the file. Type: ContentResolver
 * @property maximumPages Pages read from a PDF at most. Type: Int
 * @property pageWidthPixels Width PDF pages are rendered at. Type: Int
 * @property maximumImageSidePixels Longest side images are scaled down to. Type: Int
 */
class DocumentPageRenderer(
    private val contentResolver: ContentResolver,
    private val maximumPages: Int = DEFAULT_MAXIMUM_PAGES,
    private val pageWidthPixels: Int = DEFAULT_PAGE_WIDTH_PIXELS,
    private val maximumImageSidePixels: Int = DEFAULT_MAXIMUM_IMAGE_SIDE_PIXELS
) {

    /**
     * @param documentUri PDF or image to read. Type: Uri
     * @param isPdf True for a PDF, false for an image. Type: Boolean
     * @param onPage Called with each page, first page first; the bitmap is recycled afterwards. Type: suspend (Bitmap) -> Unit
     * @return Unit
     * @throws IOException If the file can't be opened or decoded.
     */
    suspend fun forEachPage(documentUri: Uri, isPdf: Boolean, onPage: suspend (Bitmap) -> Unit) {
        if (isPdf) forEachPdfPage(documentUri, onPage) else onImage(documentUri, onPage)
    }

    /**
     * @param pdfUri PDF to render. Type: Uri
     * @param onPage Called with each rendered page. Type: suspend (Bitmap) -> Unit
     * @return Unit
     */
    private suspend fun forEachPdfPage(pdfUri: Uri, onPage: suspend (Bitmap) -> Unit) {
        val fileDescriptor = contentResolver.openFileDescriptor(pdfUri, "r") ?: throw IOException("Can't open the PDF")
        fileDescriptor.use { openDescriptor ->
            PdfRenderer(openDescriptor).use { pdfRenderer ->
                for (pageIndex in 0 until minOf(pdfRenderer.pageCount, maximumPages)) {
                    val pageBitmap = pdfRenderer.openPage(pageIndex).use { page ->
                        val pageHeightPixels = (pageWidthPixels.toLong() * page.height / page.width.coerceAtLeast(1)).toInt()
                        createBitmap(pageWidthPixels, pageHeightPixels.coerceAtLeast(1)).also { bitmap ->
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        }
                    }
                    try {
                        onPage(pageBitmap)
                    } finally {
                        pageBitmap.recycle()
                    }
                }
            }
        }
    }

    /**
     * @param imageUri Image to decode, upright and scaled down. Type: Uri
     * @param onPage Called once with the image. Type: suspend (Bitmap) -> Unit
     * @return Unit
     */
    private suspend fun onImage(imageUri: Uri, onPage: suspend (Bitmap) -> Unit) {
        val imageBitmap = decodeImage(imageUri) ?: throw IOException("Can't read the image")
        try {
            onPage(imageBitmap)
        } finally {
            imageBitmap.recycle()
        }
    }

    /**
     * @param imageUri Image to decode. Type: Uri
     * @return The decoded image, or null if it can't be read. Type: Bitmap?
     */
    private fun decodeImage(imageUri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, imageUri)
            return ImageDecoder.decodeBitmap(source) { decoder, imageInfo, _ ->
                val longestSide = maxOf(imageInfo.size.width, imageInfo.size.height)
                if (longestSide > maximumImageSidePixels) {
                    val scale = maximumImageSidePixels.toDouble() / longestSide
                    decoder.setTargetSize((imageInfo.size.width * scale).toInt(), (imageInfo.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        var sampleSize = 1
        while (maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / sampleSize > maximumImageSidePixels) sampleSize *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private companion object {
        const val DEFAULT_MAXIMUM_PAGES = 12
        const val DEFAULT_PAGE_WIDTH_PIXELS = 2000
        const val DEFAULT_MAXIMUM_IMAGE_SIDE_PIXELS = 3000
    }
}
