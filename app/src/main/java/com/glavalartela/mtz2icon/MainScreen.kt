package com.glavalartela.mtz2icon

import android.app.LocaleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.os.LocaleList
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@StringRes
private fun IconShape.labelRes(): Int = when (name) {
    "Launcher" -> R.string.shape_original
    "Circle" -> R.string.shape_circle
    "Squircle" -> R.string.shape_squircle
    "Diamond" -> R.string.shape_diamond
    "Hexagon" -> R.string.shape_hexagon
    "Octagon" -> R.string.shape_octagon
    "Flower" -> R.string.shape_flower
    "Star" -> R.string.shape_star
    "Clover" -> R.string.shape_clover
    "Square" -> R.string.shape_square
    else -> R.string.shape_custom
}

@Composable
fun MainScreen(vm: MainViewModel, resumeTick: Int) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    HomeScreen(vm, resumeTick) { settingsOpen = true }
    if (settingsOpen) SettingsSheet(vm) { settingsOpen = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(vm: MainViewModel, resumeTick: Int, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importPack(uri)
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snack.showSnackbar(ctx.getString(it))
            vm.message = null
        }
    }
    val shown = remember(vm.matches, vm.query) {
        val q = vm.query.trim()
        if (q.isEmpty()) vm.matches
        else vm.matches.filter { it.app.label.contains(q, true) || it.app.pkg.contains(q, true) }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                scrollBehavior = scroll
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (vm.matches.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = { vm.create() }) {
                    val pr = vm.progress
                    Text(
                        if (pr != null) stringResource(R.string.progress_msg, pr.first, pr.second)
                        else stringResource(R.string.create_btn, vm.selected.size)
                    )
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PackCard(vm) { picker.launch(arrayOf("*/*")) } }
            item { PermissionCard(resumeTick) }
            item { ShapeCard(vm) }
            if (vm.matches.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = vm.query,
                            onValueChange = { vm.query = it },
                            label = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row {
                            TextButton(onClick = { vm.selectAll(shown) }) { Text(stringResource(R.string.select_all)) }
                            TextButton(onClick = { vm.clearSelection() }) { Text(stringResource(R.string.select_none)) }
                        }
                    }
                }
                items(shown, key = { it.app.pkg }) { m ->
                    val checked = m.app.pkg in vm.selected
                    ListItem(
                        modifier = Modifier.clickable { vm.toggle(m.app.pkg) },
                        leadingContent = { IconBitmap(m.bytes, vm.shape, vm.scale, 48.dp) },
                        headlineContent = { Text(m.app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = if (vm.showPkg) ({ Text(m.app.pkg, maxLines = 1, overflow = TextOverflow.Ellipsis) }) else null,
                        trailingContent = { Checkbox(checked = checked, onCheckedChange = { vm.toggle(m.app.pkg) }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun PackCard(vm: MainViewModel, onPick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.pack_title), style = MaterialTheme.typography.titleMedium)
            val p = vm.pack
            Text(
                if (p == null) stringResource(R.string.pack_hint)
                else stringResource(R.string.pack_loaded, p.name, p.icons.size, vm.matches.size),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onPick, enabled = !vm.busy) { Text(stringResource(R.string.import_pack)) }
            if (vm.busy && vm.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionCard(resumeTick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<Int?>(null) }
    // after returning from the Shizuku app, check whether the service got connected
    LaunchedEffect(resumeTick) {
        if (ShizukuHelper.isConnected()) status = R.string.shz_connected
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.perm_hint), style = MaterialTheme.typography.bodyMedium)
            status?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        status = when (ShizukuHelper.connect(ctx)) {
                            ShizukuHelper.Connect.NOT_RUNNING -> R.string.shz_not_running
                            ShizukuHelper.Connect.ASKED -> R.string.shz_asked
                            ShizukuHelper.Connect.CONNECTING, ShizukuHelper.Connect.CONNECTED -> {
                                kotlinx.coroutines.delay(800)
                                R.string.shz_connected
                            }
                        }
                    }
                }) { Text(stringResource(R.string.perm_connect)) }
                Button(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            if (ShizukuHelper.isConnected()) ShizukuHelper.grantShortcutPermission(ctx) else null
                        }
                        status = when (ok) {
                            null -> R.string.shz_asked
                            true -> R.string.perm_done
                            false -> R.string.perm_failed
                        }
                    }
                }) { Text(stringResource(R.string.perm_allow)) }
            }
            TextButton(onClick = { ShizukuHelper.openSettings(ctx) }) { Text(stringResource(R.string.perm_settings)) }
        }
    }
}

@Composable
private fun ShapeCard(vm: MainViewModel) {
    val ctx = LocalContext.current
    val builtIn = remember { listOf(IconShapes.LAUNCHER) + IconShapes.presets }
    val shapes = builtIn + vm.customShapes
    var drawing by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.shape_title), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sample = vm.matches.firstOrNull()?.bytes
                shapes.forEach { s ->
                    val sel = s == vm.shape
                    val label = if (s in builtIn) stringResource(s.labelRes())
                    else stringResource(R.string.shape_custom) + " " + (vm.customShapes.indexOf(s) + 1)
                    Surface(
                        onClick = { vm.updateShape(s) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconBitmap(sample, s, vm.scale, 56.dp)
                            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Text(stringResource(R.string.scale_title), style = MaterialTheme.typography.labelLarge)
            Slider(value = vm.scale, onValueChange = { vm.updateScale(it) }, valueRange = 0.8f..1.4f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { drawing = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shape_draw))
                }
                OutlinedButton(onClick = { importing = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shape_paste))
                }
            }
            vm.shape.code?.let { code ->
                Text(
                    stringResource(R.string.shape_share),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(code, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        ctx.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("icrec", code))
                        Toast.makeText(ctx, R.string.code_copied, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.shape_copy)) }
                    if (vm.shape in vm.customShapes) {
                        TextButton(onClick = { vm.deleteCustomShape(vm.shape) }) {
                            Text(stringResource(R.string.shape_delete))
                        }
                    }
                }
            }
        }
    }
    if (drawing) {
        DrawShapeDialog(
            onSave = { q -> vm.addCustomShape(q, "Custom"); drawing = false },
            onDismiss = { drawing = false }
        )
    }
    if (importing) {
        ImportShapeDialog(
            onImport = { q -> vm.addCustomShape(q, "Imported"); importing = false },
            onDismiss = { importing = false }
        )
    }
}

@Composable
private fun IconBitmap(bytes: ByteArray?, shape: IconShape, scale: Float, size: Dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val color = MaterialTheme.colorScheme.primary.toArgb()
    val bmp by produceState<ImageBitmap?>(null, bytes, shape, scale, px, color) {
        value = withContext(Dispatchers.Default) {
            val b = if (bytes != null) ShapeRenderer.render(bytes, shape, scale, px)
            else ShapeRenderer.swatch(shape, px, color)
            b?.asImageBitmap()
        }
    }
    Box(Modifier.size(size)) {
        bmp?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            SettingsSection(R.string.section_appearance) {
                ChoiceRow(
                    stringResource(R.string.settings_theme),
                    listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).map { stringResource(it) },
                    vm.themeMode
                ) { vm.setTheme(it) }
                SwitchRow(R.string.settings_dynamic, vm.dynamicColor) { vm.setDynamic(it) }
                LanguageRow()
            }
            SettingsSection(R.string.section_behavior) {
                SwitchRow(R.string.settings_remember, vm.rememberShape) { vm.changeRememberShape(it) }
                SwitchRow(R.string.settings_preselect, vm.preselect) { vm.changePreselect(it) }
                SwitchRow(R.string.settings_show_pkg, vm.showPkg) { vm.changeShowPkg(it) }
            }
            SettingsSection(R.string.section_shortcuts) {
                val delays = listOf(400, 800, 1500, 2500)
                ChoiceRow(
                    stringResource(R.string.settings_delay),
                    delays.map { stringResource(R.string.unit_ms, it) },
                    delays.indexOf(vm.pinDelay).coerceAtLeast(0)
                ) { vm.changePinDelay(delays[it]) }
                val sizes = listOf(0, 192, 256, 384)
                ChoiceRow(
                    stringResource(R.string.settings_quality),
                    sizes.map { if (it == 0) stringResource(R.string.quality_auto) else "$it px" },
                    sizes.indexOf(vm.quality).coerceAtLeast(0)
                ) { vm.changeQuality(sizes[it]) }
            }
            OutlinedButton(onClick = { vm.resetSettings() }) { Text(stringResource(R.string.settings_reset)) }
            SettingsSection(R.string.section_about) {
                Text(stringResource(R.string.about_text, version), style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(@StringRes title: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(stringResource(title)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ChoiceRow(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, l ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(l) })
            }
        }
    }
}

@Composable
private fun LanguageRow() {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val langs = listOf(
        "" to stringResource(R.string.lang_system),
        "en" to "English", "de" to "Deutsch", "ru" to "Русский", "uk" to "Українська",
        "pl" to "Polski", "es" to "Español", "fr" to "Français"
    )
    Box {
        ListItem(
            modifier = Modifier.clickable { open = true },
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            trailingContent = { Icon(GlobeIcon, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            langs.forEach { (tag, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    open = false
                    ctx.getSystemService(LocaleManager::class.java).applicationLocales =
                        if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                })
            }
        }
    }
}
