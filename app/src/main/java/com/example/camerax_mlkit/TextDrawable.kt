package com.example.camerax_mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.text.Text.TextBlock

class TextDrawable(private val textBlock: TextBlock) : Drawable() {
    private var text: String = textBlock.text

    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }
    fun updateText(newText: String) {
        text = newText
        // Update the drawing logic to reflect the new text
    }

    private val contentPadding = 25
    private var textWidth = contentTextPaint.measureText(textBlock.text).toInt()
    private val boundingBox = textBlock.boundingBox!!

    override fun draw(canvas: Canvas) {
        canvas.drawRect(boundingBox, boundingRectPaint)
        canvas.drawRect(
            Rect(
                boundingBox.left,
                boundingBox.bottom + contentPadding/2,
                boundingBox.left + textWidth + contentPadding*2,
                boundingBox.bottom + contentTextPaint.textSize.toInt() + contentPadding),
            contentRectPaint
        )
        canvas.drawText(
            text,
            (boundingBox.left + contentPadding).toFloat(),
            (boundingBox.bottom + contentPadding*2).toFloat(),
            contentTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
