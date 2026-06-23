package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.model.Hollow
import kotlin.math.roundToInt

/**
 * Bottom sheet that slides up when a Hollow dot is tapped, showing its details.
 * Race / count / spiritual power are placeholders until real Hollow data exists;
 * distance is computed live from the user's position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HollowDetailsSheet(
    hollow: Hollow,
    distanceM: Double?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF11151F)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Обнаруженный объект",
                color = Color(0xFFE53935),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Row("Раса", hollow.info.race)
            Row("Численность", hollow.info.count.toString())
            Row("Духовная сила", hollow.info.spiritualPower)
            Row("Местность", hollow.terrainType.ruName)
            Row("Расстояние", distanceM?.let { formatDistance(it) } ?: "—")
        }
    }
}

@Composable
private fun Row(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color(0xFFEDEDED),
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 12.dp),
    )
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f км".format(meters / 1000.0)
    else "${meters.roundToInt()} м"
