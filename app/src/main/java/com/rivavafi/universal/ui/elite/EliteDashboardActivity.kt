package com.rivavafi.universal.ui.elite

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.rivavafi.universal.data.repository.EliteRepository

import com.rivavafi.universal.ui.portfolio.PremiumUnlockDialog
import com.rivavafi.universal.utils.SecretConfig
import com.rivavafi.universal.utils.WhatsAppUtils
import com.google.firebase.auth.FirebaseAuth

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EliteDashboardActivity : ComponentActivity() {

    @Inject
    lateinit var eliteRepository: EliteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val auth = FirebaseAuth.getInstance()
            val profileViewModel: com.rivavafi.universal.ui.profile.ProfileViewModel = hiltViewModel()

            val profileState by profileViewModel.profileState.collectAsState()

            val userModel = profileState.userModel

            val userPhone = userModel?.phone?.takeIf { it.isNotBlank() } ?: userModel?.phoneno?.takeIf { it.isNotBlank() } ?: auth.currentUser?.phoneNumber ?: ""
            var showSecretDialog by remember { mutableStateOf(false) }
            val prefs = getSharedPreferences("RivavaElitePrefs", MODE_PRIVATE)
            var isEliteUnlocked by remember { mutableStateOf(prefs.getBoolean("elite_unlocked", false)) }
            var showWhatsAppDialog by remember { mutableStateOf(false) }

            if (!isEliteUnlocked) {
                showSecretDialog = true
                if (showSecretDialog) {
                        PremiumUnlockDialog(
                        onDismiss = { finish() },
                        onUnlockSuccess = {
                            prefs.edit().putBoolean("elite_unlocked", true).apply()
                            isEliteUnlocked = true
                            showSecretDialog = false
                        },
                        onPayClick = {
                            WhatsAppUtils.openWhatsAppForAdvisor(
                                context = this@EliteDashboardActivity,
                                username = auth.currentUser?.displayName ?: "User",
                                email = auth.currentUser?.email ?: "",
                                phoneNumber = userPhone,
                                preference = "Rivava Elite",
                                premiumStatus = false
                            )
                            showWhatsAppDialog = true
                        }
                    )
                }

                if (showWhatsAppDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showWhatsAppDialog = false },
                        title = { androidx.compose.material3.Text("Contact Advisor", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        text = { androidx.compose.material3.Text("Did you connect with the advisor successfully?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showWhatsAppDialog = false
                                WhatsAppUtils.openWhatsAppForAdvisor(
                                    context = this@EliteDashboardActivity,
                                    username = auth.currentUser?.displayName ?: "User",
                                    email = auth.currentUser?.email ?: "",
                                    phoneNumber = userPhone,
                                    preference = "Rivava Elite",
                                    premiumStatus = false
                                )
                            }) {
                                androidx.compose.material3.Text("Contact Again", color = Color(0xFFD4AF37))
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showWhatsAppDialog = false }) {
                                androidx.compose.material3.Text("Close", color = Color.Gray)
                            }
                        },
                        containerColor = Color(0xFF1E1E1E),
                        titleContentColor = Color.White,
                        textContentColor = Color.White
                    )
                }
            } else {
                EliteDashboardScreen(
                    onBack = { finish() }
                )
            }
        }

    }
}
