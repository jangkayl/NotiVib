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
public val campaign: ImageVector
  get() {
    if (_campaign != null) {
      return _campaign!!
    }
    _campaign =
      ImageVector.Builder(
          name = "campaign",
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
            moveTo(18f, 13f)
            verticalLineTo(11f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineTo(18f)
            close()
            moveToRelative(1.2f, 7f)
            lineTo(16f, 17.6f)
            lineTo(17.2f, 16f)
            lineToRelative(3.2f, 2.4f)
            lineTo(19.2f, 20f)
            close()
            moveTo(17.2f, 8f)
            lineTo(16f, 6.4f)
            lineTo(19.2f, 4f)
            lineToRelative(1.2f, 1.6f)
            lineTo(17.2f, 8f)
            close()
            moveTo(5f, 19f)
            verticalLineTo(15f)
            horizontalLineTo(4f)
            quadTo(3.18f, 15f, 2.59f, 14.41f)
            reflectiveQuadTo(2f, 13f)
            verticalLineTo(11f)
            quadTo(2f, 10.17f, 2.59f, 9.59f)
            reflectiveQuadTo(4f, 9f)
            horizontalLineTo(8f)
            lineTo(13f, 6f)
            verticalLineTo(18f)
            lineTo(8f, 15f)
            horizontalLineTo(7f)
            verticalLineToRelative(4f)
            horizontalLineTo(5f)
            close()
            moveToRelative(9f, -3.65f)
            verticalLineTo(8.65f)
            quadToRelative(0.68f, 0.6f, 1.09f, 1.46f)
            quadTo(15.5f, 10.98f, 15.5f, 12f)
            quadToRelative(0f, 1.02f, -0.41f, 1.89f)
            reflectiveQuadTo(14f, 15.35f)
            close()
          }
        }
        .build()
    return _campaign!!
  }

private var _campaign: ImageVector? = null
