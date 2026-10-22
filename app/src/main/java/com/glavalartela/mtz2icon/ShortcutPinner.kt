package com.glavalartela.mtz2icon

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ShortcutPinner {
    const val ACTION_LAUNCH = "com.glavalartela.mtz2icon.LAUNCH"
    const val EXTRA_COMPONENT = "component"

    /** Returns the number of requests sent, or -1 if the launcher cannot pin shortcuts. */
    suspend fun pin(ctx: Context, items: List<Match>, shape: IconShape, scale: Float, delayMs: Long, sizePx: Int, onProgress: (Int) -> Unit): Int {
        val sm = ctx.getSystemService(ShortcutManager::class.java)
        if (!sm.isRequestPinShortcutSupported) return -1
        val size = if (sizePx > 0) sizePx else sm.iconMaxWidth.coerceIn(128, 512)
        var sent = 0
        items.forEachIndexed { idx, m ->
            val bmp = withContext(Dispatchers.Default) { ShapeRenderer.render(m.bytes, shape, scale, size) }
            if (bmp != null) {
                val intent = Intent(ctx, LaunchActivity::class.java)
                    .setAction(ACTION_LAUNCH)
                    .putExtra(EXTRA_COMPONENT, m.app.component.flattenToString())
                val info = ShortcutInfo.Builder(ctx, "mtz2icon:" + m.app.pkg)
                    .setShortLabel(m.app.label)
                    .setLongLabel(m.app.label)
                    .setIcon(Icon.createWithBitmap(bmp))
                    .setIntent(intent)
                    .build()
                // for an already pinned id the launcher just updates it
                sm.requestPinShortcut(info, null)
                sent++
                delay(delayMs)
            }
            onProgress(idx + 1)
        }
        return sent
    }
}
