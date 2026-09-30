package com.rivavafi.universal.ui.elite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rivavafi.universal.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EliteLandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EliteLandingScreen(
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun EliteLandingScreen(
    viewModel: EliteViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val eliteConfig by viewModel.eliteConfig.collectAsState()

    val seatsRemaining = (eliteConfig.totalSeats - eliteConfig.occupiedSeats).coerceAtLeast(0)
    val isFull = seatsRemaining == 0

    var isConnecting by remember { mutableStateOf(false) }


    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val targetNumber = "+918881176909"
    val displayTargetNumber = "+91-8881176909"

    val uid = auth.currentUser?.uid ?: "unknown"
    val finalUserName = auth.currentUser?.displayName ?: "User"
    val finalUserEmail = auth.currentUser?.email ?: "Not provided"

    val currentTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

    val message = """
        Hello Rivava Team,

        A user wants to connect with an advisor.

        Details:
        Name: {fullName}
        Username: {username}
        Email: {email}
        Phone: {phoneNumber}
        Preference: {preference}

        Please contact me.
    """.trimIndent()

    var fullName by remember { mutableStateOf(finalUserName) }
    var username by remember { mutableStateOf(finalUserName) }
    var email by remember { mutableStateOf(finalUserEmail) }
    var phoneNumber by remember { mutableStateOf("") }
    var preference by remember { mutableStateOf("") }
    var userPremiumStatus by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        try {
            val doc = firestore.collection("THEDATA").document(uid).get().await()
            if (doc.exists()) {
                fullName = doc.getString("name") ?: finalUserName
                username = doc.getString("name") ?: finalUserName
                email = doc.getString("email") ?: finalUserEmail
                phoneNumber = doc.getString("phone") ?: ""
                preference = doc.getString("preference") ?: ""
                userPremiumStatus = doc.getBoolean("premiumStatus") ?: false
            } else {
                val theRivDoc = firestore.collection("therivdata").document(uid).get().await()
                if (theRivDoc.exists()) {
                    userPremiumStatus = theRivDoc.getBoolean("premiumStatus") ?: false
                }
            }
        } catch (e: Exception) {}
    }

    val formattedMessage = message.replace("{fullName}", fullName).replace("{username}", username).replace("{email}", email).replace("{phoneNumber}", phoneNumber).replace("{preference}", preference)


    val logInquiry = { method: String, eventType: String ->
        val inquiryData = hashMapOf(
            "uid" to uid,
            "name" to finalUserName,
            "email" to finalUserEmail,
            "inquiryMethod" to method,
            "createdAt" to Timestamp.now(),
            "membershipPlan" to "elite_3300",
            "contactNumber" to displayTargetNumber,
            "eventType" to eventType
        )
        firestore.collection("elite_inquiries").add(inquiryData)
    }

    var showAfterDialog by remember { mutableStateOf(false) }

    val openWhatsApp = {
        logInquiry("whatsapp", "whatsapp_opened")
        com.rivavafi.universal.utils.WhatsAppUtils.openWhatsAppForAdvisor(
            context = context,
            username = finalUserName,
            email = finalUserEmail,
            phoneNumber = phoneNumber,
            preference = "Rivava Elite",
            premiumStatus = userPremiumStatus
        )
        logInquiry("whatsapp", "inquiry_completed")
        showAfterDialog = true
    }

    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            logInquiry("whatsapp", "inquiry_started")
            delay(500)
            openWhatsApp()
            isConnecting = false
        }
    }

    if (showAfterDialog) {
        AlertDialog(
            onDismissRequest = { showAfterDialog = false },
            title = { Text("Request Sent", color = Color(0xFFD4AF37)) },
            text = { Text("Your request has been sent via WhatsApp.", color = Color.White) },
            containerColor = Color(0xFF131313),
            confirmButton = {
                TextButton(onClick = {
                    showAfterDialog = false
                    openWhatsApp()
                }) {
                    Text("Contact Again", color = Color(0xFFD4AF37))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAfterDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            item {
                HeroSection(seatsRemaining, eliteConfig.totalSeats)
            }
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionWhyJoin()
            }
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionMembershipExperience()
            }
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionPricing()
            }
        }

        // Custom Back Button
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        var isProcessingPayment by remember { mutableStateOf(false) }
        var showPaymentSuccessDialog by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val eliteRepository = remember { com.rivavafi.universal.data.repository.EliteRepository() }

        if (showPaymentSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPaymentSuccessDialog = false
                    context.startActivity(Intent(context, EliteDashboardActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E471))
                        Spacer(Modifier.width(8.dp))
                        Text("🎉 Elite Unlocked!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        "Your payment of ₹399 was successful!\n\nYou now have full Rivava Elite access with 600 monthly minutes and your 1-on-1 private advisory session is ready to book.",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                },
                containerColor = Color(0xFF161616),
                confirmButton = {
                    Button(
                        onClick = {
                            showPaymentSuccessDialog = false
                            context.startActivity(Intent(context, EliteDashboardActivity::class.java))
                            (context as? android.app.Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Go to Elite Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Bottom CTA Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f),
                            Color(0xFF050505)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isProcessingPayment = true
                        coroutineScope.launch {
                            try {
                                val res = eliteRepository.createEliteOrder()
                                if (res.success && res.orderId != null) {
                                    val verify = eliteRepository.verifyElitePayment(res.orderId)
                                    isProcessingPayment = false
                                    if (verify) {
                                        showPaymentSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Payment verification pending. Please check connection.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    isProcessingPayment = false
                                    Toast.makeText(context, res.error ?: "Failed to initiate payment", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isProcessingPayment = false
                                Toast.makeText(context, "Payment error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isProcessingPayment && !isFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Processing ₹399 Payment...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else if (isFull) {
                        Text("Membership Full", color = Color.LightGray, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pay ₹399 & Unlock Elite", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("(+ Free Session)", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        com.rivavafi.universal.utils.WhatsAppUtils.openWhatsAppForAdvisor(
                            context = context,
                            username = finalUserName,
                            email = finalUserEmail,
                            phoneNumber = phoneNumber,
                            preference = "Rivava Elite (399 Offer)",
                            premiumStatus = userPremiumStatus
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
                ) {
                    Text("Chat With Advisor on WhatsApp", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Connecting you to Rivava Elite...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun HeroSection(seatsRemaining: Int, totalSeats: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Hero Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF050505))))
        ) {
            // Animated Gold Light Streaks / Glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFD4AF37).copy(alpha = alphaAnim), Color.Transparent),
                            radius = 600f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            val goldGradient = Brush.linearGradient(
                colors = listOf(Color(0xFFF3E5AB), Color(0xFFD4AF37), Color(0xFFAA7700))
            )

            Text(
                "RIVAVA ELITE",
                style = androidx.compose.ui.text.TextStyle(brush = goldGradient, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Private Fund Manager Access for Serious Investors",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Limited Seats Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD4AF37).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "LIMITED TO 100 MEMBERS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color(0xFFD4AF37)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (seatsRemaining > 0) Color(0xFF4CAF50) else Color.Red)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$seatsRemaining / $totalSeats Seats Remaining",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SectionWhyJoin() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "WHY JOIN ELITE",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = Color(0xFFD4AF37)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BenefitCard("600 mins/month with fund manager", Icons.Default.Insights)
            BenefitCard("Scheduled private video calls", Icons.Default.VideoCall)
            BenefitCard("Priority investor support", Icons.Default.SupportAgent)
            BenefitCard("Exclusive insights & strategies", Icons.Default.Diamond)
            BenefitCard("Elite investor community", Icons.Default.Groups)
        }
    }
}

@Composable
fun BenefitCard(text: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFD4AF37).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
fun SectionMembershipExperience() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "MEMBERSHIP EXPERIENCE",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = Color(0xFFD4AF37)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val steps = listOf(
            "Apply for Elite Access",
            "Get Approved by our team",
            "Activate Membership securely",
            "Schedule your private Sessions",
            "Meet Your Fund Manager"
        )

        steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (index == 0) Color(0xFFD4AF37) else Color(0xFF333333), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (index == 0) Color.Black else Color.White)
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(Color(0xFF333333))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (index == 0) Color.White else Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SectionPricing() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "RIVAVA ELITE",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "₹399 Special Access",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFD4AF37)
                )
                Text(
                    "+ Free 1-on-1 Wealth Consultation Session",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00E471)
                )
                Spacer(modifier = Modifier.height(20.dp))

                val features = listOf(
                    "Free 1-on-1 Fund Manager Session",
                    "600 Monthly Advisory Minutes",
                    "Private Video Consultations",
                    "Priority 24/7 Support",
                    "Exclusive Portfolio Insights & Strategies",
                    "Elite Community Access"
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(feature, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Payment handled in bottom sticky overlay instead

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Secure. Private. Exclusive.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
