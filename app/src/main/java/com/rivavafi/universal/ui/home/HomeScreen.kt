package com.rivavafi.universal.ui.home

import java.util.Calendar
import java.util.Locale

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.rivavafi.universal.R
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.usecase.FinancialSummaryState
import com.rivavafi.universal.ui.add.AddTransactionBottomSheet
import com.rivavafi.universal.ui.theme.CategoryVisuals
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.theme.glassMorphism
import com.rivavafi.universal.ui.theme.glowEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lock
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.rivavafi.universal.ui.portfolio.PasswordDialog
import androidx.compose.material.icons.filled.Person

import com.rivavafi.universal.ui.components.PremiumCard
import com.rivavafi.universal.ui.components.SectionHeader
import androidx.compose.ui.layout.ContentScale

import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: com.rivavafi.universal.ui.profile.ProfileViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    onNavigateToRivavaPortfolio: () -> Unit = {},
    onNavigateToCalculators: () -> Unit = {}
) {
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dailyBudget by viewModel.dailyBudget.collectAsState()
    val layoutPreset by viewModel.homeLayoutPreset.collectAsState()
    val showDetails by viewModel.showSmsDetails.collectAsState()
    val viewModelUserName by viewModel.userName.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val viewModelProfileImageUri by viewModel.profileImageUri.collectAsState()
    val eliteConfig by viewModel.eliteConfig.collectAsState()
    val eliteSubscription by viewModel.eliteSubscription.collectAsState()

    val profileState by profileViewModel.profileState.collectAsState()
    val userModel = profileState.userModel

    val context = LocalContext.current
    val cachedUser = remember { com.rivavafi.universal.utils.PrefsManager(context).getUser() }
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    val userName = userModel?.name?.takeIf { it.isNotBlank() }
        ?: viewModelUserName?.takeIf { it.isNotBlank() }
        ?: cachedUser?.name?.takeIf { it.isNotBlank() }
        ?: authUser?.displayName?.takeIf { it.isNotBlank() }
    val profileImageUri = userModel?.profileImage ?: viewModelProfileImageUri ?: cachedUser?.photo ?: authUser?.photoUrl?.toString()
    var showAddSheet by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showVideoCallDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", android.content.Context.MODE_PRIVATE)

    // Removed showEliteBottomSheet logic as we now navigate directly to EliteLandingActivity
    val isPremiumPref = prefs.getBoolean("isPremium", false)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 100.dp) // Extra spacing to ensure it completely clears the floating nav bar
                    .size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(32.dp))
            }
        },
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            // Removed header top bar as requested
        },
        containerColor = Color(0xFF0A0A0A) // Deep Black / Amoled Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp), // Provide enough bottom padding for the floating nav bar
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Bar: Logo on Left, Profile Avatar on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rivava_logo),
                            contentDescription = "Rivava Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Profile Icon Top Right
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                                .clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val initial = if (!userName.isNullOrEmpty()) userName.first().toString().uppercase() else ""
                                    if (initial.isNotEmpty()) {
                                        Text(
                                            text = initial,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile Avatar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    val decodedBitmap = remember(profileImageUri) {
                                        if (profileImageUri.startsWith("data:image")) {
                                            try {
                                                val base64String = profileImageUri.substringAfter("base64,")
                                                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        } else null
                                    }

                                    if (decodedBitmap != null) {
                                        Image(
                                            bitmap = decodedBitmap.asImageBitmap(),
                                            contentDescription = "Profile Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        coil.compose.AsyncImage(
                                            model = profileImageUri,
                                            contentDescription = "Profile Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.rivava_logo),
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (!userName.isNullOrEmpty()) "Welcome back, $userName" else "Welcome to Rivava+",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Smart financial insights and portfolio tracking.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Your next step to independence",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Take control of your financial future with clarity.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { onNavigateToRivavaPortfolio() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Take a step", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                val seatsRemaining = (eliteConfig.totalSeats - eliteConfig.occupiedSeats).coerceAtLeast(0)
                val isFull = seatsRemaining == 0

                // Ultra-Premium Rivava Elite Card Redesign
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            if (eliteSubscription.isElite) {
                                context.startActivity(Intent(context, com.rivavafi.universal.ui.elite.EliteDashboardActivity::class.java))
                            } else {
                                val intent = Intent(context, com.rivavafi.universal.ui.elite.EliteLandingActivity::class.java)
                                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out)
                                context.startActivity(intent, options.toBundle())
                            }
                        }
                        .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Subtle shimmer/glow background effect
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFD4AF37).copy(alpha = 0.03f))
                        )

                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Premium",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "EXCLUSIVE MEMBERSHIP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                        color = Color(0xFFD4AF37)
                                    )
                                }
                                if (eliteSubscription.isElite) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD4AF37).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("MEMBER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD4AF37))
                                    }
                                } else {
                                    Text(
                                        text = if (isFull) "FULL" else "$seatsRemaining/100",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isFull) Color.Red else Color(0xFFD4AF37)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                "RIVAVA ELITE",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Private Wealth Guidance for Serious Investors",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Features Chips
                            @Composable
                            fun FeatureChip(text: String) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FeatureChip("600 Mins/mo")
                                FeatureChip("Video Consults")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FeatureChip("Priority Support")
                                FeatureChip("Elite Access")
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (!eliteSubscription.isElite) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "₹3300/month",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Button(
                                        onClick = {
                                            if (!isFull) {
                                                val intent = Intent(context, com.rivavafi.universal.ui.elite.EliteLandingActivity::class.java)
                                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out)
                                context.startActivity(intent, options.toBundle())
                                            }
                                        },
                                        enabled = !isFull,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD4AF37),
                                            disabledContainerColor = Color.DarkGray
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            if (isFull) "Membership Full" else "Apply for Access",
                                            color = if (isFull) Color.LightGray else Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(context, com.rivavafi.universal.ui.elite.EliteDashboardActivity::class.java))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Enter Elite Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Financial Calculators Quick Access
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToCalculators() }
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧮", fontSize = 22.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Financial Tools & Calculators",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Percentage, Ratio, P&L, MDR, EMI, CI & History",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Open →",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Talk to Rivava",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918881176909"))
                                context.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Call", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clickable { showVideoCallDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Video Call", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clickable { showChatDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Chat", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isPremiumUser && !isPremiumPref) {
                item {
                    PremiumCard(
                        modifier = Modifier
                            .heightIn(min = 120.dp)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToRivavaPortfolio()
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rivava Premium Portfolio",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onNavigateToRivavaPortfolio() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C91), contentColor = Color.White),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        text = "Unlock Premium",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Premium Feature",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard Overview",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                DashboardOverviewBento(summary = summary)
            }

            when (layoutPreset) {
                "Minimal" -> {
                    item {
                        RealBalanceCard(transactions = transactions)
                    }
                }
                "Analytics" -> {
                    item {
                        SpendingSummaryCards(transactions = transactions)
                    }
                    item {
                        RealBalanceCard(transactions = transactions)
                    }
                }
                "Daily Tracker" -> {
                    item {
                        DailyBudgetCard(
                            transactions = transactions,
                            dailyBudget = dailyBudget,
                            onBudgetUpdate = { newBudget -> viewModel.updateDailyBudget(newBudget) }
                        )
                    }
                    item {
                        SpendingSummaryCards(transactions = transactions)
                    }
                }
                "Subscription View" -> {
                    item {
                        com.rivavafi.universal.ui.analytics.SubscriptionTrackerCard(transactions = transactions)
                    }
                }
                else -> {
                    item {
                        DailyBudgetCard(
                            transactions = transactions,
                            dailyBudget = dailyBudget,
                            onBudgetUpdate = { newBudget -> viewModel.updateDailyBudget(newBudget) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(transactions.take(5), key = { it.id }) { transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        TransactionItem(transaction, showDetails = showDetails, onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToTransactionDetail(transaction.id)
                        })
                    }
                }
            }
        }

        if (showVideoCallDialog) {
            var name by remember { mutableStateOf("") }
            var preferredTime by remember { mutableStateOf("") }
            var contactInfo by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showVideoCallDialog = false },
                title = { Text("Schedule a Video Call") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fill out the details below to book a consultation via WhatsApp.")
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        OutlinedTextField(
                            value = preferredTime,
                            onValueChange = { preferredTime = it },
                            label = { Text("Preferred Time (e.g. 2:00 PM)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        OutlinedTextField(
                            value = contactInfo,
                            onValueChange = { contactInfo = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank() && preferredTime.isNotBlank() && contactInfo.isNotBlank()) {
                            showVideoCallDialog = false
                            val message = "Hi, I want to schedule a video call.\nName: $name\nPreferred Time: $preferredTime\nContact: $contactInfo"
                            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/918881176909?text=$encodedMessage"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVideoCallDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }


        if (showChatDialog) {
            var message by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showChatDialog = false },
                title = { Text("Chat with Rivava") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Send us a message and our support team will get back to you.")
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Your Message") },
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showChatDialog = false
                        Toast.makeText(context, "Message sent!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChatDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddSheet) {
            val dynamicCategories by viewModel.categories.collectAsState()
            AddTransactionBottomSheet(
                categories = dynamicCategories.map { it.name }.ifEmpty { listOf("General") },
                onDismiss = { showAddSheet = false },
                onSave = { title, amount, type, category, subcategory, date ->
                    viewModel.addTransaction(title, amount, type, category, subcategory, date)
                    showAddSheet = false
                },
                onAddCategory = { newCategoryName, type ->
                    viewModel.addCategory(newCategoryName, type)
                }
            )
        }

        if (showPasswordDialog) {
            PasswordDialog(
                onDismiss = { showPasswordDialog = false },
                onUnlock = { password ->
                    if (password.trim() == userName?.trim()) {
                        showPasswordDialog = false
                        viewModel.setPremiumUser(true)
                        Toast.makeText(context, "Premium Unlocked!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun SpendingSummaryCards(transactions: List<TransactionEntity>) {
    val weekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var weeklySpending = 0.0
    var monthlySpending = 0.0

    transactions.forEach {
        if (it.type == "DEBIT" || it.type == "EXPENSE" || it.type == "BILL_PENDING") {
            if (it.date >= weekStart.toLong()) weeklySpending += it.amount
            if (it.date >= monthStart.toLong()) monthlySpending += it.amount
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SpendingCard("This Week", weeklySpending, Modifier.weight(1f))
        SpendingCard("This Month", monthlySpending, Modifier.weight(1f))
    }
}

@Composable
fun SpendingCard(title: String, amount: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.glassMorphism(cornerRadius = 24f, alpha = 0.15f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", amount),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}

@Composable
fun DailyBudgetCard(
    transactions: List<TransactionEntity>,
    dailyBudget: Double,
    onBudgetUpdate: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(dailyBudget.toString()) }

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var spentToday = 0.0
    transactions.forEach {
        if ((it.type == "DEBIT" || it.type == "EXPENSE" || it.type == "BILL_PENDING") && it.date >= todayStart) {
            spentToday += it.amount
        }
    }

    val remaining = (dailyBudget - spentToday).coerceAtLeast(0.0)
    val progress = if (dailyBudget > 0) (spentToday / dailyBudget).toFloat().coerceIn(0f, 1f) else 1f

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Daily Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    label = { Text("Budget Amount") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newBudget = budgetInput.toDoubleOrNull()
                        if (newBudget != null && newBudget >= 0) {
                            onBudgetUpdate(newBudget)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .glassMorphism(cornerRadius = 28f, alpha = 0.15f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Budget", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${String.format(Locale.getDefault(), "%.0f", dailyBudget)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            budgetInput = dailyBudget.toString()
                            showEditDialog = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "₹${String.format(Locale.getDefault(), "%.0f", remaining)}",
                color = if (remaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text("Remaining Today", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Spent: ₹${String.format(Locale.getDefault(), "%.0f", spentToday)}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first credit or debit",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DashboardOverviewBento(summary: FinancialSummaryState) {
    val netWorth = summary.totalCredit + summary.netSavings
    val savings = summary.netSavings
    val investments = summary.totalCredit

    val netWorthChange = if (netWorth > 0) "+4.2%" else "0.0%"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Net Worth Card (full width)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .glassMorphism(cornerRadius = 24f, alpha = 0.15f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "NET WORTH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹${String.format(java.util.Locale.getDefault(), "%.0f", netWorth)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = netWorthChange,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Row for Savings & Investments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Savings Card (half width)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161616)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Savings",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Savings",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format(java.util.Locale.getDefault(), "%.0f", savings)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Investments Card (half width)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161616)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "Investments",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Investments",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format(java.util.Locale.getDefault(), "%.0f", investments)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RealBalanceCard(transactions: List<TransactionEntity>) {
    val bankBalances = remember(transactions) {
        val balances = mutableMapOf<String, Double>()
        // transactions are already ordered by timestamp desc, so first found is latest
        transactions.forEach { t ->
            if (t.bankName != null && t.availableBalance != null) {
                if (!balances.containsKey(t.bankName)) {
                    balances[t.bankName] = t.availableBalance
                }
            }
        }
        balances
    }

    if (bankBalances.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .glassMorphism(cornerRadius = 28f, alpha = 0.2f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Estimated Bank Balance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                bankBalances.forEach { (bank, balance) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bank,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₹" + String.format(java.util.Locale.getDefault(), "%.2f", balance),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, showDetails: Boolean = true, onClick: () -> Unit) {
    val isCredit = transaction.type == "CREDIT" || transaction.type == "INCOME" || transaction.type == "REWARD" || transaction.type == "INCOME" || transaction.type == "REWARD"
    val amountColor = if (isCredit) Color(0xFF4CAF50) else Color(0xFFE53935)

    val categoryVisual = CategoryVisuals.getCategoryVisual(transaction.category)
    val subCategoryVisual = transaction.subcategory?.let { CategoryVisuals.getSubcategoryVisual(it) }

    val visualToUse = subCategoryVisual ?: categoryVisual

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .glassMorphism(cornerRadius = 24f, alpha = 0.15f)
            .bounceClick { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(visualToUse.color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visualToUse.icon,
                contentDescription = null,
                tint = visualToUse.color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (showDetails) transaction.merchantName else "Hidden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showDetails) visualToUse.title else "Hidden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualToUse.color,
                    modifier = Modifier
                        .background(visualToUse.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val dateString = if (showDetails) formatter.format(java.util.Date(transaction.date)) else "****"
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showDetails && !transaction.bankName.isNullOrBlank()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = transaction.bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = "${if (isCredit) "+" else "-"}₹${String.format(java.util.Locale.getDefault(), "%.0f", transaction.amount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}
