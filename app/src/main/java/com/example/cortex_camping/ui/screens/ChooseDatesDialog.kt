package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.ui.components.CalendarLegend
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexContentDialog
import com.example.cortex_camping.ui.components.DateRangeCalendar
import com.example.cortex_camping.ui.components.formatRange
import com.example.cortex_camping.ui.components.formatShortDate
import com.example.cortex_camping.ui.components.nights
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Figma frame "Choose dates dialog" */
@Composable
fun ChooseDatesDialog(
    lottery: Lottery,
    campsite: Campsite,
    taken: Set<LocalDate>,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CortexTheme.colors
    val windowStart = maxOf(lottery.stayFrom, campsite.availability.availableFrom, LocalDate.now())
    val windowEnd = minOf(lottery.stayUntil, campsite.availability.availableUntil)
    val requiredNights = lottery.nights
    var month by remember { mutableStateOf(YearMonth.from(windowStart)) }
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }

    fun clash(a: LocalDate, b: LocalDate) =
        generateSequence(a) { it.plusDays(1) }.takeWhile { it.isBefore(b) }.any { it in taken }

    val valid = start != null && end != null && ChronoUnit.DAYS.between(start, end).toInt() == requiredNights

    CortexContentDialog(
        title = "Choose dates",
        onDismiss = onDismiss,
        actions = {
            CortexButton("Cancel", onDismiss, Modifier.weight(1f), style = CortexButtonStyle.Danger)
            CortexButton("Confirm dates", { onConfirm(start!!, end!!) }, Modifier.weight(1f), enabled = valid)
        },
    ) {
        Text(campsite.name, style = CortexTheme.emphasized.bodyMedium, color = colors.textPrimary)
        Text(campsite.location, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.surfaceBase).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("Lottery window: ${formatRange(windowStart, windowEnd)}", style = CortexTheme.emphasized.bodyMedium, color = colors.textPrimary)
            Text("Window & availability · Select ${nights(requiredNights)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        DateRangeCalendar(
            month = month, windowStart = windowStart, windowEnd = windowEnd, taken = taken,
            start = start, end = end,
            onDayClick = { day ->
                val s = start
                when {
                    s == null || end != null || !day.isAfter(s) -> { start = day; end = null }
                    clash(s, day) -> { start = day; end = null }
                    else -> end = day
                }
            },
            onPrevMonth = if (month.isAfter(YearMonth.from(windowStart))) ({ month = month.minusMonths(1) }) else null,
            onNextMonth = if (month.isBefore(YearMonth.from(windowEnd))) ({ month = month.plusMonths(1) }) else null,
        )
        Spacer(Modifier.height(12.dp))
        CalendarLegend()
        Spacer(Modifier.height(12.dp))
        Text("Taken and outside window dates cannot be selected.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(colors.accentContainer).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Selected stay", style = MaterialTheme.typography.bodySmall, color = colors.onAccentContainer)
            val s = start; val e = end
            Text(
                when {
                    s != null && e != null -> formatRange(s, e)
                    s != null -> "${formatShortDate(s)}, pick check out"
                    else -> "Pick check in"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.onAccentContainer,
            )
            val picked = if (s != null && e != null) ChronoUnit.DAYS.between(s, e).toInt() else requiredNights
            Text(nights(picked), style = MaterialTheme.typography.bodySmall, color = colors.onAccentContainer)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "By confirming dates, I confirm that I have read and agree to the campsite rules and policies.",
            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
        )
    }
}
