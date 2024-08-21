package org.elixir.essence.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.app.WallpaperManager
import android.util.AttributeSet
import android.widget.ImageView

class WallpaperImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    init {
        setWallpaperImage()
    }

    private fun setWallpaperImage() {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable: Drawable? = wallpaperManager.drawable
        val wallpaperBitmap = getBitmapFromDrawable(wallpaperDrawable)

        wallpaperBitmap?.let { bitmap ->
            val croppedBitmap = cropBitmap(bitmap)
            setImageBitmap(croppedBitmap)
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun cropBitmap(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val newHeight = (height * 0.5).toInt()
        return cropBitmapToBottomHalf(Bitmap.createBitmap(original, 0, 0, width, newHeight))
    }

    private fun cropBitmapToBottomHalf(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val yCrop = (height * 0.5).toInt()
        return cropBitmapHorizontally(Bitmap.createBitmap(original, 0, yCrop, width, height - yCrop))
    }

    private fun cropBitmapHorizontally(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val xCrop = (width * 0.25).toInt()
        return Bitmap.createBitmap(original, xCrop, 0, width - 2 * xCrop, height)
    }
}
