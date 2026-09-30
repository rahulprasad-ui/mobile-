package com.rivavafi.universal.ui.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.theme.EmeraldGreen
import com.rivavafi.universal.ui.theme.VibrantRed
import com.rivavafi.universal.ui.theme.glassMorphism

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun PortfolioMetricsTable(
    ticker: String,
    exchange: String,
    change: String,
    position: String,
    avgVolume: String,
    avgPrice: String,
    lastPrice: String,
    costBasis: String,
    dayHigh: String,
    dayLow: String,
    openPrice: String,
    pnl: String,
    pnlPercent: String,
    unrealizedPnl: String,
    isPositive: Boolean,
    focusedMetric: String? = null
) {
    val valueColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isPositive) EmeraldGreen else VibrantRed,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500)
    )
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val openUrl: () -> Unit = {
        val exchangeSuffix = if (exchange.equals("NSE", ignoreCase = true)) "NSE" else "NYSE"
        val url = "https://www.google.com/search?q=$ticker+stock+price+$exchangeSuffix"
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .glassMorphism(cornerRadius = 16f, alpha = 0.1f)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ticker,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "($exchange)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        MetricRow("Change", change, Icons.AutoMirrored.Filled.TrendingUp, EmeraldGreen, valueColor, openUrl, isFocused = focusedMetric == "change")
        MetricRow("Position", position, Icons.Default.PieChart, MaterialTheme.colorScheme.primary, onClick = openUrl, isFocused = focusedMetric == "position")
        MetricRow("Avg Volume", avgVolume, Icons.Default.BarChart, MaterialTheme.colorScheme.primaryContainer, onClick = openUrl, isFocused = focusedMetric == "avgVolume")
        MetricRow("Avg Price", avgPrice, Icons.Default.Payments, MaterialTheme.colorScheme.secondary, onClick = openUrl, isFocused = focusedMetric == "avgPrice")
        MetricRow("Last Price", lastPrice, Icons.Default.Sell, MaterialTheme.colorScheme.primary, onClick = openUrl, isFocused = focusedMetric == "lastPrice")
        MetricRow("Day High", dayHigh, Icons.Default.ArrowUpward, EmeraldGreen, EmeraldGreen, openUrl, isFocused = focusedMetric == "dayHigh")
        MetricRow("Day Low", dayLow, Icons.Default.ArrowDownward, VibrantRed, VibrantRed, openUrl, isFocused = focusedMetric == "dayLow")
        MetricRow("Open Price", openPrice, Icons.Default.ArrowUpward, MaterialTheme.colorScheme.outline, onClick = openUrl, isFocused = focusedMetric == "openPrice")
        MetricRow("Cost Basis", costBasis, Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.secondary, onClick = openUrl, isFocused = focusedMetric == "costBasis")
        MetricRow("P&L", pnl, Icons.Default.AddChart, EmeraldGreen, valueColor, openUrl, isFocused = focusedMetric == "pnl")
        MetricRow("P&L %", pnlPercent, Icons.Default.Percent, EmeraldGreen, valueColor, openUrl, isFocused = focusedMetric == "pnlPercent")
        MetricRow("Unrealized P&L %", unrealizedPnl, Icons.Default.PieChart, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary, openUrl, isFocused = focusedMetric == "unrealizedPnl")
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    isFocused: Boolean = false
) {
    val glowAlpha = remember { Animatable(0f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            glowAlpha.animateTo(0.2f, tween(300))
            glowAlpha.animateTo(0f, tween(1500))
        }
    }

    Column(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha.value)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = if (isFocused) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = valueColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Details",
                        tint = valueColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .bounceClick { onClick() }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

@Composable
fun CashBalanceSection(usdCash: String, totalCash: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Cash Balances",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .glassMorphism(cornerRadius = 16f, alpha = 0.1f)
                .padding(16.dp)
        ) {
            BalanceRow("INR Cash", usdCash, Icons.Default.AttachMoney, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), "CURRENCY BALANCE")
            BalanceRow("Total Cash", totalCash, Icons.Default.Savings, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), "AGGREGATE", showDivider = false)
        }
    }
}

@Composable
fun BalanceRow(label: String, value: String, icon: ImageVector, iconColor: Color, iconBgColor: Color, subtitle: String, showDivider: Boolean = true) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    uriHandler.openUri("https://www.google.com/search?q=$label+exchange+rate")
                } catch (e: Exception) {}
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBgColor, shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Market Value",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}
