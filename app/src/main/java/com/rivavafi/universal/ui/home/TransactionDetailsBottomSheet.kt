package com.rivavafi.universal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rivavafi.universal.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isCredit = transaction.type == "CREDIT"
    val color = if (isCredit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    val sign = if (isCredit) "+" else "-"

    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(transaction.date))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Main Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchantName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val categoryVisual = com.rivavafi.universal.ui.theme.CategoryVisuals.getCategoryVisual(transaction.category)
                    val subCategoryVisual = transaction.subcategory?.let { com.rivavafi.universal.ui.theme.CategoryVisuals.getSubcategoryVisual(it) }
                    val visualToUse = subCategoryVisual ?: categoryVisual

                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            text = visualToUse.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = visualToUse.color,
                            modifier = Modifier
                                .background(visualToUse.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "$sign₹${String.format(Locale.getDefault(), "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details List
            DetailRow("Date & Time", dateString)
            if (transaction.bankName != null) {
                DetailRow("Bank", transaction.bankName)
            }
            DetailRow("Type", if (isCredit) "Credit" else "Debit")

            if (transaction.availableBalance != null) {
                DetailRow("Remaining Balance", "₹${String.format(Locale.getDefault(), "%.2f", transaction.availableBalance)}")
            }

            // Raw SMS Message block
            if (!transaction.rawMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Original Message",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = transaction.rawMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
