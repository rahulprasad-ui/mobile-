package com.rivavafi.universal.ui.auth

import android.content.Intent
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rivavafi.universal.ui.theme.AmoledBlack
import com.rivavafi.universal.ui.theme.PrimarySky
import com.rivavafi.universal.ui.theme.RivavaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetNewPasswordActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var token = intent.getStringExtra("token")
        var email = intent.getStringExtra("email")
        var oobCode = intent.getStringExtra("oobCode")

        if (intent.data != null) {
            val data = intent.data!!
            if (token.isNullOrBlank()) token = data.getQueryParameter("token")
            if (email.isNullOrBlank()) email = data.getQueryParameter("email")
            if (oobCode.isNullOrBlank()) oobCode = data.getQueryParameter("oobCode")
            
            if (oobCode.isNullOrBlank()) {
                val deepLink = data.getQueryParameter("link")
                if (!deepLink.isNullOrBlank()) {
                    val parsedNested = android.net.Uri.parse(deepLink)
                    if (token.isNullOrBlank()) token = parsedNested.getQueryParameter("token")
                    if (email.isNullOrBlank()) email = parsedNested.getQueryParameter("email")
                    if (oobCode.isNullOrBlank()) oobCode = parsedNested.getQueryParameter("oobCode")
                }
            }
        }

        if (token.isNullOrBlank() && oobCode.isNullOrBlank()) {
            Toast.makeText(this, "Invalid or expired password reset link.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            RivavaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    SetNewPasswordContent(
                        viewModel = viewModel,
                        email = email ?: "",
                        token = token ?: "",
                        oobCode = oobCode ?: "",
                        onSuccess = {
                            Toast.makeText(this, "Password reset successful. Please log in.", Toast.LENGTH_LONG).show()
                            val loginIntent = Intent(this, AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(loginIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SetNewPasswordContent(
    viewModel: AuthViewModel,
    email: String,
    token: String,
    oobCode: String,
    onSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSuccess) {
            Text(
                text = "Password Reset Done! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PrimarySky,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your password has been updated successfully. You can now log in with your new password.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onSuccess,
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySky, contentColor = AmoledBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Back to Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            return
        }

        Text(
            text = "Create New Password",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimarySky,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter a strong password of at least 6 characters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                validationError = null
            },
            label = { Text("New Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimarySky,
                focusedLabelColor = PrimarySky,
                cursorColor = PrimarySky
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                validationError = null
            },
            label = { Text("Confirm New Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimarySky,
                focusedLabelColor = PrimarySky,
                cursorColor = PrimarySky
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        val activeError = validationError ?: errorMessage
        if (!activeError.isNullOrBlank()) {
            Text(
                text = activeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                when {
                    newPassword.length < 6 -> {
                        validationError = "Password must be at least 6 characters."
                    }
                    newPassword != confirmPassword -> {
                        validationError = "Passwords do not match."
                    }
                    token.isNotBlank() && email.isNotBlank() -> {
                        viewModel.resetPasswordWithCustomToken(email, token, newPassword) {
                            isSuccess = true
                        }
                    }
                    oobCode.isNotBlank() -> {
                        viewModel.resetPasswordWithActionCode(oobCode, newPassword) {
                            isSuccess = true
                        }
                    }
                    else -> {
                        validationError = "Invalid reset token."
                    }
                }
            },
            enabled = authState != AuthState.LOADING && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimarySky, contentColor = AmoledBlack),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (authState == AuthState.LOADING) {
                CircularProgressIndicator(color = AmoledBlack, modifier = Modifier.size(24.dp))
            } else {
                Text("Set New Password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
