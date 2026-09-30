package com.rivavafi.universal.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.ui.theme.AmoledBlack
import com.rivavafi.universal.ui.theme.PrimarySky
import com.rivavafi.universal.ui.components.RivavaLoadingOverlay
import com.rivavafi.universal.ui.components.RivavaBrandDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputScreen(
    onNavigateNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (it.isNotEmpty()) {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RivavaBrandDisplay(showQuote = true)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Add your number",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Kindly add the correct phone no. else the data can be degraded.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                val digitsOnly = it.filter { char -> char.isDigit() }
                if (digitsOnly.length <= 10) {
                    phoneNumber = digitsOnly
                }
            },
            label = { Text("Phone Number (10 digits)", color = Color.White.copy(0.7f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
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
                val formattedNumber = phoneNumber.trim()
                if (formattedNumber.length == 10) {
                    showConfirmDialog = true
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
            enabled = phoneNumber.length == 10 && !isLoading,
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



    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(text = "Confirm Number", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(text = "Is this your correct phone number?", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "+91 $phoneNumber", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimarySky)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.savePhoneAndCompleteOnboarding(phoneNumber) {
                        onNavigateNext()
                    }
                }) {
                    Text("Yes, Continue", fontWeight = FontWeight.Bold, color = PrimarySky)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Edit Number", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = AmoledBlack,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    RivavaLoadingOverlay(isLoading = isLoading)
}
