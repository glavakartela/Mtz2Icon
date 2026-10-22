package com.glavalartela.mtz2icon

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var pack by mutableStateOf<Pack?>(null); private set
    var matches by mutableStateOf<List<Match>>(emptyList()); private set
    var selected by mutableStateOf<Set<String>>(emptySet()); private set
    private val sp = app.getSharedPreferences("mtz2icon", Context.MODE_PRIVATE)

    // settings (persisted)
    var themeMode by mutableIntStateOf(sp.getInt("theme", 0)); private set // 0 system, 1 light, 2 dark
    var dynamicColor by mutableStateOf(sp.getBoolean("dynamic", true)); private set
    var rememberShape by mutableStateOf(sp.getBoolean("remember", true)); private set
    var preselect by mutableStateOf(sp.getBoolean("preselect", false)); private set
    var showPkg by mutableStateOf(sp.getBoolean("showpkg", true)); private set
    var pinDelay by mutableIntStateOf(sp.getInt("delay", 800)); private set
    var quality by mutableIntStateOf(sp.getInt("quality", 0)); private set // 0 = auto

    var customShapes by mutableStateOf(loadCustoms()); private set

    var shape by mutableStateOf(if (rememberShape) savedShape() else IconShapes.LAUNCHER); private set
    var scale by mutableFloatStateOf(if (rememberShape) sp.getFloat("scale", 1f) else 1f); private set
    var query by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var progress by mutableStateOf<Pair<Int, Int>?>(null); private set
    var message by mutableStateOf<Int?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) { apps = PackReader.loadApps(app) }
    }

    private fun savedShape(): IconShape {
        val q = sp.getString("shape", null)?.let { ShapeCodec.decode(it) } ?: return IconShapes.LAUNCHER
        return (IconShapes.presets + customShapes).firstOrNull { it.q == q } ?: IconShape("Custom", q)
    }

    private fun loadCustoms(): List<IconShape> =
        (sp.getString("shapes", "") ?: "").lines().mapNotNull { line ->
            val i = line.indexOf('|')
            if (i <= 0) return@mapNotNull null
            val q = ShapeCodec.decode(line.substring(i + 1)) ?: return@mapNotNull null
            IconShape(line.substring(0, i), q)
        }

    private fun saveCustoms() {
        val text = customShapes.joinToString("\n") { it.name.replace("|", " ").replace("\n", " ") + "|" + it.code }
        sp.edit().putString("shapes", text).apply()
    }

    /** Adds a drawn or imported shape (or just selects it if it already exists) and selects it. */
    fun addCustomShape(q: List<Int>, name: String) {
        val existing = (IconShapes.presets + customShapes).firstOrNull { it.q == q }
        if (existing != null) {
            updateShape(existing)
            return
        }
        val s = IconShape(name, q)
        customShapes = customShapes + s
        saveCustoms()
        updateShape(s)
    }

    fun deleteCustomShape(s: IconShape) {
        customShapes = customShapes - s
        saveCustoms()
        if (shape == s) updateShape(IconShapes.LAUNCHER)
    }

    private fun saveShape() {
        if (!rememberShape) return
        val e = sp.edit()
        val c = shape.code
        if (c == null) e.remove("shape") else e.putString("shape", c)
        e.putFloat("scale", scale).apply()
    }

    fun updateShape(s: IconShape) { shape = s; saveShape() }
    fun updateScale(v: Float) { scale = v; saveShape() }

    fun setTheme(v: Int) { themeMode = v; sp.edit().putInt("theme", v).apply() }
    fun setDynamic(v: Boolean) { dynamicColor = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun changePreselect(v: Boolean) { preselect = v; sp.edit().putBoolean("preselect", v).apply() }
    fun changeShowPkg(v: Boolean) { showPkg = v; sp.edit().putBoolean("showpkg", v).apply() }
    fun changePinDelay(v: Int) { pinDelay = v; sp.edit().putInt("delay", v).apply() }
    fun changeQuality(v: Int) { quality = v; sp.edit().putInt("quality", v).apply() }
    fun changeRememberShape(v: Boolean) {
        rememberShape = v
        sp.edit().putBoolean("remember", v).apply()
        if (v) saveShape() else sp.edit().remove("shape").remove("scale").apply()
    }

    fun resetSettings() {
        val keep = customShapes
        sp.edit().clear().apply()
        customShapes = keep
        saveCustoms()
        themeMode = 0; dynamicColor = true; rememberShape = true; preselect = false
        showPkg = true; pinDelay = 800; quality = 0
        shape = IconShapes.LAUNCHER; scale = 1f
    }

    fun importPack(uri: Uri) {
        viewModelScope.launch {
            busy = true
            message = null
            val p = withContext(Dispatchers.IO) {
                runCatching { PackReader.read(getApplication(), uri) }.getOrNull()
            }
            if (p == null || p.icons.isEmpty()) {
                message = R.string.err_read
            } else {
                val m = withContext(Dispatchers.Default) { PackReader.match(p, apps) }
                pack = p
                matches = m
                selected = if (preselect) m.map { it.app.pkg }.toSet() else emptySet()
                if (m.isEmpty()) message = R.string.err_no_matches
            }
            busy = false
        }
    }

    fun toggle(pkg: String) {
        selected = if (pkg in selected) selected - pkg else selected + pkg
    }

    fun selectAll(shown: List<Match>) {
        selected = selected + shown.map { it.app.pkg }
    }

    fun clearSelection() {
        selected = emptySet()
    }

    fun create() {
        val items = matches.filter { it.app.pkg in selected }
        if (items.isEmpty() || busy) return
        viewModelScope.launch {
            busy = true
            progress = 0 to items.size
            val n = ShortcutPinner.pin(getApplication(), items, shape, scale, pinDelay.toLong(), quality) { progress = it to items.size }
            progress = null
            busy = false
            message = if (n < 0) R.string.err_pin_unsupported else R.string.done_msg
        }
    }
}
