package com.example.jiggle

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class JiggleWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return JiggleEngine()
    }

    inner class JiggleEngine : Engine() {
        private var renderer: JiggleRenderer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            renderer = JiggleRenderer(this@JiggleWallpaperService, holder, this)
            renderer?.start()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            renderer?.stop()
            renderer = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderer?.setVisible(visible)
        }
    }
}
