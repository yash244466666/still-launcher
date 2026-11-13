package org.stilllauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A small, original icon set for the few concepts Material's core icon
 * library does not cover. Everything is drawn on a 24 x 24 grid so the
 * icons line up with the Material icons used elsewhere.
 */
object StillIcons {

    val Timer: ImageVector by lazy {
        vector("Timer") {
            stroke {
                moveTo(9.4f, 3.5f)
                lineTo(14.6f, 3.5f)
                moveTo(12f, 3.5f)
                lineTo(12f, 5.9f)
                moveTo(12f, 20.7f)
                arcTo(7.4f, 7.4f, 0f, true, true, 12f, 5.9f)
                arcTo(7.4f, 7.4f, 0f, true, true, 12f, 20.7f)
                moveTo(12f, 13.5f)
                lineTo(12f, 9.6f)
                moveTo(12f, 13.5f)
                lineTo(15.2f, 15.2f)
            }
        }
    }

    val Contrast: ImageVector by lazy {
        vector("Contrast") {
            solid {
                moveTo(12f, 4f)
                arcTo(8f, 8f, 0f, false, false, 12f, 20f)
                close()
            }
            stroke {
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4f)
                arcTo(8f, 8f, 0f, true, true, 12f, 20f)
            }
        }
    }

    val Palette: ImageVector by lazy {
        vector("Palette") {
            stroke {
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4f)
                arcTo(8f, 8f, 0f, true, true, 12f, 20f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
            }
            solid {
                moveTo(12f, 12f)
                lineTo(12f, 4.6f)
                arcTo(7.4f, 7.4f, 0f, false, true, 19.4f, 12f)
                close()
            }
        }
    }

    val EyeOff: ImageVector by lazy {
        vector("EyeOff") {
            stroke {
                moveTo(3.2f, 12f)
                curveTo(6.2f, 7.6f, 9f, 6.4f, 12f, 6.4f)
                curveTo(15f, 6.4f, 17.8f, 7.6f, 20.8f, 12f)
                curveTo(17.8f, 16.4f, 15f, 17.6f, 12f, 17.6f)
                curveTo(9f, 17.6f, 6.2f, 16.4f, 3.2f, 12f)
                close()
                moveTo(4.2f, 4.2f)
                lineTo(19.8f, 19.8f)
            }
            solid {
                moveTo(12f, 9.6f)
                arcTo(2.4f, 2.4f, 0f, true, true, 12f, 14.4f)
                arcTo(2.4f, 2.4f, 0f, true, true, 12f, 9.6f)
            }
        }
    }

    val Folder: ImageVector by lazy {
        vector("Folder") {
            solid {
                moveTo(3.2f, 7.2f)
                curveTo(3.2f, 6.1f, 4.1f, 5.2f, 5.2f, 5.2f)
                lineTo(9.1f, 5.2f)
                lineTo(11.1f, 7.4f)
                lineTo(18.8f, 7.4f)
                curveTo(19.9f, 7.4f, 20.8f, 8.3f, 20.8f, 9.4f)
                lineTo(20.8f, 17f)
                curveTo(20.8f, 18.1f, 19.9f, 19f, 18.8f, 19f)
                lineTo(5.2f, 19f)
                curveTo(4.1f, 19f, 3.2f, 18.1f, 3.2f, 17f)
                close()
            }
        }
    }

    val History: ImageVector by lazy {
        vector("History") {
            stroke {
                moveTo(12f, 20.4f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4.4f)
                arcTo(8f, 8f, 0f, true, true, 12f, 20.4f)
                moveTo(12f, 8.1f)
                lineTo(12f, 12.6f)
                lineTo(15.6f, 14.7f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        vector("Clock") {
            stroke {
                moveTo(12f, 20.2f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4.2f)
                arcTo(8f, 8f, 0f, true, true, 12f, 20.2f)
                moveTo(12f, 7.8f)
                lineTo(12f, 12.3f)
                lineTo(15.4f, 14.2f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        vector("Shield") {
            stroke {
                moveTo(12f, 3.4f)
                lineTo(19f, 6.2f)
                lineTo(19f, 11.4f)
                curveTo(19f, 15.8f, 16.2f, 19.1f, 12f, 20.8f)
                curveTo(7.8f, 19.1f, 5f, 15.8f, 5f, 11.4f)
                lineTo(5f, 6.2f)
                close()
                moveTo(8.5f, 12f)
                lineTo(11.1f, 14.6f)
                lineTo(15.6f, 9.7f)
            }
        }
    }

    val Grid: ImageVector by lazy {
        vector("Grid") {
            solid {
                roundedSquare(4f, 4f)
                roundedSquare(13f, 4f)
                roundedSquare(4f, 13f)
                roundedSquare(13f, 13f)
            }
        }
    }

    val Chart: ImageVector by lazy {
        vector("Chart") {
            solid {
                moveTo(4.5f, 13f)
                lineTo(8f, 13f)
                lineTo(8f, 20f)
                lineTo(4.5f, 20f)
                close()
                moveTo(10.3f, 9f)
                lineTo(13.8f, 9f)
                lineTo(13.8f, 20f)
                lineTo(10.3f, 20f)
                close()
                moveTo(16.1f, 5f)
                lineTo(19.6f, 5f)
                lineTo(19.6f, 20f)
                lineTo(16.1f, 20f)
                close()
            }
        }
    }

    val Gesture: ImageVector by lazy {
        vector("Gesture") {
            stroke {
                moveTo(5.5f, 17.4f)
                curveTo(5.5f, 11.4f, 9.6f, 8.4f, 18f, 8.4f)
                moveTo(14.7f, 5.1f)
                lineTo(18f, 8.4f)
                lineTo(14.7f, 11.7f)
            }
            solid {
                moveTo(5.5f, 19.3f)
                arcTo(1.1f, 1.1f, 0f, true, true, 5.5f, 17.1f)
                arcTo(1.1f, 1.1f, 0f, true, true, 5.5f, 19.3f)
            }
        }
    }

    val Keyboard: ImageVector by lazy {
        vector("Keyboard") {
            solid {
                roundedSquare(3f, 6f, 8.6f, 5.2f, 1.2f)
                roundedSquare(12.4f, 6f, 8.6f, 5.2f, 1.2f)
                moveTo(3f, 13.2f)
                lineTo(21f, 13.2f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
            }
        }
    }
}

private fun vector(name: String, builder: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(builder).build()

private fun ImageVector.Builder.stroke(pathBuilder: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder
    )
}

private fun ImageVector.Builder.solid(pathBuilder: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        pathBuilder = pathBuilder
    )
}

private fun PathBuilder.roundedSquare(
    left: Float,
    top: Float,
    width: Float = 7f,
    height: Float = 7f,
    radius: Float = 1.6f
) {
    moveTo(left + radius, top)
    lineTo(left + width - radius, top)
    quadTo(left + width, top, left + width, top + radius)
    lineTo(left + width, top + height - radius)
    quadTo(left + width, top + height, left + width - radius, top + height)
    lineTo(left + radius, top + height)
    quadTo(left, top + height, left, top + height - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}
