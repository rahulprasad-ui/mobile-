package com.rivavafi.universal.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.ui.theme.CategoryVisuals
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.theme.glowEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoryBottomSheet(
    transaction: TransactionEntity,
    customCategories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (TransactionEntity, Boolean, String?) -> Unit,
    onAddCategory: (String, String) -> Unit = { _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultMainCategories = listOf("DEBIT", "CREDIT", "EXPENSE", "INCOME", "BILL_PENDING", "INVESTMENT", "SUBSCRIPTION", "IGNORE")
    val mainCategories = (defaultMainCategories + customCategories.filter { it.type == "MAIN" }.map { it.name }).distinct()

    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedSubcategory by remember { mutableStateOf(transaction.subcategory ?: "") }
    val initialType = transaction.type
    var isCreditTransaction by remember { mutableStateOf(initialType == "CREDIT" || initialType == "INCOME" || initialType == "REWARD") }
    var createRule by remember { mutableStateOf(true) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var isSubCat by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSubCat,
                            onClick = { isSubCat = true }
                        )
                        Text("Subcategory (Debit)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isSubCat,
                            onClick = { isSubCat = false }
                        )
                        Text("Main Category")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) {
                        onAddCategory(newCatName, if (isSubCat) "SUB" else "MAIN")
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // Slight glassmorphism
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edit Transaction",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isCreditTransaction) Color.Transparent else MaterialTheme.colorScheme.error)
                        .clickable { isCreditTransaction = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Debit", color = if (!isCreditTransaction) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isCreditTransaction) Color(0xFF4CAF50) else Color.Transparent)
                        .clickable { isCreditTransaction = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Credit", color = if (isCreditTransaction) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
            ) {
                items(mainCategories) { category ->
                    val catVisual = CategoryVisuals.getCategoryVisual(category)
                    val isSelected = selectedCategory == category

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .glowEffect(color = catVisual.color, radius = 30f, isSelected = isSelected)
                            .background(if (isSelected) catVisual.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) catVisual.color else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = category
                                selectedSubcategory = ""
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = catVisual.icon,
                                contentDescription = null,
                                tint = if (isSelected) catVisual.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = catVisual.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) catVisual.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { createRule = !createRule },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = createRule,
                    onCheckedChange = { createRule = it }
                )
                Text(
                    text = "Remember this for future transactions from ${transaction.merchantName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    val type = if (isCreditTransaction) {
                        when (selectedCategory) {
                            "REWARD" -> "REWARD"
                            "SELF_TRANSFER" -> "SELF_TRANSFER"
                            "IGNORE" -> "IGNORE"
                            else -> "CREDIT"
                        }
                    } else {
                        when (selectedCategory) {
                            "BILL_PENDING" -> "BILL_PENDING"
                            "MANDATE_CREATED" -> "MANDATE_CREATED"
                            "SELF_TRANSFER" -> "SELF_TRANSFER"
                            "IGNORE" -> "IGNORE"
                            else -> "DEBIT"
                        }
                    }
                    val updated = transaction.copy(
                        category = selectedCategory,
                        subcategory = if (isCreditTransaction && selectedSubcategory.isNotEmpty()) selectedSubcategory else null,
                        type = type
                    )
                    onSave(updated, createRule, transaction.merchantName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Changes", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }
}
