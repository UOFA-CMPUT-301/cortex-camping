package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.ui.components.CampsiteListCard
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexHomeTopBar
import com.example.cortex_camping.ui.components.CortexSearchField
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.theme.CortexTheme

/** Figma frame "Browse" */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    campsites: List<Campsite>,
    unreadNotifications: Int,
    onCampsiteClick: (Campsite) -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var query by remember { mutableStateOf("") }
    var minGuests by remember { mutableIntStateOf(0) }
    var filterOpen by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val results = campsites.filter {
        (query.isBlank() || it.name.contains(query, true) || it.location.contains(query, true) || it.tag.any { t -> t.contains(query, true) }) &&
            it.maxOccupancy >= minGuests
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { CortexHomeTopBar("Cortex Camping", onProfileClick, onNotificationsClick, unreadNotifications, scroll) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CortexSearchField(
                        query = query, onQueryChange = { query = it },
                        placeholder = "Search campsites", modifier = Modifier.weight(1f),
                    )
                    Box {
                        CortexButton(
                            if (minGuests == 0) "Filter" else "$minGuests+ guests", { filterOpen = true },
                            style = CortexButtonStyle.Outlined, leadingIcon = Icons.Outlined.FilterList,
                        )
                        DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                            listOf(0, 2, 4, 6).forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(if (n == 0) "Any group size" else "$n+ guests") },
                                    onClick = { minGuests = n; filterOpen = false },
                                )
                            }
                        }
                    }
                }
            }
            item { ResultsHeading("Campsites", "${results.size} result${if (results.size == 1) "" else "s"}") }
            items(results, key = { it.campsiteId }) { site ->
                CampsiteListCard(site, onClick = { onCampsiteClick(site) })
            }
        }
    }
}
