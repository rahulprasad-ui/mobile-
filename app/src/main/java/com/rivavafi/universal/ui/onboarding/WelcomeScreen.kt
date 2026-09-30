package com.rivavafi.universal.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.R
import com.rivavafi.universal.ui.theme.WelcomePrimaryContainer
import com.rivavafi.universal.ui.theme.WelcomePrimaryLightBlue
import com.rivavafi.universal.ui.theme.WelcomeSecondaryPink
import com.rivavafi.universal.ui.theme.WelcomeSurfaceContainerHighest
import com.rivavafi.universal.ui.theme.WelcomeSurfaceContainerLow
import com.rivavafi.universal.ui.theme.glassMorphism
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.rivavafi.universal.ui.components.RivavaLoadingOverlay
import com.rivavafi.universal.ui.components.RivavaBrandDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RivavaBrandDisplay(showQuote = true)

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Welcome to Rivava",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Let’s get to know you",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Enter your name", color = Color.White.copy(0.7f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00A3FF),
                unfocusedBorderColor = Color.White.copy(0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.saveName(name)
                    onNavigateNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A3FF),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF005D8A),
                disabledContentColor = Color.White.copy(alpha = 0.75f)
            ),
            enabled = name.isNotBlank() && !isLoading,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    RivavaLoadingOverlay(isLoading = isLoading)
}
