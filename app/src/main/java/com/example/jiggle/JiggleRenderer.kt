package com.example.jiggle

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class JiggleRenderer(
    private val context: Context,
    private val holder: SurfaceHolder,
    private val engine: WallpaperService.Engine
) : SensorEventListener, Runnable {

    private var running = false
    private var visible = true
    private var thread: Thread? = null

    private var bitmap: Bitmap? = null
    private var zoneX = 0.3f
    private var zoneY = 0.3f
    private var zoneW = 0.4f
    private var zoneH = 0.4f

    private var offsetX = 0f
    private var offsetY = 0f
    private var velX = 0f
    private var velY = 0f

    private var accX = 0f
    private var accY = 0f

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    init {
        val prefs = context.getSharedPreferences("jiggle", Context.MODE_PRIVATE)
        zoneX = prefs.getFloat("zone_x", 0.3f)
        zoneY = prefs.getFloat("zone_y", 0.3f)
        zoneW = prefs.getFloat("zone_w", 0.4f)
        zoneH = prefs.getFloat("zone_h", 0.4f)

        val file = File(context.filesDir, "wallpaper.png")
        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    fun start() {
        running = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        thread = Thread(this).also { it.start() }
    }

    fun stop() {
        running = false
        sensorManager.unregisterListener(this)
        thread?.join()
        thread = null
    }

    fun setVisible(v: Boolean) {
        visible = v
    }

    override fun onSensorChanged(event: SensorEvent) {
        accX = event.values[0]
        accY = event.values[1]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun run() {
        while (running) {
            if (visible) {
                draw()
            }
            Thread.sleep(16)
        }
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            val bmp = bitmap ?: return

            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            val scale = minOf(w / bmp.width, h / bmp.height)
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val left = (w - dw) / 2
            val top = (h - dh) / 2

            val stiffness = 0.15f
            val damping = 0.85f
            velX += accX * 0.8f
            velY += accY * 0.8f
            velX += -offsetX * stiffness
            velY += -offsetY * stiffness
            velX *= damping
            velY *= damping
            offsetX += velX
            offsetY += velY

            val maxShift = 40f
            offsetX = offsetX.coerceIn(-maxShift, maxShift)
            offsetY = offsetY.coerceIn(-maxShift, maxShift)

            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            val dstRect = RectF(left, top, left + dw, top + dh)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)

            val zx = left + dw * zoneX
            val zy = top + dh * zoneY
            val zw = dw * zoneW
            val zh = dh * zoneH

            val srcZone = Rect(
                (bmp.width * zoneX).toInt(),
                (bmp.height * zoneY).toInt(),
                (bmp.width * (zoneX + zoneW)).toInt(),
                (bmp.height * (zoneY + zoneH)).toInt()
            )
            val dstZone = RectF(zx + offsetX, zy + offsetY, zx + zw + offsetX, zy + zh + offsetY)

            canvas.drawBitmap(bmp, srcZone, dstZone, null)

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}
