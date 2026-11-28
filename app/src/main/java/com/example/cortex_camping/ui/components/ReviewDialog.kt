package com.example.cortex_camping.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.cortex_camping.model.CampsiteRating

@Composable
fun ReviewDialog(
    campsiteName: String,
    initialStars: Int,
    initialText: String,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var stars by rememberSaveable { mutableIntStateOf(initialStars) }
    var text by rememberSaveable { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review $campsiteName") },
        text = {
            Column {
                Row {
                    (1..5).forEach { n ->
                        TextButton(onClick = { stars = n }) { Text(if (n <= stars) "★" else "☆") }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= CampsiteRating.MAX_REVIEW_LENGTH) text = it },
                    label = { Text("Your review (optional)") },
                    supportingText = { Text("${text.length}/${CampsiteRating.MAX_REVIEW_LENGTH}") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(stars, text) }, enabled = stars in 1..5) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}