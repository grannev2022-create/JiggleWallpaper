package com.example.jiggle

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private lateinit var imageView: ImageView
    private var selectedBitmap: Bitmap? = null
    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "1. Выберите фото\n2. Нажмите 'Сохранить как обои'\n(Зона колыхания = центр картинки, 40% ширины)"
            textSize = 16f
        }

        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 800
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val btnPick = Button(this).apply {
            text = "Выбрать фото"
            setOnClickListener { pickImage() }
        }

        val btnSave = Button(this).apply {
            text = "Сохранить как обои"
            setOnClickListener { saveAndApply() }
        }

        layout.addView(title)
        layout.addView(btnPick)
        layout.addView(imageView)
        layout.addView(btnSave)
        setContentView(layout)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            if (uri != null) {
                val input = contentResolver.openInputStream(uri)
                selectedBitmap = BitmapFactory.decodeStream(input)
                input?.close()
                imageView.setImageBitmap(selectedBitmap)
            }
        }
    }

    private fun saveAndApply() {
        val bmp = selectedBitmap ?: run {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filesDir, "wallpaper.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val prefs = getSharedPreferences("jiggle", MODE_PRIVATE)
        prefs.edit()
            .putFloat("zone_x", 0.3f)
            .putFloat("zone_y", 0.3f)
            .putFloat("zone_w", 0.4f)
            .putFloat("zone_h", 0.4f)
            .apply()

        Toast.makeText(this, "Сохранено! Теперь: Настройки → Обои → Живые обои → Jiggle Wallpaper", Toast.LENGTH_LONG).show()

        val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                android.content.ComponentName(this@MainActivity, JiggleWallpaperService::class.java)
            )
        }
        startActivity(intent)
    }
}
