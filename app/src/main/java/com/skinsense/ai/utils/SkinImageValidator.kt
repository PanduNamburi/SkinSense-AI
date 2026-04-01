package com.skinsense.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class SkinImageValidator(private val context: Context) {

    private val TAG = "SkinImageValidator"

    private var faceDetector: FaceDetector? = null
    private var handLandmarker: HandLandmarker? = null
    private var imageSegmenter: ImageSegmenter? = null

    init {
        initializeModels()
    }

    private fun initializeModels() {
        try {
            faceDetector = FaceDetector.createFromOptions(
                context,
                FaceDetector.FaceDetectorOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("blaze_face_short_range.tflite")
                            .setDelegate(Delegate.CPU)
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setMinDetectionConfidence(0.6f) // Raised from default 0.5
                    .build()
            )
            Log.d(TAG, "FaceDetector initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FaceDetector", e)
        }

        try {
            handLandmarker = HandLandmarker.createFromOptions(
                context,
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("hand_landmarker.task")
                            .setDelegate(Delegate.CPU)
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumHands(2)
                    .setMinHandDetectionConfidence(0.6f)
                    .build()
            )
            Log.d(TAG, "HandLandmarker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HandLandmarker", e)
        }

        try {
            imageSegmenter = ImageSegmenter.createFromOptions(
                context,
                ImageSegmenter.ImageSegmenterOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("selfie_segmenter.tflite")
                            .setDelegate(Delegate.CPU)
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setOutputCategoryMask(true)
                    .setOutputConfidenceMasks(false)
                    .build()
            )
            Log.d(TAG, "ImageSegmenter initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ImageSegmenter", e)
        }
    }

    suspend fun validate(bitmap: Bitmap, callback: (isValid: Boolean, reason: String) -> Unit) {
        withContext(Dispatchers.Default) {
            try {
                if (faceDetector == null && handLandmarker == null && imageSegmenter == null) {
                    initializeModels()
                    if (faceDetector == null && handLandmarker == null && imageSegmenter == null) {
                        withContext(Dispatchers.Main) {
                            callback(false, "Internal error: Vision models could not be loaded.")
                        }
                        return@withContext
                    }
                }

                val mpImage = BitmapImageBuilder(bitmap).build()

                // ── Check 1: Face Detection ──────────────────────────────────
                // A detected face is the strongest signal — always pass.
                val faceResult = faceDetector?.detect(mpImage)
                val faceCount = faceResult?.detections()?.size ?: 0
                if (faceCount > 0) {
                    Log.d(TAG, "PASS: Face detected ($faceCount face(s))")
                    withContext(Dispatchers.Main) { callback(true, "") }
                    return@withContext
                }

                // ── Check 2: Hand Detection ──────────────────────────────────
                // Hands are valid skin areas (common for rashes, eczema, etc.)
                val handResult = handLandmarker?.detect(mpImage)
                val handCount = handResult?.handednesses()?.size ?: 0
                if (handCount > 0) {
                    Log.d(TAG, "PASS: Hand detected ($handCount hand(s))")
                    withContext(Dispatchers.Main) { callback(true, "") }
                    return@withContext
                }

                // ── Check 3: Selfie/Body Segmentation ───────────────────────
                // Requires BOTH a significant human body area AND skin-like colors.
                // This double-check prevents non-human images with warm tones from passing.
                val segmenterResult = imageSegmenter?.segment(mpImage)
                val masks = segmenterResult?.categoryMask()
                if (masks?.isPresent == true) {
                    val maskImage = masks.get()
                    val totalPixels = maskImage.width * maskImage.height

                    val buffer: ByteBuffer = ByteBufferExtractor.extract(maskImage)
                    buffer.rewind()
                    val maskBytes = ByteArray(buffer.remaining())
                    buffer.get(maskBytes)

                    val humanPixels = maskBytes.count { (it.toInt() and 0xFF) > 0 }
                    val humanRatio = humanPixels.toFloat() / totalPixels.toFloat()
                    Log.d(TAG, "Selfie segmentation human ratio: ${humanRatio * 100}%")

                    // Require at least 15% of image classified as human body
                    if (humanRatio > 0.15f) {
                        if (hasSkinColorInRegion(bitmap, maskBytes, maskImage.width, maskImage.height)) {
                            Log.d(TAG, "PASS: Segmentation ${humanRatio * 100}% body + skin color confirmed")
                            withContext(Dispatchers.Main) { callback(true, "") }
                            return@withContext
                        }
                    }
                }

                // ── Check 4: High-coverage skin tone fallback ────────────────
                // For close-up skin disease images (arm, back, leg), MediaPipe often
                // misses the person since there's no face/hand and it's too close for
                // selfie segmentation. BUT a genuine close-up skin photo will have the
                // MAJORITY of its pixels as skin tones. Random images (food, landscapes,
                // objects) will NOT reach 40% skin pixel coverage.
                if (hasSkinDominant(bitmap)) {
                    Log.d(TAG, "PASS: High skin-tone coverage (close-up skin photo)")
                    withContext(Dispatchers.Main) { callback(true, "") }
                    return@withContext
                }

                // ── FINAL REJECTION ──────────────────────────────────────────
                Log.d(TAG, "REJECT: No face, hand, or body with skin tones detected")
                withContext(Dispatchers.Main) {
                    callback(
                        false,
                        "This doesn't look like a skin image. Please upload a clear photo of your skin (face, hands, or affected body area)."
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during validation", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error analyzing image. Please try again.")
                }
            }
        }
    }

    /**
     * Checks if the image is dominated by skin tones — used as a fallback for
     * close-up skin shots where MediaPipe can't detect face/hands/body frame.
     * Threshold of 40% means the majority of the image must be skin-colored,
     * which is realistic for a macro skin photo but not for food/landscapes.
     */
    private fun hasSkinDominant(bitmap: Bitmap): Boolean {
        val stepX = maxOf(1, bitmap.width / 60)
        val stepY = maxOf(1, bitmap.height / 60)
        var skinPixels = 0
        var totalSampled = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                if (isSkinTone(r, g, b)) skinPixels++
                totalSampled++
            }
        }

        val skinRatio = skinPixels.toFloat() / totalSampled.toFloat()
        Log.d(TAG, "Dominant skin color ratio: ${skinRatio * 100}%")
        // 40% threshold: genuine close-up skin photos will easily exceed this,
        // while random images rarely have 40%+ skin-tone pixels
        return skinRatio > 0.40f
    }

    /**
     * Checks whether pixels identified as human by the segmenter
     * actually contain skin-like colors.
     */
    private fun hasSkinColorInRegion(
        bitmap: Bitmap,
        maskBytes: ByteArray,
        maskWidth: Int,
        maskHeight: Int
    ): Boolean {
        // Scale factors in case bitmap and mask have different dimensions
        val scaleX = bitmap.width.toFloat() / maskWidth.toFloat()
        val scaleY = bitmap.height.toFloat() / maskHeight.toFloat()

        var skinPixels = 0
        var humanMaskPixels = 0
        val step = 4 // Sample every 4th pixel for performance

        for (my in 0 until maskHeight step step) {
            for (mx in 0 until maskWidth step step) {
                val maskIdx = my * maskWidth + mx
                if (maskIdx >= maskBytes.size) continue
                val isMaskHuman = (maskBytes[maskIdx].toInt() and 0xFF) > 0
                if (!isMaskHuman) continue

                humanMaskPixels++

                val bx = (mx * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                val by = (my * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(bx, by)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)

                if (isSkinTone(r, g, b)) skinPixels++
            }
        }

        if (humanMaskPixels == 0) return false
        val skinRatio = skinPixels.toFloat() / humanMaskPixels.toFloat()
        Log.d(TAG, "Skin color ratio within human mask: ${skinRatio * 100}%")

        // At least 20% of the detected human pixels must look like skin tone
        return skinRatio > 0.20f
    }

    /**
     * Strict multi-range skin tone detector covering all ethnicities.
     * Uses both RGB and YCbCr-inspired logic.
     */
    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        // --- Range 1: Classic RGB skin heuristic (works for lighter/medium tones) ---
        val rgbSkin = r > 95 && g > 40 && b > 20
                && r > g && r > b
                && (r - g) > 15
                && (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) > 15

        // --- Range 2: YCbCr-based heuristic (robust across ethnicities) ---
        // Convert to YCbCr
        val y  =  0.299f  * r + 0.587f  * g + 0.114f  * b
        val cb = -0.1687f * r - 0.3313f * g + 0.5f    * b + 128f
        val cr =  0.5f    * r - 0.4187f * g - 0.0813f * b + 128f

        val ycbcrSkin = y > 80f
                && cb in 77f..127f
                && cr in 133f..173f

        return rgbSkin || ycbcrSkin
    }

    fun close() {
        faceDetector?.close()
        handLandmarker?.close()
        imageSegmenter?.close()
    }
}
