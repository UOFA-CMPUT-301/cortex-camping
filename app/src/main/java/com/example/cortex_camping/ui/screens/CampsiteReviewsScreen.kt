package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.CampsiteReview
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.formatRating
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.format.DateTimeFormatter

private val stayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun CampsiteReviewsScreen(
    campsite: Campsite,
    reviews: List<CampsiteReview>,
    onBack: () -> Unit,
) {
    val colors = CortexTheme.colors
    Scaffold(
        topBar = { CortexBackTopBar("Reviews", onBack) },
        containerColor = colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(campsite.name, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(
                        if (campsite.ratingCount == 0) "No ratings yet"
                        else "★ ${formatRating(campsite)} · ${campsite.ratingCount} ratings",
                        style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                    )
                }
            }
            if (reviews.isEmpty()) item {
                Text(
                    "No reviews yet. Campers can review a stay after they check out.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
                )
            }
            items(reviews, key = { it.bookingId }) { r ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${"★".repeat(r.stars)}${"☆".repeat(5 - r.stars)}  ${r.author}",
                        style = MaterialTheme.typography.titleSmall, color = colors.textPrimary,
                    )
                    Text("Stayed until ${r.stayEnd.format(stayFormat)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    if (r.text.isNotBlank()) {
                        Text(r.text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}