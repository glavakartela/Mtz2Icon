package com.glavalartela.mtz2icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Outlined globe in the Material style ("language" is not part of material-icons-core). */
val GlobeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Globe",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        ) {
            // outer circle
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, true, true, 12f, 21f)
            arcTo(9f, 9f, 0f, true, true, 12f, 3f)
            close()
            // equator
            moveTo(3f, 12f)
            lineTo(21f, 12f)
            // meridian
            moveTo(12f, 3f)
            arcTo(4.5f, 9f, 0f, true, true, 12f, 21f)
            arcTo(4.5f, 9f, 0f, true, true, 12f, 3f)
            close()
        }
    }.build()
}
