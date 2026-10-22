package com.glavalartela.mtz2icon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlin.math.max

object ShapeRenderer {

    private fun shapePath(q: List<Int>, size: Int): Path {
        val pts = IconShapes.outline(q)
        val half = size / 2f
        val path = Path()
        for (k in 0 until pts.size / 2) {
            val x = half + pts[2 * k] * half
            val y = half + pts[2 * k + 1] * half
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun decode(bytes: ByteArray, target: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var sample = 1
        while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o2)
    }

    /** Pack icon clipped to [shape] and zoomed by [scale]. The launcher shape means: keep the pack icon as is. */
    fun render(bytes: ByteArray, shape: IconShape, scale: Float, px: Int): Bitmap? {
        val src = decode(bytes, max(px, 64)) ?: return null
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val half = px * scale / 2f
        val c = px / 2f
        val dst = RectF(c - half, c - half, c + half, c + half)
        val bp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val q = shape.q
        if (q == null) {
            canvas.drawBitmap(src, null, dst, bp)
            return out
        }
        canvas.drawPath(shapePath(q, px), Paint(Paint.ANTI_ALIAS_FLAG))
        bp.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, null, dst, bp)
        return out
    }

    /** Plain filled shape, used for previews while no pack is loaded. */
    fun swatch(shape: IconShape, px: Int, color: Int): Bitmap {
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val q = shape.q ?: IconShapes.presets[8].q!! // square
        Canvas(out).drawPath(shapePath(q, px), Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        return out
    }
}
