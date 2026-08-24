package com.feldman.flashlight.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean readout card displaying sensor and flashlight metrics.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScope.SensorCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    border: androidx.compose.foundation.BorderStroke? = null,
    iconShapeIndex: Int = 1,
    index: Int = iconShapeIndex
) {
    val iconBackgroundShape = when (iconShapeIndex) {
        1 -> MaterialShapes.Cookie9Sided.toShape()
        2 -> MaterialShapes.Slanted.toShape()
        3 -> MaterialShapes.Sunny.toShape()
        4 -> MaterialShapes.Cookie12Sided.toShape()
        5 -> MaterialShapes.PixelCircle.toShape()
        else -> MaterialShapes.Square.toShape()
    }

    Card(
        modifier = modifier
            .weight(1f)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(cornerRadius),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(if (iconShapeIndex == 2) Modifier.rotate(45f) else Modifier)
                    .background(
                        color = valueColor.copy(alpha = 0.12f),
                        shape = iconBackgroundShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (iconShapeIndex == 2) Modifier.rotate(-45f) else Modifier),
                    tint = valueColor
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                maxLines = 1
            )
        }
    }
}
