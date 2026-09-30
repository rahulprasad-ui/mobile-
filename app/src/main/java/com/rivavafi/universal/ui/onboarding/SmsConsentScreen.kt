package com.rivavafi.universal.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.sms.SmsInboxScanner
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.rivavafi.universal.sms.SmsTrackingMode
import com.rivavafi.universal.ui.components.RivavaBrandDisplay
import com.rivavafi.universal.ui.components.RivavaLoadingOverlay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConsentScreen(
    onNavigateNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var selectedMode by remember { mutableStateOf(SmsTrackingMode.OFF) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // We get the scanner via DI if we can, but since this is inside a composable, we typically use the ViewModel.
    // We will let the ViewModel trigger the scan instead. Wait, let's add it to the ViewModel.

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true && permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            viewModel.setSmsTrackingMode(selectedMode.name)
            viewModel.scanSmsInbox(context, selectedMode, onSuccess = {
                onNavigateNext()
            })
        } else {
            viewModel.setSmsTrackingMode(SmsTrackingMode.OFF.name)
            showSettingsDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RivavaBrandDisplay(showQuote = false)
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Automate Your Tracker",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Allow Rivava to read bank SMS alerts to automatically log your transactions. We only process financial messages locally on your device.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Options
        val options = listOf(
            SmsTrackingMode.BOTH to "Both (Credit & Debit)",
            SmsTrackingMode.CREDIT_ONLY to "Credit Only",
            SmsTrackingMode.DEBIT_ONLY to "Debit Only"
        )

        options.forEach { (mode, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { selectedMode = mode }
                    .background(
                        if (selectedMode == mode) Color(0xFF00A3FF).copy(alpha = 0.2f) else Color.White.copy(0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00A3FF))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(label, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))



        Button(
            onClick = {
                if (selectedMode != SmsTrackingMode.OFF) {
                    showExplanationDialog = true
                } else {
                    viewModel.setSmsTrackingMode(SmsTrackingMode.OFF.name)
                    onNavigateNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A3FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                if (selectedMode == SmsTrackingMode.OFF) "Skip" else "Allow & Continue",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = {
            viewModel.setSmsTrackingMode(SmsTrackingMode.OFF.name)
            onNavigateNext()
        }) {
            Text("Maybe Later", color = Color.White.copy(alpha = 0.6f))
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
                onNavigateNext()
            },
            title = { Text("Permission Denied") },
            text = { Text("It looks like SMS permissions were denied. You can manually enable them in the app settings, or skip for now.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onNavigateNext()
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    onNavigateNext()
                }) {
                    Text("Skip")
                }
            }
        )
    }


    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("Allow SMS Permission") },
            text = { Text("To automatically log your transactions, Rivava needs permission to read incoming bank SMS messages.\n\n1. Tap 'Continue' below.\n2. When prompted by your device, tap 'Allow'.") },
            confirmButton = {
                TextButton(onClick = {
                    showExplanationDialog = false
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    RivavaLoadingOverlay(isLoading = isLoading)
}
