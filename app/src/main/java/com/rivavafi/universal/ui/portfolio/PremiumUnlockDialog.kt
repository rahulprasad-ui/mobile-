package com.rivavafi.universal.ui.portfolio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.ui.theme.EmeraldGreen
import com.rivavafi.universal.utils.SecretKeyValidator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUnlockDialog(
    userName: String = "User",
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit,
    onPayClick: (() -> Unit)? = null,
    secretKeyToMatch: String = "",
    viewModel: PremiumViewModel = hiltViewModel()
) {
    var secretKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(UnlockStep.Main) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val keyState by viewModel.keyVerificationState.collectAsState()

    LaunchedEffect(keyState) {
        when (val state = keyState) {
            is KeyVerificationState.Verifying -> {
                currentStep = UnlockStep.Verifying
            }
            is KeyVerificationState.Success -> {
                currentStep = UnlockStep.Success
            }
            is KeyVerificationState.Error -> {
                currentStep = UnlockStep.Main
                errorMessage = state.errorMessage
            }
            is KeyVerificationState.Idle -> {
                // Idle
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (currentStep != UnlockStep.Verifying) {
                viewModel.resetKeyVerificationState()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131313))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Unlock Flow"
            ) { step ->
                when (step) {
                    UnlockStep.Main -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF3B82F6), shape = RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                "Upgrade to Rivava Premium",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Dedicated financial tools & analytics",
                                    "Unlimited real-time tracking & reports",
                                    "Secure encrypted cloud backups"
                                ).forEach { benefit ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00E471),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = benefit,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Enter Secret Access Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = secretKeyInput,
                                onValueChange = {
                                    secretKeyInput = SecretKeyValidator.normalize(it)
                                    errorMessage = null
                                },
                                label = { Text("License / Access Key (e.g. RIV-XXXX-XXXX)", color = Color.White.copy(0.7f)) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (secretKeyInput.length >= 6) {
                                            viewModel.verifyAndRedeemSecretKey(secretKeyInput)
                                        } else {
                                            errorMessage = "Please enter a valid secret key format."
                                        }
                                    }
                                ),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    val description = if (passwordVisible) "Hide key" else "Show key"

                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = description, tint = Color.White.copy(0.7f))
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                isError = errorMessage != null
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "Invalid Key",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (secretKeyInput.length >= 6) {
                                        viewModel.verifyAndRedeemSecretKey(secretKeyInput)
                                    } else {
                                        errorMessage = "Please enter a valid secret key format."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
                            ) {
                                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Verify & Unlock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }

                            Button(
                                onClick = {
                                    viewModel.startPremiumPurchase(amountPaise = 39900)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color.Black),
                                shape = RoundedCornerShape(18.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
                            ) {
                                Text("Pay ₹399 & Unlock Instantly", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetKeyVerificationState()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B7280))
                            ) {
                                Text(
                                    "Cancel",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }

                    UnlockStep.Verifying -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(color = EmeraldGreen)
                            Text(
                                "Verifying key with secure server...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Authenticating license & entitlement...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    UnlockStep.Success -> {
                        LaunchedEffect(Unit) {
                            delay(1800)
                            viewModel.resetKeyVerificationState()
                            onUnlockSuccess()
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFF00E471).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                            }

                            Text(
                                "Rivava+ Premium Unlocked!",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                "Your license key has been verified and activated on your account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class UnlockStep {
    Main, Verifying, Success
}
