package com.example.notivib.presentation.alarm

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val rocket_launch: ImageVector
    get() {
        if (_rocket_launch != null) return _rocket_launch!!
        _rocket_launch = ImageVector.Builder(
            name = "rocket_launch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2.45f, 10.58f)
                lineToRelative(4.2f, -4.2f)
                quadTo(7f, 6.02f, 7.48f, 5.88f)
                quadTo(7.95f, 5.72f, 8.45f, 5.82f)
                lineTo(9.75f, 6.1f)
                quadTo(8.4f, 7.7f, 7.63f, 9f)
                reflectiveQuadToRelative(-1.5f, 3.15f)
                lineTo(2.45f, 10.58f)
                close()
                moveToRelative(5.13f, 2.28f)
                quadToRelative(0.58f, -1.8f, 1.56f, -3.4f)
                reflectiveQuadToRelative(2.39f, -3f)
                quadToRelative(2.2f, -2.2f, 5.03f, -3.29f)
                reflectiveQuadTo(21.83f, 2.5f)
                quadToRelative(0.42f, 2.45f, -0.65f, 5.27f)
                reflectiveQuadTo(17.9f, 12.8f)
                quadToRelative(-1.38f, 1.38f, -3f, 2.39f)
                quadToRelative(-1.63f, 1.01f, -3.42f, 1.59f)
                lineTo(7.58f, 12.85f)
                close()
                moveToRelative(8.31f, -2.43f)
                quadToRelative(0.84f, 0f, 1.41f, -0.57f)
                quadTo(17.88f, 9.27f, 17.88f, 8.44f)
                reflectiveQuadTo(17.3f, 7.02f)
                reflectiveQuadTo(15.89f, 6.45f)
                reflectiveQuadTo(14.48f, 7.02f)
                reflectiveQuadTo(13.9f, 8.44f)
                quadToRelative(0f, 0.84f, 0.58f, 1.41f)
                reflectiveQuadToRelative(1.41f, 0.57f)
                close()
                moveTo(13.78f, 21.88f)
                lineTo(12.18f, 18.2f)
                quadToRelative(1.85f, -0.72f, 3.16f, -1.5f)
                quadToRelative(1.31f, -0.78f, 2.91f, -2.13f)
                lineToRelative(0.25f, 1.3f)
                quadToRelative(0.1f, 0.5f, -0.05f, 0.99f)
                reflectiveQuadToRelative(-0.5f, 0.84f)
                lineToRelative(-4.17f, 4.18f)
                close()
                moveTo(4.05f, 16.05f)
                quadTo(4.93f, 15.18f, 6.18f, 15.16f)
                reflectiveQuadTo(8.3f, 16.02f)
                reflectiveQuadToRelative(0.88f, 2.13f)
                reflectiveQuadTo(8.3f, 20.27f)
                quadTo(7.68f, 20.9f, 6.21f, 21.35f)
                reflectiveQuadToRelative(-4.04f, 0.8f)
                quadToRelative(0.35f, -2.57f, 0.8f, -4.02f)
                reflectiveQuadTo(4.05f, 16.05f)
                close()
            }
        }.build()
        return _rocket_launch!!
    }
private var _rocket_launch: ImageVector? = null

val extension: ImageVector
    get() {
        if (_extension != null) return _extension!!
        _extension = ImageVector.Builder(
            name = "extension",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(5f, 21f)
                quadTo(4.15f, 21f, 3.58f, 20.43f)
                reflectiveQuadTo(3f, 19f)
                verticalLineTo(15.2f)
                quadToRelative(1.2f, 0f, 2.1f, -0.76f)
                reflectiveQuadTo(6f, 12.5f)
                quadTo(6f, 11.35f, 5.1f, 10.6f)
                reflectiveQuadTo(3f, 9.8f)
                verticalLineTo(6f)
                quadTo(3f, 5.18f, 3.59f, 4.59f)
                reflectiveQuadTo(5f, 4f)
                horizontalLineTo(8.05f)
                quadTo(8.23f, 2.72f, 9.2f, 1.86f)
                reflectiveQuadTo(11.5f, 1f)
                quadToRelative(1.3f, 0f, 2.28f, 0.86f)
                reflectiveQuadTo(14.95f, 4f)
                horizontalLineTo(18f)
                quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                quadTo(20f, 5.18f, 20f, 6f)
                verticalLineTo(9.35f)
                quadToRelative(0.9f, 0.45f, 1.45f, 1.3f)
                reflectiveQuadTo(22f, 12.5f)
                quadToRelative(0f, 1.02f, -0.55f, 1.88f)
                reflectiveQuadTo(20f, 15.65f)
                verticalLineTo(19f)
                quadToRelative(0f, 0.85f, -0.59f, 1.43f)
                reflectiveQuadTo(18f, 21f)
                horizontalLineTo(5f)
                close()
            }
        }.build()
        return _extension!!
    }
private var _extension: ImageVector? = null

val hourglass_bottom: ImageVector
    get() {
        if (_hourglass_bottom != null) return _hourglass_bottom!!
        _hourglass_bottom = ImageVector.Builder(
            name = "hourglass_bottom",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(14.83f, 9.82f)
                quadTo(16f, 8.65f, 16f, 7f)
                verticalLineTo(4f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                quadTo(8f, 8.65f, 9.18f, 9.82f)
                reflectiveQuadTo(12f, 11f)
                reflectiveQuadTo(14.83f, 9.82f)
                close()
                moveTo(4f, 22f)
                verticalLineTo(20f)
                horizontalLineTo(6f)
                verticalLineTo(17f)
                quadTo(6f, 15.48f, 6.71f, 14.14f)
                reflectiveQuadTo(8.7f, 12f)
                quadTo(7.43f, 11.2f, 6.71f, 9.86f)
                reflectiveQuadTo(6f, 7f)
                verticalLineTo(4f)
                horizontalLineTo(4f)
                verticalLineTo(2f)
                horizontalLineTo(20f)
                verticalLineTo(4f)
                horizontalLineTo(18f)
                verticalLineTo(7f)
                quadToRelative(0f, 1.52f, -0.71f, 2.86f)
                reflectiveQuadTo(15.3f, 12f)
                quadToRelative(1.27f, 0.8f, 1.99f, 2.14f)
                reflectiveQuadTo(18f, 17f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
            }
        }.build()
        return _hourglass_bottom!!
    }
private var _hourglass_bottom: ImageVector? = null

val bomb: ImageVector
    get() {
        if (_bomb != null) return _bomb!!
        _bomb = ImageVector.Builder(
            name = "bomb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(8.65f, 22.8f)
                quadToRelative(-3.13f, 0f, -5.31f, -2.21f)
                quadTo(1.15f, 18.38f, 1.15f, 15.25f)
                reflectiveQuadTo(3.31f, 9.96f)
                reflectiveQuadTo(8.6f, 7.8f)
                quadToRelative(0.08f, 0f, 0.16f, 0f)
                reflectiveQuadToRelative(0.16f, 0f)
                lineTo(9.6f, 6.63f)
                quadTo(9.9f, 6.07f, 10.5f, 5.91f)
                reflectiveQuadToRelative(1.15f, 0.16f)
                lineTo(12.4f, 6.5f)
                lineTo(12.53f, 6.3f)
                quadTo(13.1f, 5.22f, 14.33f, 4.9f)
                reflectiveQuadToRelative(2.3f, 0.3f)
                lineTo(17.5f, 5.7f)
                lineToRelative(-1f, 1.73f)
                lineTo(15.63f, 6.93f)
                quadTo(15.28f, 6.72f, 14.86f, 6.84f)
                reflectiveQuadTo(14.25f, 7.3f)
                lineTo(14.13f, 7.5f)
                lineToRelative(1f, 0.57f)
                quadToRelative(0.53f, 0.3f, 0.69f, 0.9f)
                reflectiveQuadTo(15.68f, 10.1f)
                lineTo(15f, 11.3f)
                quadToRelative(0.58f, 0.9f, 0.86f, 1.91f)
                quadToRelative(0.29f, 1.01f, 0.29f, 2.09f)
                quadToRelative(0f, 3.13f, -2.19f, 5.31f)
                quadTo(11.78f, 22.8f, 8.65f, 22.8f)
                close()
                moveTo(20f, 8.8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(2f)
                horizontalLineTo(20f)
                close()
                moveTo(14.5f, 3.3f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(-2f)
                close()
                moveToRelative(4.88f, 2.03f)
                lineToRelative(-1.4f, -1.4f)
                lineTo(20.1f, 1.8f)
                lineToRelative(1.4f, 1.4f)
                lineTo(19.38f, 5.32f)
                close()
            }
        }.build()
        return _bomb!!
    }
private var _bomb: ImageVector? = null

val bolt: ImageVector
    get() {
        if (_bolt != null) return _bolt!!
        _bolt = ImageVector.Builder(
            name = "bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(8f, 22f)
                lineTo(9f, 15f)
                horizontalLineTo(4f)
                lineTo(13f, 2f)
                horizontalLineToRelative(2f)
                lineToRelative(-1f, 8f)
                horizontalLineToRelative(6f)
                lineTo(10f, 22f)
                horizontalLineTo(8f)
                close()
            }
        }.build()
        return _bolt!!
    }
private var _bolt: ImageVector? = null
