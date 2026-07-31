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
public val mop: ImageVector
  get() {
    if (_mop != null) {
      return _mop!!
    }
    _mop =
      ImageVector.Builder(
          name = "mop",
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
            moveTo(11f, 11f)
            horizontalLineToRelative(2f)
            verticalLineTo(4f)
            quadTo(13f, 3.57f, 12.71f, 3.29f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadTo(11.29f, 3.29f)
            reflectiveQuadTo(11f, 4f)
            verticalLineToRelative(7f)
            close()
            moveTo(5f, 15f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(5f)
            verticalLineToRelative(2f)
            close()
            moveTo(3.55f, 21f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            quadTo(6f, 18.58f, 6.29f, 18.29f)
            reflectiveQuadTo(7f, 18f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(8f, 19f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3f)
            verticalLineTo(19f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 19f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3f)
            verticalLineTo(19f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(17f, 18f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 19f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2.45f)
            lineToRelative(-1f, -4f)
            horizontalLineTo(4.55f)
            lineToRelative(-1f, 4f)
            close()
            moveToRelative(16.9f, 2f)
            horizontalLineTo(3.55f)
            quadTo(2.58f, 23f, 1.98f, 22.23f)
            reflectiveQuadTo(1.63f, 20.5f)
            lineTo(3f, 15f)
            verticalLineTo(13f)
            quadTo(3f, 12.18f, 3.59f, 11.59f)
            reflectiveQuadTo(5f, 11f)
            horizontalLineTo(9f)
            verticalLineTo(4f)
            quadTo(9f, 2.75f, 9.88f, 1.88f)
            reflectiveQuadTo(12f, 1f)
            reflectiveQuadToRelative(2.13f, 0.88f)
            reflectiveQuadTo(15f, 4f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(4f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(21f, 12.18f, 21f, 13f)
            verticalLineToRelative(2f)
            lineToRelative(1.38f, 5.5f)
            quadToRelative(0.33f, 0.95f, -0.29f, 1.73f)
            reflectiveQuadTo(20.45f, 23f)
            close()
            moveTo(19f, 13f)
            horizontalLineTo(5f)
            horizontalLineTo(19f)
            close()
            moveTo(13f, 11f)
            horizontalLineTo(11f)
            quadToRelative(0f, 0f, 0.29f, 0f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadToRelative(0.71f, 0f)
            reflectiveQuadTo(13f, 11f)
            close()
          }
        }
        .build()
    return _mop!!
  }

private var _mop: ImageVector? = null
