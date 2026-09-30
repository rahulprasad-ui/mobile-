package com.rivavafi.universal.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.6f),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

fun Modifier.glassMorphism(
    cornerRadius: Float = 24f,
    alpha: Float = 0.03f,
    strokeAlpha: Float = 0.08f,
    color: Color = Color.White
) = composed {
    this
        .clip(RoundedCornerShape(cornerRadius.dp))
        .background(
            Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.5f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = strokeAlpha),
                    color.copy(alpha = 0f)
                )
            ),
            shape = RoundedCornerShape(cornerRadius.dp)
        )
}

fun Modifier.glowEffect(color: Color, radius: Float = 20f, isSelected: Boolean) = composed {
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 0.5f else 0f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f),
        label = "glowAlpha"
    )

    this.drawWithContent {
        if (alpha > 0f) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    this.color = color.copy(alpha = alpha)
                    this.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(radius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = 24.dp.toPx(),
                    radiusY = 24.dp.toPx(),
                    paint = paint
                )
            }
        }
        drawContent()
    }
}
