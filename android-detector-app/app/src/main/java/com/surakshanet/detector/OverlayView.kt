package com.surakshanet.detector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import org.tensorflow.lite.task.vision.detector.Detection

class OverlayView(context: Context) : View(context) {

    private var currentTti: Float = 0f

    private var results: List<Detection> = emptyList()

    private var imageWidth = 0
    private var imageHeight = 0

    private val boxPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        style = Paint.Style.FILL
    }

    fun setResults(
        results: List<Detection>,
        imageWidth: Int,
        imageHeight: Int,
        tti: Float
    ) {
        this.results = results
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.currentTti = tti
        invalidate()
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        if (results.isEmpty()) return
        if (imageWidth == 0 || imageHeight == 0) return

        // Because the phone is rotated 90° left
        val scaleX = width.toFloat() / imageHeight
        val scaleY = height.toFloat() / imageWidth

        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.style = Paint.Style.FILL

        for (detection in results) {

            val box = detection.boundingBox

            val rotatedBox = RectF(
                box.top,
                imageWidth - box.right,
                box.bottom,
                imageWidth - box.left
            )

            val left = rotatedBox.left * scaleX
            val top = rotatedBox.top * scaleY
            val right = rotatedBox.right * scaleX
            val bottom = rotatedBox.bottom * scaleY

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val label = detection.categories.firstOrNull()?.label ?: "Vehicle"
            val score = detection.categories.firstOrNull()?.score ?: 0f

            val text = "$label ${(score * 100).toInt()}%"

            canvas.drawText(text, left, top - 10, textPaint)

            textPaint.color = when {
                currentTti > 6f -> Color.GREEN
                currentTti > 4f -> Color.YELLOW
                else -> Color.RED
            }

            canvas.drawText(
                "TTI: %.2fs".format(currentTti),
                left,
                top - 40,
                textPaint
            )
        }
    }
}