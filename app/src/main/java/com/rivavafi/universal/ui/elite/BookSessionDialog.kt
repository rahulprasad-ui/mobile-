package com.rivavafi.universal.ui.elite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun BookSessionDialog(
    onDismiss: () -> Unit,
    onBook: (Int, Long, String) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(30) }
    var selectedTime by remember { mutableStateOf("10:00 AM") }

    val durations = listOf(15, 30, 45, 60)
    val times = listOf("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM")
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Book Private Session", style = MaterialTheme.typography.titleLarge, color = Color.White)

                Text("Select Duration", color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(durations) { duration ->
                        val isSelected = duration == selectedDuration
                        Box(
                            modifier = Modifier
                                .clickable { selectedDuration = duration }
                                .background(if (isSelected) Color(0xFFD4AF37) else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) Color(0xFFD4AF37) else Color.DarkGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("$duration min", color = if (isSelected) Color.Black else Color.White)
                        }
                    }
                }

                Text("Select Time (Tomorrow)", color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(times) { time ->
                        val isSelected = time == selectedTime
                        Box(
                            modifier = Modifier
                                .clickable { selectedTime = time }
                                .background(if (isSelected) Color(0xFFD4AF37) else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) Color(0xFFD4AF37) else Color.DarkGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(time, color = if (isSelected) Color.Black else Color.White)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = { onBook(selectedDuration, cal.timeInMillis, selectedTime) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Confirm Booking", color = Color.Black)
                    }
                }
            }
        }
    }
}
