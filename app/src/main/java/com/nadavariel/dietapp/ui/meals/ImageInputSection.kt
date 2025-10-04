// ImageInputSection.kt
package com.nadavariel.dietapp.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImageInputSection(
    onTakePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(title = "Or Take a Photo") {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTakePhotoClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Take Photo")
                Spacer(Modifier.width(8.dp))
                Text("Camera")
            }
            Button(
                onClick = onUploadPhotoClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = "Upload Photo")
                Spacer(Modifier.width(8.dp))
                Text("Upload")
            }
        }
    }
}