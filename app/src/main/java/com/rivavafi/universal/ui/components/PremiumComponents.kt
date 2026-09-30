package com.rivavafi.universal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rivavafi.universal.ui.theme.glassMorphism
import com.rivavafi.universal.ui.theme.glowEffect
import androidx.compose.ui.graphics.Brush
import com.rivavafi.universal.ui.theme.EmeraldGreen
import com.rivavafi.universal.ui.theme.PremiumGradientStart
import com.rivavafi.universal.ui.theme.PremiumGradientEnd
import com.rivavafi.universal.ui.theme.VibrantRed
import com.rivavafi.universal.ui.theme.bounceClick
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.foundation.clickable
import com.rivavafi.universal.domain.api.FinnhubNewsResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    colors: List<Color> = listOf(Color(0xFF3B82F6))
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.first(), contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.9f),
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    }
}

fun getRelativeTimeString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    // Finnhub timestamps are in seconds
    val timeMillis = timestamp * 1000
    val diff = now - timeMillis

    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        hours < 1 -> "Just now"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}

@Composable
fun PortfolioStockCard(
    exchange: String,
    ticker: String,
    companyName: String,
    marketPrice: String,
    isPremium: Boolean = true,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    absoluteChange: String = "+0.00",
    percentageChange: String = "+2.4%",
    isDefault: Boolean = false,
    latestNews: com.rivavafi.universal.domain.api.FinnhubNewsResponse? = null, // kept for backward compatibility if needed elsewhere
    onValueClick: ((String) -> Unit)? = null
) {
    val isNyse = exchange.equals("NYSE", ignoreCase = true)

    val primaryColor = if (isNyse) Color(0xFF3B82F6) else Color(0xFF34D399)
    val badgeBgColor = primaryColor.copy(alpha = 0.1f)

    val priceColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isPositive) Color(0xFF34D399) else Color(0xFFFF4C91),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
        label = "priceColor"
    )

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val openUrl: () -> Unit = {
        val url = if (ticker.equals("IREDA", ignoreCase = true)) {
            "https://www.google.com/search?q=IREDA+share+price"
        } else {
            val exchangeSuffix = if (isNyse) "NYSE" else "NSE"
            "https://www.google.com/search?q=$ticker+stock+price+$exchangeSuffix"
        }
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {}
    }

    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                onValueClick?.invoke("market_price")
            },
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Logo + Names
                Row(
                    modifier = Modifier.weight(1.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(badgeBgColor)
                            .clickable { openUrl() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (ticker.length > 4) ticker.take(4).uppercase() else ticker.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ticker.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFAAAAAA),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // CENTER: Price
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = marketPrice,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (isDefault) {
                            Text(
                                text = "No live data",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.Gray,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // RIGHT: % Change
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = percentageChange,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = priceColor
                    )
                    Text(
                        text = absoluteChange,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = priceColor.copy(alpha = 0.8f)
                    )
                }
            }

            // TOP-RIGHT: Link Icon
            androidx.compose.material3.IconButton(
                onClick = { openUrl() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
