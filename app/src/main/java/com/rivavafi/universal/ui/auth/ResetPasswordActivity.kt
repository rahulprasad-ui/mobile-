package com.rivavafi.universal.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rivavafi.universal.ui.theme.AmoledBlack
import com.rivavafi.universal.ui.theme.PrimarySky
import com.rivavafi.universal.ui.theme.RivavaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RivavaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    ResetPasswordContent(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun ResetPasswordContent(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (successMessage.isNotEmpty()) {
            Text(
                text = "Success",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PrimarySky
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = successMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySky, contentColor = AmoledBlack),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Back to Login", fontWeight = FontWeight.Bold)
            }
            return
        }

        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = PrimarySky
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enter your email address to receive a password reset link.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimarySky,
                focusedLabelColor = PrimarySky,
                cursorColor = PrimarySky
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage?.isNotEmpty() == true && successMessage.isEmpty()) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                viewModel.onForgotPassword(email) {
                    successMessage = "Reset link sent. Open email to reset password"
                    viewModel.setErrorMessage("")
                }
            },
            enabled = email.isNotBlank() && authState != AuthState.LOADING,
            colors = ButtonDefaults.buttonColors(containerColor = PrimarySky, contentColor = AmoledBlack),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (authState == AuthState.LOADING) {
                CircularProgressIndicator(color = AmoledBlack, modifier = Modifier.size(24.dp))
            } else {
                Text("Send Reset Link", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Cancel", color = PrimarySky)
        }
    }
}
