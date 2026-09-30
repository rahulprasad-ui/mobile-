package com.rivavafi.universal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PremiumUnlockAnimation(
    onAnimationComplete: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(300)
        step = 1 // Lock turns to unlocked
        delay(600)
        step = 2 // Checkmark appears
        delay(800)
        onAnimationComplete()
    }

    val iconScale by animateFloatAsState(
        targetValue = if (step >= 1) 1.2f else 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "iconScale"
    )

    val checkAlpha by animateFloatAsState(
        targetValue = if (step >= 2) 1f else 0f,
        animationSpec = tween(400),
        label = "checkAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111).copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF00E471).copy(alpha = glowAlpha * 0.3f), CircleShape)
                )

                // Icons
                androidx.compose.animation.AnimatedVisibility(
                    visible = step == 0,
                    enter = fadeIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = step == 1,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlocked",
                        tint = Color(0xFF00E471),
                        modifier = Modifier.size(56.dp)
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = step == 2,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF00E471),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Premium Unlocked!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.alpha(checkAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preparing your insights...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(checkAlpha)
            )
        }
    }
}
