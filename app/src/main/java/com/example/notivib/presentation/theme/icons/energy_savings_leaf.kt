package com.example.notivib.presentation.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val energy_savings_leaf: ImageVector
  get() {
    if (_energy_savings_leaf != null) {
      return _energy_savings_leaf!!
    }
    _energy_savings_leaf =
      ImageVector.Builder(
          name = "energy_savings_leaf",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.83f, 16.33f)
            lineToRelative(4.6f, -4.1f)
            quadToRelative(0.22f, -0.2f, 0.13f, -0.47f)
            reflectiveQuadToRelative(-0.4f, -0.33f)
            lineToRelative(-3.6f, -0.35f)
            lineTo(13.7f, 8.1f)
            quadTo(13.78f, 7.97f, 13.79f, 7.86f)
            reflectiveQuadTo(13.7f, 7.65f)
            quadTo(13.6f, 7.52f, 13.45f, 7.54f)
            reflectiveQuadTo(13.18f, 7.65f)
            lineTo(8.6f, 11.75f)
            quadToRelative(-0.22f, 0.2f, -0.13f, 0.47f)
            quadToRelative(0.1f, 0.28f, 0.4f, 0.32f)
            lineToRelative(3.6f, 0.35f)
            lineTo(10.3f, 15.88f)
            quadTo(10.23f, 16f, 10.23f, 16.11f)
            reflectiveQuadToRelative(0.1f, 0.21f)
            quadToRelative(0.1f, 0.1f, 0.24f, 0.1f)
            reflectiveQuadToRelative(0.26f, -0.1f)
            close()
            moveTo(12f, 20f)
            quadTo(10.6f, 20f, 9.36f, 19.56f)
            reflectiveQuadTo(7.1f, 18.33f)
            lineTo(5.73f, 19.7f)
            quadTo(5.58f, 19.85f, 5.39f, 19.93f)
            reflectiveQuadTo(5f, 20f)
            quadTo(4.58f, 20f, 4.29f, 19.71f)
            quadTo(4f, 19.43f, 4f, 19f)
            quadTo(4f, 18.8f, 4.08f, 18.61f)
            reflectiveQuadTo(4.3f, 18.27f)
            lineTo(5.68f, 16.9f)
            quadTo(4.88f, 15.88f, 4.44f, 14.64f)
            reflectiveQuadTo(4f, 12f)
            quadTo(4f, 8.65f, 6.33f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 3.35f, -2.32f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
          }
        }
        .build()
    return _energy_savings_leaf!!
  }

private var _energy_savings_leaf: ImageVector? = null
