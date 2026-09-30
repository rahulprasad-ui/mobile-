package com.rivavafi.universal.ui.elite

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.draw.alpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteBottomSheet(
    onDismiss: () -> Unit,
    userName: String?,
    userEmail: String?
) {
    var isConnecting by remember { mutableStateOf(false) }
    var showFallbackDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val targetNumber = "+918881176909"
    val displayTargetNumber = "+91-8881176909"

    val uid = auth.currentUser?.uid ?: "unknown"
    val finalUserName = userName?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName ?: "User"
    val finalUserEmail = userEmail?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: "Not provided"

    val currentTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

    val message = """
        Hello Rivava Team,

        I am interested in applying for Rivava Elite Membership.

        Details:
        • Name: $finalUserName
        • Email: $finalUserEmail
        • User ID: $uid
        • Plan: Rivava Elite ₹3300/month
        • Timestamp: $currentTime

        I would like to proceed with Elite enrollment and private fund manager access.

        Please guide me with the next steps.
    """.trimIndent()

    val logInquiry = { method: String, eventType: String ->
        val inquiryData = hashMapOf(
            "uid" to uid,
            "name" to finalUserName,
            "email" to finalUserEmail,
            "inquiryMethod" to method,
            "createdAt" to Timestamp.now(),
            "membershipPlan" to "elite_3300",
            "eventType" to eventType
        )
        firestore.collection("elite_inquiries").add(inquiryData)
    }

    val openWhatsApp = {
        try {
            logInquiry("whatsapp", "whatsapp_opened")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$targetNumber&text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
            logInquiry("whatsapp", "inquiry_completed")
            onDismiss()
        } catch (e: Exception) {
            showFallbackDialog = true
        }
    }

    val scale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            logInquiry("whatsapp", "inquiry_started")
            delay(50) // Allow UI to update before animation
            scale.animateTo(0.95f, animationSpec = tween(100))
            scale.animateTo(1f, animationSpec = tween(100))
            delay(2000) // Simulate connecting delay
            openWhatsApp()
            isConnecting = false
        }
    }

    if (showFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showFallbackDialog = false },
            title = { Text("WhatsApp not found", color = Color(0xFFD4AF37)) },
            text = { Text("We couldn't find WhatsApp installed on your device. How would you like to proceed?", color = Color.White) },
            containerColor = Color(0xFF131313),
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Phone Number", displayTargetNumber)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Number copied to clipboard", Toast.LENGTH_SHORT).show()
                    showFallbackDialog = false
                }) {
                    Text("Copy Number", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFallbackDialog = false
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131313),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Rivava Elite",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFD4AF37) // Subtle Gold
            )

            Text(
                "₹3300 / month",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EliteBenefitRow("Auto-renew enabled")
                EliteBenefitRow("600 mins monthly included")
                EliteBenefitRow("Elite member privileges")
                EliteBenefitRow("Private video call access")
                EliteBenefitRow("Priority support")
                EliteBenefitRow("Limited 100-member access")
            }

            AnimatedVisibility(visible = isConnecting, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFFD4AF37).copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Connecting you to Rivava Elite...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !isConnecting, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Cancel", color = Color.LightGray)
                    }

                    Button(
                        onClick = { isConnecting = true },
                        modifier = Modifier.weight(1f).height(56.dp).scale(scale.value),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EliteBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFD4AF37),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}
