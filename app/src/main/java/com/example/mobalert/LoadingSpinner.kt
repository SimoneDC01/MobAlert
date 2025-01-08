package com.example.mobalert
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class LoadingSpinner(context: Context) : View(context) {

    private val paint = Paint().apply {
        color = R.color.navbar.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private var startAngle = 0f
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = Math.min(width, height) / 8f - 20 // Dividi per 4 invece di 2
        oval.set(
            width / 2f - size,
            height / 2f - size,
            width / 2f + size,
            height / 2f + size
        )
        canvas.drawArc(oval, startAngle, 270f, false, paint)

        startAngle += 10 // Rotazione della rotella
        if (startAngle >= 360) startAngle = 0f

        // Invalida la vista per ridisegnarla
        invalidate()
    }
}

