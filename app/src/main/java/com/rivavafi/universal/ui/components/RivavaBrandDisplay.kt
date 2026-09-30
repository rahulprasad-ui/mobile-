package com.rivavafi.universal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rivavafi.universal.R

@Composable
fun RivavaBrandDisplay(
    showQuote: Boolean = true,
    logoSize: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.rivava_logo),
            contentDescription = "Rivava Logo",
            modifier = Modifier
                .size(logoSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
        )
        if (showQuote) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Be a master of your money",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
