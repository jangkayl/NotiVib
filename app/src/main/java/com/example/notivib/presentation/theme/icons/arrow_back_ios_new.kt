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
public val arrow_back_ios_new: ImageVector
  get() {
    if (_arrow_back_ios_new != null) {
      return _arrow_back_ios_new!!
    }
    _arrow_back_ios_new =
      ImageVector.Builder(
          name = "arrow_back_ios_new",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(16f, 22f)
            lineTo(6f, 12f)
            lineTo(16f, 2f)
            lineToRelative(1.78f, 1.77f)
            lineTo(9.55f, 12f)
            lineToRelative(8.23f, 8.23f)
            lineTo(16f, 22f)
            close()
          }
        }
        .build()
    return _arrow_back_ios_new!!
  }

private var _arrow_back_ios_new: ImageVector? = null
