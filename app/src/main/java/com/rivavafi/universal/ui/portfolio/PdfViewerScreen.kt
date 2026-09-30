package com.rivavafi.universal.ui.portfolio

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.view.WindowManager
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    assetName: String = "portfolio_ireda.pdf",
    onBack: () -> Unit
) {
    val context = LocalContext.current


    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(assetName) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, assetName)
                if (!file.exists()) {
                    context.assets.open(assetName).use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor)
                pageCount = pdfRenderer?.pageCount ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment Thesis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pageCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load PDF.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            // Support zooming
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            var selectedTab by remember { mutableStateOf(0) }

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Summary") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Full PDF Report") }
                    )
                }

                if (selectedTab == 0) {
                    // Summary Page
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        item {
                            Text(
                                text = if (assetName.contains("ireda", ignoreCase = true)) "IREDA Investment Thesis" else "RTX Investment Thesis",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val summaryText = if (assetName.contains("ireda", ignoreCase = true)) {
                                """
                                **Executive Summary:**
                                Indian Renewable Energy Development Agency (IREDA) represents a strong buy opportunity due to the government's aggressive push towards 500GW of non-fossil fuel capacity by 2030.

                                **Key Catalysts:**
                                • Sovereign backing and Navratna status provide access to low-cost international and domestic capital.
                                • Expanding loan book with consistently declining gross NPAs (under 3.2%).
                                • Favorable shift towards emerging sectors like Green Hydrogen and Pumped Hydro.

                                **Valuation:**
                                Trading at a premium to historical book value, but justified by superior ROE metrics compared to peer NBFCs. Target price set at ₹228 over a 12-month horizon.
                                """.trimIndent()
                            } else {
                                """
                                **Executive Summary:**
                                Raytheon Technologies (RTX) is positioned for sustained multi-year growth driven by global defense modernization cycles and the recovery of commercial aerospace aftermarket services.

                                **Key Catalysts:**
                                • Historic backlog of $196 Billion across Pratt & Whitney and Collins Aerospace.
                                • Resolution of the GTF engine powdered metal issue has cleared the overhang, allowing focus on free cash flow generation.
                                • Re-arming of European and Asian allies driving sustained missile defense orders (Patriot, AMRAAM).

                                **Valuation:**
                                Current Free Cash Flow yield presents an attractive entry point. Target price $120.50 based on sum-of-the-parts analysis and projected margin expansion in 2025.
                                """.trimIndent()
                            }

                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // PDF Viewer
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(pageCount) { index ->
                            PdfPageImage(pdfRenderer = pdfRenderer, pageIndex = index)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageImage(pdfRenderer: PdfRenderer?, pageIndex: Int) {
    if (pdfRenderer == null) return

    val density = LocalDensity.current.density
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            val page = pdfRenderer.openPage(pageIndex)
            val newBitmap = Bitmap.createBitmap(
                (page.width * density).toInt(),
                (page.height * density).toInt(),
                Bitmap.Config.ARGB_8888
            )
            // Fill background with white
            newBitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap = newBitmap
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
