package com.spiritualphone.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Map launcher for the profile screen. A dark glass disc so it stands out
 * against the bright map; shows the avatar when set, an empty person
 * silhouette otherwise.
 */
@Composable
fun ProfileButton(
    avatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatar = remember(avatarPath) {
        avatarPath?.let {
            runCatching { BitmapFactory.decodeFile(it) }.getOrNull()?.asImageBitmap()
        }
    }
    Box(
        modifier
            .size(54.dp)
            .glass(shape = CircleShape, tint = Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar != null) {
            Image(
                bitmap = avatar,
                contentDescription = "Профиль",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp).clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Профиль",
                tint = Color(0xFFEDEDED),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
