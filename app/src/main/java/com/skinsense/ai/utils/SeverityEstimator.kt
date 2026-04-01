package com.skinsense.ai.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.skinsense.ai.ui.compose.SeverityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Severity Estimator
 * Analyzes the affected skin area to determine severity
 */
object SeverityEstimator {

    private const val TAG = "SeverityEstimator"

    /**
     * Estimates severity based on lesion area percentage
     */
    suspend fun estimateSeverity(bitmap: Bitmap): Pair<SeverityLevel, Float> = withContext(Dispatchers.Default) {
        try {
            // Resize for faster processing
            val scaledBitmap = if (bitmap.width > 200 || bitmap.height > 200) {
                Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            } else {
                bitmap
            }

            var skinPixels = 0
            var lesionPixels = 0

            for (y in 0 until scaledBitmap.height) {
                for (x in 0 until scaledBitmap.width) {
                    val pixel = scaledBitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    if (isSkinTone(r, g, b)) {
                        skinPixels++
                        if (isLesion(r, g, b)) {
                            lesionPixels++
                        }
                    }
                }
            }

            if (skinPixels == 0) return@withContext SeverityLevel.MILD to 0f

            val percentage = (lesionPixels.toFloat() / skinPixels.toFloat()) * 100f
            Log.d(TAG, "Lesion analysis: Skin=$skinPixels, Lesion=$lesionPixels, Percentage=$percentage%")

            val level = when {
                percentage > 15f -> SeverityLevel.SEVERE
                percentage > 5f -> SeverityLevel.MODERATE
                else -> SeverityLevel.MILD
            }

            return@withContext level to percentage
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating severity", e)
            SeverityLevel.MILD to 0f
        }
    }

    /**
     * Reuse skin tone detection logic
     */
    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        val rgbSkin = r > 95 && g > 40 && b > 20
                && r > g && r > b
                && (r - g) > 15
                && (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) > 15

        val y  =  0.299f  * r + 0.587f  * g + 0.114f  * b
        val cb = -0.1687f * r - 0.3313f * g + 0.5f    * b + 128f
        val cr =  0.5f    * r - 0.4187f * g - 0.0813f * b + 128f

        val ycbcrSkin = y > 80f && cb in 77f..127f && cr in 133f..173f

        return rgbSkin || ycbcrSkin
    }

    /**
     * Heuristic for lesion detection:
     * Lesions often have different saturation or contrast compared to healthy skin.
     * This is a simplified color-based segmentation.
     */
    private fun isLesion(r: Int, g: Int, b: Int): Boolean {
        // Lesions often appear more red, darker, or have high contrast
        // A simple heuristic: High Color difference or deviation from typical skin hue
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        
        // Hue: 0-360, Saturation: 0-1, Value: 0-1
        // Skin typically has Hue in 0-50 range.
        // Lesions might have higher saturation or lower value (darker)
        // This is a rough heuristic and can be tuned.
        return hsv[1] > 0.4f || hsv[2] < 0.4f || (r > 150 && g < 100 && b < 100)
    }
}
