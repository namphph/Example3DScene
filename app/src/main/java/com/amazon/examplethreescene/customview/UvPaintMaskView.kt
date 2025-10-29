package com.amazon.examplethreescene.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class UvPaintMaskView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var uvBitmap: Bitmap? = null
    private var scaledUvBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null
    private var drawBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    var onBitmapUpdated: ((Bitmap) -> Unit)? = null
    private var lastUpdateTime = 0L
    private val updateDelayMs = 30L

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var wasInside = false

    fun setUvBitmap(bitmap: Bitmap) {
        uvBitmap = bitmap
        if (viewWidth > 0 && viewHeight > 0) prepareBitmaps()
    }

    private fun prepareBitmaps() {
        val uv = uvBitmap ?: return
        scaledUvBitmap = Bitmap.createScaledBitmap(uv, viewWidth, viewHeight, true)
        drawBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(drawBitmap!!)
        maskBitmap = createMask(scaledUvBitmap!!)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        if (uvBitmap != null) prepareBitmaps()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scaledUvBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        drawBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.coerceIn(0f, viewWidth.toFloat() - 1)
        val y = event.y.coerceIn(0f, viewHeight.toFloat() - 1)

        val inside = isInsideMask(x.toInt(), y.toInt())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (inside) {
                    path.reset()
                    path.moveTo(x, y)
                }
                lastX = x
                lastY = y
                wasInside = inside
            }

            MotionEvent.ACTION_MOVE -> {
                when {
                    inside && wasInside -> {
                        // Cả 2 điểm đều trong vùng → vẽ nét nối
                        path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                        drawCanvas?.drawPath(path, drawPaint)
                        notifyBitmapUpdated()
                        path.reset()
                        path.moveTo(x, y)
                    }

                    inside && !wasInside -> {
                        // Vừa đi từ ngoài vào vùng → bắt đầu nét mới
                        path.reset()
                        path.moveTo(x, y)
                    }

                    !inside && wasInside -> {
                        // Vừa đi ra ngoài vùng → ngắt nét
                        path.reset()
                    }
                }

                lastX = x
                lastY = y
                wasInside = inside
            }

            MotionEvent.ACTION_UP -> {
                if (inside) {
                    drawCanvas?.drawPath(path, drawPaint)
                    notifyBitmapUpdated()
                }
                path.reset()
                wasInside = false
            }
        }

        invalidate()
        return true
    }

    // --- kiểm tra điểm có nằm trong vùng hợp lệ không ---
    private fun isInsideMask(x: Int, y: Int): Boolean {
        val mask = maskBitmap ?: return false
        if (x < 0 || y < 0 || x >= mask.width || y >= mask.height) return false
        val pixel = mask.getPixel(x, y)
        return Color.red(pixel) > 127 // trắng = vùng hợp lệ
    }

    // --- tạo mask: vùng sáng (xám, trắng) là hợp lệ ---
    private fun createMask(bitmap: Bitmap): Bitmap {
        val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val brightness = (r + g + b) / 3
            pixels[i] = if (brightness > 50) Color.WHITE else Color.BLACK
        }
        mask.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return mask
    }

    fun clearDrawing() {
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun setDrawColor(color: Int) {
        drawPaint.color = color
    }

    fun setStrokeWidth(width: Float) {
        drawPaint.strokeWidth = width
    }

    fun getResultBitmap(): Bitmap? {
        val uv = scaledUvBitmap ?: return null
        val result = Bitmap.createBitmap(uv.width, uv.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        c.drawBitmap(uv, 0f, 0f, null)
        c.drawBitmap(drawBitmap ?: return result, 0f, 0f, null)
        return result
    }

    private fun notifyBitmapUpdated() {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime > updateDelayMs) {
            drawBitmap?.let { onBitmapUpdated?.invoke(it) }
            lastUpdateTime = now
        }
    }
}
