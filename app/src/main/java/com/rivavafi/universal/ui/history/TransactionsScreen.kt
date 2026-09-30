package com.rivavafi.universal.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.home.TransactionDetailsBottomSheet
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Security
import com.rivavafi.universal.ui.theme.glassMorphism
import com.rivavafi.universal.ui.theme.glowEffect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToDetail: ((Long) -> Unit)? = null,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.systemBarsPadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header with Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )

                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.rivavafi.universal.R.drawable.rivava_logo),
                    contentDescription = "Rivava Logo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Month Selector
            val selectedMonth by viewModel.selectedMonth.collectAsState()
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = monthFormat.format(selectedMonth.time),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search with embedded Sort icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                placeholder = { Text("Search merchants, categories, amounts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date (Newest)") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.DATE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Oldest)") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.DATE_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Amount (High)") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.AMOUNT_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Amount (Low)") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.AMOUNT_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Merchant") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.MERCHANT_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Category") },
                                    onClick = {
                                        viewModel.onSortOrderChanged(SortOrder.CATEGORY_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is TransactionsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TransactionsUiState.Empty -> {
                TransactionsEmptyState()
            }
            is TransactionsUiState.Success -> {
                Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    TransactionList(
                        uiState = state,
                        viewModel = viewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
            is TransactionsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    uiState: TransactionsUiState.Success,
    viewModel: TransactionsViewModel,
    onNavigateToDetail: ((Long) -> Unit)? = null
) {
    val showDetails by viewModel.showSmsDetails.collectAsState()
    var selectedTransactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToView by remember { mutableStateOf<TransactionEntity?>(null) }

    val haptic = LocalHapticFeedback.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 140.dp) // Provide enough bottom padding for the floating nav bar
    ) {
        item {
            // Month Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Monthly Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Credit", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                            Text("₹${String.format(Locale.getDefault(), "%.0f", uiState.monthlyCredit)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Debit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", uiState.monthlyDebit)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val net = uiState.monthlyCredit - uiState.monthlyDebit
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Balance", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${if (net >= 0) "+" else ""}₹${String.format(Locale.getDefault(), "%.0f", net)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (net >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    val debits = uiState.transactions.filter { it.type == "DEBIT" || it.type == "BILL_PENDING" }
                    val topCategory = debits.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: "None"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(topCategory, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${uiState.transactions.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        uiState.groupedByDay.forEach { (date, transactionsForDay) ->
            item {
                val dailyTotal = transactionsForDay.filter { it.type == "DEBIT" || it.type == "EXPENSE" || it.type == "BILL_PENDING" }.sumOf { it.amount }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "-₹${String.format(Locale.getDefault(), "%.0f", dailyTotal)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(transactionsForDay, key = { it.id }) { transaction ->

            // To prevent heavy work during flings on high refresh rates,
            // only trigger side effects after settling, but since this is just UI swipe to edit:
            val swipeableState = rememberSwipeToDismissBoxState(
                confirmValueChange = { state ->
                    if (state == SwipeToDismissBoxValue.StartToEnd) {
                        selectedTransactionToEdit = transaction
                        false // don't dismiss
                    } else {
                        false
                    }
                }
            )

            // Trigger haptic feedback separate from the fling calculation path
            LaunchedEffect(swipeableState.targetValue) {
                if (swipeableState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            SwipeToDismissBox(
                state = swipeableState,
                backgroundContent = {
                    val color = if (swipeableState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (swipeableState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                content = {
                    TransactionItem(
                        transaction = transaction,
                        showDetails = showDetails,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (onNavigateToDetail != null) {
                                onNavigateToDetail(transaction.id)
                            } else {
                                transactionToView = transaction
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            transactionToDelete = transaction
                        }
                    )
                }
            )
        }
        }
    }

    val categories by viewModel.categories.collectAsState()

    selectedTransactionToEdit?.let { transaction ->
        TransactionCategoryBottomSheet(
            transaction = transaction,
            customCategories = categories,
            onDismiss = { selectedTransactionToEdit = null },
            onSave = { updatedTransaction, createRule, ruleKeyword ->
                viewModel.updateTransaction(updatedTransaction, createRule, ruleKeyword)
                selectedTransactionToEdit = null
            },
            onAddCategory = { name, type ->
                viewModel.addCategory(name, type)
            }
        )
    }

    transactionToView?.let { transaction ->
        TransactionDetailsBottomSheet(
            transaction = transaction,
            onDismiss = { transactionToView = null },
            onEdit = {
                transactionToView = null
                selectedTransactionToEdit = transaction
            },
            onDelete = {
                transactionToView = null
                transactionToDelete = transaction
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Visual Anchor: Asymmetric Glass Card with Neon Accents
        Box(
            modifier = Modifier
                .size(256.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background decorative elements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(128.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f), CircleShape)
            )

            // Central Iconography
            Box(
                modifier = Modifier
                    .size(192.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .rotate(3f)
                    .glassMorphism(cornerRadius = 24f, alpha = 0.1f, strokeAlpha = 0.08f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating wallet icon top left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-24).dp, y = (-24).dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .rotate(-12f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
                        Box(modifier = Modifier.size(width = 48.dp, height = 4.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape))
                        Box(modifier = Modifier.size(width = 24.dp, height = 4.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), CircleShape))
                    }
                }

                // "No Data" floating badge bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 24.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                        .rotate(6f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO DATA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Typography Cluster
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "No Transactions Found",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "You haven't logged any transactions yet. Your spending story starts with your first swipe. Use the '+' button to begin tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        Spacer(modifier = Modifier.weight(1f))

    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(transaction: TransactionEntity, showDetails: Boolean = true, onClick: () -> Unit, onLongClick: () -> Unit) {
    val isCredit = transaction.type == "CREDIT" || transaction.type == "INCOME" || transaction.type == "REWARD" || transaction.type == "INCOME" || transaction.type == "REWARD"

    val catVisual = if (transaction.subcategory != null)
        com.rivavafi.universal.ui.theme.CategoryVisuals.getSubcategoryVisual(transaction.subcategory)
    else
        com.rivavafi.universal.ui.theme.CategoryVisuals.getCategoryVisual(transaction.category)

    val iconColor = catVisual.color

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catVisual.icon,
                    contentDescription = null,
                    tint = iconColor,
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateString = if (showDetails) formatter.format(Date(transaction.date)) else "****"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (showDetails) catVisual.title else "Hidden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iconColor,
                        modifier = Modifier
                            .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}₹${String.format(Locale.getDefault(), "%.0f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFE53935)
            )
        }
    }
}
