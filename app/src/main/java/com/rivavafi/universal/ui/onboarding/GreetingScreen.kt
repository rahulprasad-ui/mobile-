package com.rivavafi.universal.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.rivavafi.universal.ui.components.RivavaLoadingOverlay
import com.rivavafi.universal.ui.components.RivavaBrandDisplay

@Composable
fun GreetingScreen(
    onNavigateNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    var isVisible by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
        delay(2500)
        viewModel.completeOnboarding()
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                RivavaBrandDisplay(showQuote = true)
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Nice to meet you, ${userName ?: ""} \uD83D\uDC4B",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Let’s organize your finances the smart way.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    RivavaLoadingOverlay(isLoading = isLoading)
}
