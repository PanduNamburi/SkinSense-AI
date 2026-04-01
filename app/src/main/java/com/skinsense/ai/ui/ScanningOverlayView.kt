package com.skinsense.ai.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class ScanningOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint()
    private val eraser = Paint()
    private val boxPaint = Paint()
    private val rect = RectF()

    init {
        paint.color = Color.parseColor("#99000000") // Semi-transparent black
        paint.style = Paint.Style.FILL

        eraser.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        boxPaint.color = Color.WHITE
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 8f
        boxPaint.pathEffect = DashPathEffect(floatArrayOf(50f, 20f), 0f) // Dashed line for "scanning" effect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the semi-transparent background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Calculate the center square
        val size = width.coerceAtMost(height) * 0.7f
        val cx = width / 2f
        val cy = height / 2f
        rect.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)

        // Cut out the hole (clear mechanism)
        // We need to draw the background on a hardware layer or software layer to use CLEAR safely
        // But for simplicity, we can just draw 4 rectangles around the center.
        // Or better, use layer save/restore
        
        // Actually, easiest way to do "cut out" is drawing 4 rects around the transparent area
        // Top
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, paint)
        // Bottom
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), paint)
        // Left
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, paint)
        // Right
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, paint)
        
        // Draw the framing box
        canvas.drawRect(rect, boxPaint)
    }
}
