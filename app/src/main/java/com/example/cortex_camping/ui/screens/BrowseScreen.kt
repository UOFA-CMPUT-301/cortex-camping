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
import com.example.cortex_camping.model.CampsiteFilter
import com.example.cortex_camping.ui.components.CampsiteListCard
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexHomeTopBar
import com.example.cortex_camping.ui.components.CortexSearchField
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.theme.CortexTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.ComposableTarget
import androidx.compose.ui.draw.clip
import com.example.cortex_camping.ui.components.CortexContentDialog
import com.example.cortex_camping.ui.components.DateRangeCalendar
import com.example.cortex_camping.ui.components.formatShortDate
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon


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
    var location by remember { mutableStateOf("") }

    var checkIn by remember { mutableStateOf<LocalDate?>(null) }
    var checkOut by remember { mutableStateOf<LocalDate?>(null) }
    var datesOpen by remember { mutableStateOf(false) }
    val results =
        CampsiteFilter(query, minGuests, location, checkIn, checkOut).filterCampsites(campsites)


    // Location menu, every location has a campsite
    val locations = campsites.map { it.location }.filter { it.isNotBlank() }.distinct()
    val start = checkIn
    val end = checkOut
    val datesLabel = if (start != null && end != null) "${formatShortDate(start)} – ${formatShortDate(end)}" else "Any dates"
    // how many filters are on, shown on the Filter button
    val activeFilters = listOf(minGuests > 0, location.isNotBlank(), start != null && end != null).count { it }


    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            CortexHomeTopBar(
                "Cortex Camping",
                onProfileClick,
                onNotificationsClick,
                unreadNotifications,
                scroll
            )
        },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 24.dp + contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CortexSearchField(
                        query = query, onQueryChange = { query = it },
                        placeholder = "Search campsites", modifier = Modifier.weight(1f),
                    )
                    Box {
                        CortexButton(
                            if (activeFilters == 0) "Filter" else "Filter ($activeFilters)",
                            { filterOpen = true },
                            style = CortexButtonStyle.Outlined,
                            leadingIcon = Icons.Outlined.FilterList,
                        )
                        DropdownMenu(
                            expanded = filterOpen,
                            onDismissRequest = { filterOpen = false }) {
                            // Group size, the picked one has a check mark
                            MenuHeading("Group size")
                            listOf(0, 2, 4, 6).forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(if (n == 0) "Any group size" else "$n+ guests") },
                                    onClick = { minGuests = n; filterOpen = false },
                                    trailingIcon = if (minGuests == n) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                                )
                            }

                            // Location
                            HorizontalDivider()
                            MenuHeading("Location")
                            DropdownMenuItem(
                                text = { Text("Anywhere") },
                                onClick = {
                                    location = ""
                                    filterOpen = false
                                },
                                trailingIcon = if (location.isBlank()) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                            )
                            locations.forEach { place ->
                                DropdownMenuItem(
                                    text = { Text(place) },
                                    onClick = {
                                        location = place
                                        filterOpen = false
                                    },
                                    trailingIcon = if (location == place) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                                )
                            }

                            // Dates, closes the menu and opens the date dialog
                            HorizontalDivider()
                            MenuHeading("Dates")
                            DropdownMenuItem(
                                text = { Text(datesLabel) },
                                onClick = {
                                    filterOpen = false
                                    datesOpen = true
                                },
                                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                            )
                        }
                    }
                }
            }
            item {
                ResultsHeading(
                    "Campsites",
                    "${results.size} result${if (results.size == 1) "" else "s"}"
                )
            }

            items(results, key = { it.campsiteId }) { site ->
                CampsiteListCard(
                    site,
                    onClick = { onCampsiteClick(site) }
                )
            }

        }

        if (datesOpen) {
            PickDates(
                checkIn = checkIn,
                checkOut = checkOut,
                onApply = { inDate, outDate ->
                    checkIn = inDate
                    checkOut = outDate
                    datesOpen = false
                },
                onClear = {
                    checkIn = null
                    checkOut = null
                    datesOpen = false
                },
                onDismiss = {
                    datesOpen = false
                }
            )
        }
    }
}

// small grey title for each part of the Filter menu
@Composable
private fun MenuHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = CortexTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

// Lets the user choose their check-in and check-out dates
@Composable
private fun PickDates(
    checkIn: LocalDate?,
    checkOut: LocalDate?,
    onApply: (LocalDate, LocalDate) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()

    var month by remember {
        mutableStateOf(YearMonth.from(checkIn ?: today))
    }

    var start by remember {
        mutableStateOf(checkIn)
    }

    var end by remember {
        mutableStateOf(checkOut)
    }

    CortexContentDialog(
        title = "Choose Dates",
        onDismiss = onDismiss,
        actions = {
            CortexButton(
                "Clear",
                onClear,
                Modifier.weight(1f),
                style = CortexButtonStyle.Danger
            )

            CortexButton(
                "Apply",
                { onApply(start!!, end!!) },
                Modifier.weight(1f),
                enabled = start != null && end != null
            )
        }
    ) {
        Text(
            "Choose check in and check out day.",
            style = MaterialTheme.typography.bodySmall,
            color = CortexTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        DateRangeCalendar(
            month = month,
            windowStart = today,
            // Allow dates up to one year from today
            windowEnd = today.plusYears(1),
            taken = emptySet(),
            start = start,
            end = end,
            onDayClick = { day ->
                // Pick the check-in date first, then the check-out date
                if (start == null || end != null || !day.isAfter(start)) {
                    start = day
                    end = null
                } else {
                    end = day
                }
            },

            // Only allow going back if it is after the current month
            onPrevMonth = if (month.isAfter(YearMonth.from(today))) {
                {
                    month = month.minusMonths(1)
                }
            } else {
                null
            },

            // Stop at the last month available for selection
            onNextMonth = if (month.isBefore(YearMonth.from(today.plusYears(1)))) {
                {
                    month = month.plusMonths(1)
                }
            } else {
                null
            }
        )
    }
}
