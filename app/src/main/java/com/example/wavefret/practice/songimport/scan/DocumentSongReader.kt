package com.example.wavefret.practice.songimport.scan

import android.content.ContentResolver
import android.net.Uri
import com.example.wavefret.common.imaging.toBinaryImage
import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.textrecognition.DocumentPageRenderer
import com.example.wavefret.common.textrecognition.MlKitImageSymbolRecognizer
import com.example.wavefret.common.textrecognition.MlKitSymbolRecognizer
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.songimport.RecognizedTabTextBuilder

/**
 * Reads the bass part of a PDF or photo: every page is turned into black
 * and white and read by the [ScoreImageReader] (drawn tab staffs, or sheet
 * music); pages without staffs are read as text, for tabs written as text.
 * Everything runs on the phone.
 *
 * Android framework glue around the tested [ScoreImageReader] and
 * [ScannedSongAssembler]; verified on-device.
 *
 * @property contentResolver Opens the document. Type: ContentResolver
 * @property tuning Tuning of the bass. Type: BassTuning
 */
class DocumentSongReader(private val contentResolver: ContentResolver, private val tuning: BassTuning) {

    /**
     * @param documentUri PDF or image. Type: Uri
     * @param isPdf True for a PDF. Type: Boolean
     * @return What was read. Type: ScannedSong
     * @throws java.io.IOException If the document can't be opened.
     */
    suspend fun read(documentUri: Uri, isPdf: Boolean): ScannedSong {
        val pageReadings = mutableListOf<PageReading>()
        val textPages = mutableListOf<List<RecognizedSymbol>>()
        MlKitSymbolRecognizer().use { symbolRecognizer ->
            val scoreImageReader = ScoreImageReader(tuning, MlKitImageSymbolRecognizer(symbolRecognizer))
            DocumentPageRenderer(contentResolver).forEachPage(documentUri, isPdf) { pageBitmap ->
                val pageReading = scoreImageReader.read(pageBitmap.toBinaryImage())
                pageReadings += pageReading
                if (pageReading.tabSystems.isEmpty() && pageReading.notationSystems.isEmpty()) {
                    textPages += symbolRecognizer.recognize(pageBitmap)
                }
            }
        }
        return ScannedSongAssembler(tuning).assemble(pageReadings, RecognizedTabTextBuilder().build(textPages))
    }

}
