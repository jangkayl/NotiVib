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
public val face: ImageVector
  get() {
    if (_face != null) {
      return _face!!
    }
    _face =
      ImageVector.Builder(
          name = "face",
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
            moveTo(8.11f, 13.89f)
            quadTo(7.75f, 13.52f, 7.75f, 13f)
            reflectiveQuadTo(8.11f, 12.11f)
            reflectiveQuadTo(9f, 11.75f)
            reflectiveQuadToRelative(0.89f, 0.36f)
            quadToRelative(0.36f, 0.36f, 0.36f, 0.89f)
            quadToRelative(0f, 0.52f, -0.36f, 0.89f)
            reflectiveQuadTo(9f, 14.25f)
            reflectiveQuadTo(8.11f, 13.89f)
            close()
            moveToRelative(6f, 0f)
            quadTo(13.75f, 13.52f, 13.75f, 13f)
            reflectiveQuadToRelative(0.36f, -0.89f)
            reflectiveQuadTo(15f, 11.75f)
            quadToRelative(0.53f, 0f, 0.89f, 0.36f)
            quadToRelative(0.36f, 0.36f, 0.36f, 0.89f)
            quadToRelative(0f, 0.52f, -0.36f, 0.89f)
            reflectiveQuadTo(15f, 14.25f)
            reflectiveQuadTo(14.11f, 13.89f)
            close()
            moveTo(12f, 20f)
            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
            reflectiveQuadTo(20f, 12f)
            quadToRelative(0f, -0.6f, -0.07f, -1.16f)
            reflectiveQuadTo(19.65f, 9.75f)
            quadTo(19.13f, 9.88f, 18.6f, 9.94f)
            reflectiveQuadTo(17.5f, 10f)
            quadTo(15.23f, 10f, 13.2f, 9.02f)
            reflectiveQuadTo(9.75f, 6.3f)
            quadTo(8.95f, 8.25f, 7.46f, 9.69f)
            reflectiveQuadTo(4f, 11.85f)
            quadToRelative(0f, 0.05f, 0f, 0.07f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.35f, 2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(0f, 2f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _face!!
  }

private var _face: ImageVector? = null
