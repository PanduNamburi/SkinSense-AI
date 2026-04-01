package com.skinsense.ai.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Prediction(val label: String, val confidence: Float)

class DiseaseClassifier(private val context: Context) {

    private var interpreter: InterpreterApi? = null
    
    // TODO: Verify the exact 11 labels with the user or load from a labels.txt file. 
    
    private var labelList: List<String> = emptyList()

    companion object {
        private const val MODEL_PATH = "skin_model_final.tflite"
        private const val LABELS_PATH = "labels.txt" // Assuming a labels file exists
        private const val INPUT_SIZE = 300
    }

    init {
        // Initialize in background to avoid blocking main thread if called there, 
        // though init is usually called in onCreate. 
        // We will do lazy init in classify or run a setup method immediately if possible.
        // For now, launch a coroutine scope would be better, but we don't have one here.
        // We rely on suspend classify to init or setupClassifier being called safely.
        // setupClassifier() // Removing explicit call in init to avoid strict mode violations or waiting on main thread
    }

    private fun setupClassifier() {
        try {
            Log.d("DiseaseClassifier", "Initializing TFLite (Play Services)...")
            // Initialize TFLite runtime
            Tasks.await(TfLite.initialize(context))
            Log.d("DiseaseClassifier", "TFLite initialized.")

            val modelFile = FileUtil.loadMappedFile(context, MODEL_PATH)
            val options = InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            interpreter = InterpreterApi.create(modelFile, options)
            labelList = FileUtil.loadLabels(context, LABELS_PATH)
            Log.d("DiseaseClassifier", "Model loaded successfully. Labels: ${labelList.size}")
        } catch (e: Exception) {
            Log.e("DiseaseClassifier", "Error initializing classifier", e)
            System.err.println("Error initializing classifier message: " + e.message)
            e.printStackTrace()
        }
    }

    suspend fun classify(bitmap: Bitmap): List<Prediction> = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            setupClassifier()
            if (interpreter == null) {
                Log.e("DiseaseClassifier", "Interpreter is null after setup attempt")
                return@withContext emptyList<Prediction>()
            }
        }

        // 1. Image Preprocessing
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 1f)) // User requested logic: (pixel - 0) / 1 = no change [0, 255]
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Inference
        // Adjust output buffer shape based on model output. 
        // Assuming output is [1, num_classes]
        val outputShape = interpreter!!.getOutputTensor(0).shape() // e.g., [1, 11]
        val numClasses = outputShape[1]
        
        // Using a raw buffer for output
        val outputArray = Array(1) { FloatArray(numClasses) }
        interpreter?.run(tensorImage.buffer, outputArray)

        // 3. Post-processing
        val result = outputArray[0]
        
        // Log raw probabilities for debugging
        Log.d("DiseaseClassifier", "Raw probs: ${result.joinToString(", ")}")

        val predictions = result.mapIndexed { index, confidence ->
            Prediction(
                label = if (index < labelList.size) labelList[index] else "Unknown",
                confidence = confidence
            )
        }.sortedByDescending { it.confidence }

        return@withContext predictions
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
