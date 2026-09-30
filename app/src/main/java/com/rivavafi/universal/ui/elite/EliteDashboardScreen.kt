package com.rivavafi.universal.ui.elite

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.data.repository.EliteSession
import com.rivavafi.universal.data.repository.EliteSubscription
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteDashboardScreen(
    viewModel: EliteViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val subscription by viewModel.subscription.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showBookingDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(error) {
        if (error != null) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (showBookingDialog) {
        BookSessionDialog(
            onDismiss = { showBookingDialog = false },
            onBook = { duration, date, time ->
                viewModel.bookSession(duration, date, time)
                showBookingDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elite Dashboard", color = Color(0xFFD4AF37)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    EliteHeader(subscription)
                }

                item {
                    MinutesRemainingCard(subscription)
                }

                item {
                    Button(
                        onClick = { showBookingDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Book Private Session", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (sessions.isNotEmpty()) {
                    item {
                        Text("Upcoming Sessions", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(sessions) { session ->
                        SessionItem(session = session, onJoin = { link ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        })
                    }
                }

                item {
                    SubscriptionStatusCard(
                        subscription = subscription,
                        onCancel = { viewModel.cancelSubscription() }
                    )
                }
            }
        }
    }
}

@Composable
fun EliteHeader(subscription: EliteSubscription) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFD4AF37).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Welcome, Elite Member", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (subscription.paymentStatus == "active") "Active Subscription" else "Inactive", color = Color.Gray)
        }
    }
}

@Composable
fun MinutesRemainingCard(subscription: EliteSubscription) {
    val progress = if (subscription.monthlyMinutes > 0) {
        subscription.minutesRemaining.toFloat() / subscription.monthlyMinutes.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "progress")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Minutes Remaining", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${subscription.minutesRemaining}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    " / ${subscription.monthlyMinutes} mins left",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFD4AF37),
                trackColor = Color(0xFF222222),
            )
        }
    }
}

@Composable
fun SessionItem(session: EliteSession, onJoin: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(sdf.format(Date(session.selectedDate)), color = Color.White, fontWeight = FontWeight.Bold)
                StatusChip(status = session.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${session.selectedTime} • ${session.minutesBooked} mins", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (session.status == "confirmed" && !session.meetingLink.isNullOrEmpty()) {
                Button(
                    onClick = { onJoin(session.meetingLink) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Join Meeting", color = Color.Black)
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Text(if (session.status == "pending") "Awaiting Confirmation" else "Meeting link unavailable", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "pending" -> Color(0xFFFFA500)
        "confirmed" -> Color(0xFF00E471)
        "completed" -> Color.Gray
        "cancelled", "rejected" -> Color.Red
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun SubscriptionStatusCard(subscription: EliteSubscription, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subscription Plan", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rivava Elite", color = Color.Gray)
                Text("₹3300/month", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-renew", color = Color.Gray)
                Text(if (subscription.autoRenew) "Active" else "Disabled", color = if (subscription.autoRenew) Color(0xFF00E471) else Color.Gray)
            }
            if (subscription.nextBillingDate > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Next Billing Date", color = Color.Gray)
                    Text(sdf.format(Date(subscription.nextBillingDate)), color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (subscription.autoRenew) {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel Subscription", color = Color.Red)
                }
            }
        }
    }
}
