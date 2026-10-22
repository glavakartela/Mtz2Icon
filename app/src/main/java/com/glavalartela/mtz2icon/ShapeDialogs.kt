package com.glavalartela.mtz2icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Draw a custom shape: the stroke becomes a 24-point shape that can be shared as a code. */
@Composable
internal fun DrawShapeDialog(onSave: (List<Int>) -> Unit, onDismiss: () -> Unit) {
    var stroke by remember { mutableStateOf(listOf<Offset>()) }
    val profile = remember(stroke) { IconShapes.fromStroke(stroke.map { it.x to it.y }) }
    val fill = MaterialTheme.colorScheme.primaryContainer
    val line = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outline

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shape_draw_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.shape_draw_hint), style = MaterialTheme.typography.bodySmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { o -> stroke = listOf(o) },
                                onDrag = { change, _ ->
                                    stroke = stroke + change.position
                                    change.consume()
                                },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val lim = size.minDimension / 2f * 0.92f

                        // frame = the icon square
                        drawRect(
                            color = guide,
                            topLeft = Offset(cx - lim, cy - lim),
                            size = Size(lim * 2f, lim * 2f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        if (stroke.size > 1) {
                            val p = Path()
                            p.moveTo(stroke[0].x, stroke[0].y)
                            for (i in 1 until stroke.size) p.lineTo(stroke[i].x, stroke[i].y)
                            drawPath(p, color = line.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
                        }
                        profile?.let { q ->
                            val pts = IconShapes.outline(q)
                            val path = Path()
                            for (k in 0 until pts.size / 2) {
                                val x = cx + pts[2 * k] * lim
                                val y = cy + pts[2 * k + 1] * lim
                                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            path.close()
                            drawPath(path, color = fill.copy(alpha = 0.85f))
                            drawPath(path, color = line, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = profile != null, onClick = { profile?.let(onSave) }) {
                Text(stringResource(R.string.shape_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { stroke = emptyList() }) { Text(stringResource(R.string.select_none)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
internal fun ImportShapeDialog(onImport: (List<Int>) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val q = remember(text) { ShapeCodec.decode(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shape_code_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("icrec-…") },
                isError = text.isNotBlank() && q == null,
                supportingText = {
                    Text(
                        stringResource(
                            when {
                                text.isBlank() -> R.string.shape_code_hint
                                q == null -> R.string.shape_code_bad
                                else -> R.string.shape_code_valid
                            }
                        )
                    )
                },
            )
        },
        confirmButton = {
            TextButton(enabled = q != null, onClick = { q?.let(onImport) }) { Text(stringResource(R.string.shape_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
