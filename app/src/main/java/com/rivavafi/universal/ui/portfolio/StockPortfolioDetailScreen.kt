package com.rivavafi.universal.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.rivavafi.universal.ui.portfolio.components.CashBalanceSection
import com.rivavafi.universal.ui.portfolio.components.PortfolioMetricsTable
import kotlinx.coroutines.delay
import com.rivavafi.universal.ui.theme.glassMorphism
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.shape.CircleShape
import com.rivavafi.universal.ui.theme.PremiumGradientStart
import com.rivavafi.universal.ui.components.PremiumButton

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.WindowManager
import android.content.ContextWrapper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPortfolioDetailScreen(
    ticker: String,
    initialFocus: String? = null,
    onBack: () -> Unit,
    onNavigateToPdfViewer: ((String) -> Unit)? = null,
    viewModel: StockViewModel = hiltViewModel()
) {
    val exchange = if (ticker == "RTX" || ticker == "WMT") "NYSE" else "NSE"

    val stockStates by viewModel.stockStates.collectAsState()

    val context = LocalContext.current


    LaunchedEffect(ticker) {
        viewModel.startPolling(listOf(ticker))
    }

    val symbolKey = if (ticker == "IREDA") "IREDA.NS" else ticker
    val stockData = stockStates[symbolKey]?.data

    val uriHandler = LocalUriHandler.current

    val lastPrice = stockData?.c ?: (if (ticker == "RTX") 100.00 else 150.00)
    val previousClose = stockData?.pc ?: (if (ticker == "RTX") 99.00 else 148.00)
    val changeValue = lastPrice - previousClose
    val pnlPercent = if (previousClose != 0.0) (changeValue / previousClose) * 100 else 0.0
    val dayHigh = stockData?.h ?: (lastPrice + 5.0)
    val dayLow = stockData?.l ?: (lastPrice - 5.0)
    val openPrice = stockData?.o ?: previousClose

    val isPositive = changeValue >= 0
    val pnl = changeValue * 2.99 // Simulated position P&L

    val companyProfiles by viewModel.companyProfiles.collectAsState()
    val companyNews by viewModel.companyNews.collectAsState()

    val profile = companyProfiles[symbolKey]?.profile
    val newsList = companyNews[symbolKey] ?: emptyList()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("$ticker ($exchange)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add to favorites */ }) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
            if (profile != null) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (!profile.logo.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = profile.logo,
                            contentDescription = profile.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column {
                        Text(
                            text = profile.name ?: ticker,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = profile.finnhubIndustry ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            PortfolioMetricsTable(
                ticker = ticker,
                exchange = exchange,
                change = String.format("%s%.2f", if (isPositive) "+" else "", changeValue),
                position = "2.99",
                avgVolume = "6.10M",
                avgPrice = "119.31",
                lastPrice = String.format("%.2f", lastPrice),
                dayHigh = String.format("%.2f", dayHigh),
                dayLow = String.format("%.2f", dayLow),
                openPrice = String.format("%.2f", openPrice),
                costBasis = "356.74",
                pnl = String.format("%s%.2f", if (isPositive) "+" else "", pnl),
                pnlPercent = String.format("%s%.2f%%", if (isPositive) "+" else "", pnlPercent),
                unrealizedPnl = "71.8%",
                isPositive = isPositive,
                focusedMetric = initialFocus
            )

            val cashBalanceValue = lastPrice * 2.99
            val cashBalanceText = if (exchange == "NYSE") String.format("%.2f USD", cashBalanceValue) else String.format("₹%.2f", cashBalanceValue)

            CashBalanceSection(
                usdCash = cashBalanceText,
                totalCash = cashBalanceText
            )

            if (newsList.isNotEmpty()) {
                com.rivavafi.universal.ui.components.SectionHeader(title = "Company News")
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsList.size) { index ->
                        NewsCard(news = newsList[index])
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PremiumButton(
                text = "View Full Stock Price",
                onClick = {
                    val exchangeSuffix = if (exchange.equals("NSE", ignoreCase = true)) "NSE" else "NYSE"
                    val url = "https://www.google.com/search?q=$ticker+stock+price+$exchangeSuffix"
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // Ignored if browser not found
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Filled.ShowChart,
                colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
            )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Metric Card 3
                    Card(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MARKET OPEN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val marketOpenText = if (exchange == "NYSE") String.format("%.2f USD", openPrice) else String.format("₹%.2f", openPrice)
                            Text(marketOpenText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("09:30:01 EST", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Metric Card 4
                    Card(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("52W CHANGE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val simulated52wChange = pnlPercent * 2.5
                            val is52wPositive = simulated52wChange >= 0
                            val simulated52wChangeText = String.format("%s%.2f%%", if(is52wPositive) "+" else "", simulated52wChange)
                            Text(simulated52wChangeText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = if (is52wPositive) com.rivavafi.universal.ui.theme.EmeraldGreen else com.rivavafi.universal.ui.theme.VibrantRed)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Exponential Trend", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Wealth Insights Card / PDF Viewer for IREDA and RTX
            item {
                if (ticker == "IREDA" || ticker == "RTX" || ticker == "INDHOTEL" || ticker == "NVDA") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .glassMorphism(cornerRadius = 24f, alpha = 0.15f)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF161616),
                                        if (ticker == "IREDA") com.rivavafi.universal.ui.theme.TertiaryEmerald.copy(alpha = 0.15f) else com.rivavafi.universal.ui.theme.PrimaryContainerSky.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Investment Thesis", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(if (ticker == "IREDA") "IPO Price" else if (ticker == "INDHOTEL" || ticker == "NVDA") "Buying Price" else "Buy Price", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Text(when(ticker) { "IREDA" -> "₹32"; "INDHOTEL" -> "₹76.49"; "NVDA" -> "$23.36"; else -> "82.22 USD" }, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Status", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Text(if (ticker == "RTX") "StillActive" else "Sold", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Returns", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Text(when(ticker) { "IREDA" -> "715%"; "INDHOTEL" -> "951.05%"; "NVDA" -> "575.4%"; else -> "138.64%" }, color = if (ticker == "IREDA" || ticker == "INDHOTEL") com.rivavafi.universal.ui.theme.TertiaryEmerald else com.rivavafi.universal.ui.theme.PrimaryContainerSky, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    val pdfName = when(ticker) {
                                        "IREDA" -> "portfolio_ireda.pdf"
                                        "INDHOTEL" -> "portfolio_ihcl.pdf"
                                        "NVDA" -> "portfolio_nvidia.pdf"
                                        else -> "portfolio_rtx.pdf"
                                    }
                                    onNavigateToPdfViewer?.invoke(pdfName)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (ticker == "IREDA") com.rivavafi.universal.ui.theme.TertiaryEmerald else com.rivavafi.universal.ui.theme.PrimaryContainerSky)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Read Detailed PDF", fontWeight = FontWeight.Bold, color = if (ticker == "IREDA") com.rivavafi.universal.ui.theme.TertiaryEmerald else com.rivavafi.universal.ui.theme.PrimaryContainerSky)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(com.rivavafi.universal.ui.theme.LightPink.copy(alpha = 0.7f), com.rivavafi.universal.ui.theme.DeepBlueVariant)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Premium Analysis", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Unlock Rivava AI price predictions for 2024", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                            }
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = com.rivavafi.universal.ui.theme.LightPink),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("UPGRADE NOW", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Redirect to External
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            val exchangePrefix = if (exchange == "NSE") "NSE" else "NYSE"
                            val url = "https://www.google.com/finance/quote/$ticker:$exchangePrefix"
                            try { uriHandler.openUri(url) } catch (e: Exception) { }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp)
                    ) {
                        Text("VIEW FULL STOCK PRICE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
}
