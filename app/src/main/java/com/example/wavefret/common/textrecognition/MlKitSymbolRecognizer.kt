package com.example.wavefret.common.textrecognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Finds the characters in an image with Google's ML Kit text recognition,
 * which runs entirely on the device, offline and free of charge (the
 * model ships inside the app).
 *
 * Android framework glue, verified on-device; what is done with the
 * symbols is tested in [TextGridBuilder].
 */
class MlKitSymbolRecognizer : Closeable {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * @param image Photo or rendered page. Type: Bitmap
     * @return Every recognized character with its box. Type: List<RecognizedSymbol>
     */
    suspend fun recognize(image: Bitmap): List<RecognizedSymbol> {
        val recognizedText = suspendCancellableCoroutine { continuation ->
            textRecognizer.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        return recognizedText.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .flatMap { element ->
                val symbols = element.symbols.mapNotNull { symbol ->
                    symbol.boundingBox?.let { RecognizedSymbol(symbol.text, it.left, it.top, it.right, it.bottom) }
                }
                symbols.ifEmpty {
                    listOfNotNull(element.boundingBox?.let { RecognizedSymbol(element.text, it.left, it.top, it.right, it.bottom) })
                }
            }
    }

    /**
     * @return Unit
     */
    override fun close() {
        textRecognizer.close()
    }
}
