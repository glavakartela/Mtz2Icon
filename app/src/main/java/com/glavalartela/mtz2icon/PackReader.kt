package com.glavalartela.mtz2icon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.ZipInputStream

data class AppEntry(val pkg: String, val label: String, val component: ComponentName)

/** icons: file stem -> image bytes; components: "pkg/cls" or "pkg" -> drawable name (from appfilter.xml). */
class Pack(val name: String, val icons: Map<String, ByteArray>, val components: Map<String, String>)

class Match(val app: AppEntry, val bytes: ByteArray)

object PackReader {
    private val IMAGE_EXT = setOf("png", "webp", "jpg", "jpeg")
    private val SKIP_DIRS = listOf("wallpaper", "lockscreen", "preview", "font", "ringtone")
    private const val MAX_ICON = 1_000_000

    fun loadApps(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map {
                val ai = it.activityInfo
                AppEntry(ai.packageName, it.loadLabel(pm).toString(), ComponentName(ai.packageName, ai.name))
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun read(ctx: Context, uri: Uri): Pack? {
        val icons = HashMap<String, ByteArray>()
        val comps = HashMap<String, String>()
        ctx.contentResolver.openInputStream(uri)?.use { scan(ZipInputStream(it.buffered()), icons, comps, 0) }
            ?: return null
        return Pack(displayName(ctx, uri), icons, comps)
    }

    private fun displayName(ctx: Context, uri: Uri): String =
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: "pack"

    // .mtz is a zip; its "icons" entry is a nested zip with res/drawable-*/<package>.png
    private fun scan(zis: ZipInputStream, icons: MutableMap<String, ByteArray>, comps: MutableMap<String, String>, depth: Int) {
        while (true) {
            val e = zis.nextEntry ?: break
            if (e.isDirectory) continue
            val path = e.name.replace('\\', '/')
            val lower = path.lowercase()
            val file = path.substringAfterLast('/')
            val ext = file.substringAfterLast('.', "").lowercase()
            when {
                lower.endsWith("appfilter.xml") -> readLimited(zis, 4_000_000)?.let { parseAppFilter(it, comps) }
                ext in IMAGE_EXT -> {
                    if (SKIP_DIRS.any { lower.contains(it) }) continue
                    val bytes = readLimited(zis, MAX_ICON) ?: continue
                    val key = file.substringBeforeLast('.')
                    val old = icons[key]
                    if (old == null || bytes.size > old.size) icons[key] = bytes // keep the highest density
                }
                depth < 2 && (ext == "" || ext == "zip" || ext == "mtz") -> {
                    val pb = PushbackInputStream(zis as InputStream, 4)
                    val head = ByteArray(4)
                    val n = pb.read(head)
                    if (n > 0) pb.unread(head, 0, n)
                    if (n == 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) {
                        scan(ZipInputStream(pb), icons, comps, depth + 1)
                    }
                }
            }
        }
    }

    private fun readLimited(s: InputStream, limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > limit) return null
        }
        return out.toByteArray()
    }

    private fun parseAppFilter(bytes: ByteArray, comps: MutableMap<String, String>) {
        try {
            val p = Xml.newPullParser()
            p.setInput(ByteArrayInputStream(bytes), null)
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.name == "item") {
                    val c = p.getAttributeValue(null, "component")
                    val d = p.getAttributeValue(null, "drawable")
                    val inner = c?.substringAfter("{", "")?.substringBefore("}") ?: ""
                    if (d != null && inner.contains('/')) {
                        val pkg = inner.substringBefore('/')
                        var cls = inner.substringAfter('/')
                        if (cls.startsWith(".")) cls = pkg + cls
                        comps.putIfAbsent("$pkg/$cls", d)
                        comps.putIfAbsent(pkg, d)
                    }
                }
                ev = p.next()
            }
        } catch (_: Exception) {
        }
    }

    fun match(pack: Pack, apps: List<AppEntry>): List<Match> {
        val base = HashMap<String, ByteArray>()
        for ((k, v) in pack.icons) base.putIfAbsent(k.substringBefore('$').substringBefore('#'), v)
        return apps.mapNotNull { a ->
            val drawable = pack.components[a.pkg + "/" + a.component.className] ?: pack.components[a.pkg]
            val bytes = drawable?.let { pack.icons[it] } ?: pack.icons[a.pkg] ?: base[a.pkg]
            bytes?.let { Match(a, it) }
        }
    }
}
