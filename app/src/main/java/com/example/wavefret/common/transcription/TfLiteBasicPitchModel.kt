package com.example.wavefret.common.transcription

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Runs the bundled basic-pitch network (`assets/basic_pitch/nmp.tflite`)
 * on-device with LiteRT. Outputs are recognised by shape: the 264-bin
 * pitch contour is ignored and the two 88-bin maps are returned for
 * [BasicPitchOutputRoles] to tell apart.
 *
 * Thin wrapper over LiteRT (native code, not runnable in JUnit), verified
 * on-device; the maths around it is tested in [BasicPitchTranscriber].
 *
 * @param context Context used to open the model asset. Type: Context
 */
class TfLiteBasicPitchModel(context: Context) : BasicPitchModel, AutoCloseable {

    private val interpreter = Interpreter(loadModel(context))
    private val eightyEightBinOutputIndices = (0 until interpreter.outputTensorCount).filter { outputIndex ->
        interpreter.getOutputTensor(outputIndex).shape().last() == PITCH_BINS
    }
    private val contourOutputIndex = (0 until interpreter.outputTensorCount).first { it !in eightyEightBinOutputIndices }

    init {
        check(eightyEightBinOutputIndices.size == 2) { "Unexpected basic-pitch model outputs" }
    }

    /**
     * @param window Exactly [BasicPitchWindowing.WINDOW_SAMPLES] mono samples at 22 050 Hz. Type: FloatArray
     * @return The two 88-bin maps for this window. Type: BasicPitchPrediction
     */
    override fun predict(window: FloatArray): BasicPitchPrediction {
        val input = arrayOf(Array(window.size) { sampleIndex -> floatArrayOf(window[sampleIndex]) })
        val outputs = mutableMapOf<Int, Any>()
        eightyEightBinOutputIndices.forEach { outputIndex ->
            outputs[outputIndex] = arrayOf(Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(PITCH_BINS) })
        }
        val contourShape = interpreter.getOutputTensor(contourOutputIndex).shape()
        outputs[contourOutputIndex] = arrayOf(Array(contourShape[1]) { FloatArray(contourShape[2]) })
        interpreter.runForMultipleInputsOutputs(arrayOf<Any>(input), outputs)
        @Suppress("UNCHECKED_CAST")
        val maps = eightyEightBinOutputIndices.map { (outputs.getValue(it) as Array<Array<FloatArray>>)[0] }
        return BasicPitchPrediction(firstMap = maps[0], secondMap = maps[1])
    }

    /**
     * Frees the native interpreter.
     *
     * @return Unit
     */
    override fun close() = interpreter.close()

    private companion object {
        const val MODEL_ASSET_PATH = "basic_pitch/nmp.tflite"
        const val PITCH_BINS = 88

        /**
         * @param context Context used to open the asset. Type: Context
         * @return The model file, memory-mapped. Type: MappedByteBuffer
         */
        fun loadModel(context: Context): MappedByteBuffer {
            context.assets.openFd(MODEL_ASSET_PATH).use { assetFileDescriptor ->
                FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
                    return inputStream.channel.map(
                        FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength
                    )
                }
            }
        }
    }
}
