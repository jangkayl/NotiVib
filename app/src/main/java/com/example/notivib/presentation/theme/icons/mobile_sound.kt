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
public val mobile_sound: ImageVector
  get() {
    if (_mobile_sound != null) {
      return _mobile_sound!!
    }
    _mobile_sound =
      ImageVector.Builder(
          name = "mobile_sound",
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
            moveTo(7f, 23f)
            quadTo(6.18f, 23f, 5.59f, 22.41f)
            reflectiveQuadTo(5f, 21f)
            verticalLineTo(3f)
            quadTo(5f, 2.17f, 5.59f, 1.59f)
            reflectiveQuadTo(7f, 1f)
            horizontalLineTo(17f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(19f, 3f)
            verticalLineTo(3.67f)
            lineToRelative(-7.1f, 7.08f)
            verticalLineToRelative(2.5f)
            lineTo(19f, 20.33f)
            verticalLineTo(21f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 23f)
            horizontalLineTo(7f)
            close()
            moveTo(12.71f, 5.71f)
            quadTo(13f, 5.43f, 13f, 5f)
            reflectiveQuadTo(12.71f, 4.29f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(11.29f, 4.29f)
            reflectiveQuadTo(11f, 5f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 6f)
            reflectiveQuadTo(12.71f, 5.71f)
            close()
            moveToRelative(3.61f, 9.14f)
            lineTo(14.88f, 13.4f)
            quadToRelative(0.3f, -0.27f, 0.46f, -0.64f)
            reflectiveQuadTo(15.5f, 12f)
            reflectiveQuadTo(15.34f, 11.24f)
            quadTo(15.18f, 10.88f, 14.88f, 10.6f)
            lineTo(16.33f, 9.15f)
            quadToRelative(0.58f, 0.58f, 0.88f, 1.31f)
            reflectiveQuadTo(17.5f, 12f)
            reflectiveQuadToRelative(-0.3f, 1.54f)
            quadToRelative(-0.3f, 0.74f, -0.88f, 1.31f)
            close()
            moveToRelative(2.45f, 2.45f)
            lineToRelative(-1.4f, -1.4f)
            quadToRelative(0.78f, -0.78f, 1.2f, -1.78f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadTo(18.58f, 9.88f)
            reflectiveQuadTo(17.38f, 8.1f)
            lineToRelative(1.4f, -1.4f)
            quadToRelative(1.07f, 1.05f, 1.65f, 2.43f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.57f, 2.88f)
            reflectiveQuadTo(18.78f, 17.3f)
            close()
          }
        }
        .build()
    return _mobile_sound!!
  }

private var _mobile_sound: ImageVector? = null
