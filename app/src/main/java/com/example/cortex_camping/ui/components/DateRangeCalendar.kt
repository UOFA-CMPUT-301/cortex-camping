package com.example.cortex_camping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DateRangeCalendar(
    month: YearMonth,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    taken: Set<LocalDate>,
    start: LocalDate?,
    end: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onPrevMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    allowTakenClicks: Boolean = false,
) {
    val colors = CortexTheme.colors
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onPrevMonth?.invoke() }, enabled = onPrevMonth != null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = { onNextMonth?.invoke() }, enabled = onNextMonth != null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(8.dp))
        val week = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
        Row(Modifier.fillMaxWidth()) {
            week.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f).height(40.dp).padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value % 7
        val gridStart = first.minusDays(lead.toLong())
        val rows = (lead + month.lengthOfMonth() + 6) / 7
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                repeat(7) { c ->
                    val day = gridStart.plusDays((r * 7 + c).toLong())
                    DayCell(
                        day = day,
                        inMonth = day.month == month.month,
                        inWindow = !day.isBefore(windowStart) && !day.isAfter(windowEnd),
                        isTaken = day in taken,
                        isEdge = day == start || day == end,
                        inRange = start != null && end != null && day.isAfter(start) && day.isBefore(end),
                        bandLeft = end != null && start != null && (day == end || (day.isAfter(start) && day.isBefore(end))),
                        bandRight = end != null && start != null && (day == start || (day.isAfter(start) && day.isBefore(end))),
                        onClick = { onDayClick(day) },
                        allowTakenClicks = allowTakenClicks,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    inWindow: Boolean,
    isTaken: Boolean,
    isEdge: Boolean,
    inRange: Boolean,
    bandLeft: Boolean,
    bandRight: Boolean,
    onClick: () -> Unit,
    allowTakenClicks: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors
    val selectable = inMonth && inWindow && (!isTaken || allowTakenClicks)
    val band = colors.selectedContainer
    Box(
        modifier
            .height(40.dp)
            .drawBehind {
                val h = 40.dp.toPx()
                if (bandLeft) drawRect(band, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width / 2, h))
                if (bandRight) drawRect(band, topLeft = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), size = androidx.compose.ui.geometry.Size(size.width / 2, h))
            },
        contentAlignment = Alignment.Center,
    ) {
        val circle = Modifier.size(40.dp).clip(CircleShape)
        val styled = when {
            isEdge -> circle.background(CortexPalette.BrandPrimary)
            inRange -> circle
            selectable && !isTaken -> circle.border(1.dp, colors.borderSubtle, CircleShape)
            else -> circle
        }
        Box(
            styled.clickable(enabled = selectable, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val textColor: Color = when {
                isEdge -> CortexPalette.BrandOnPrimary
                !inMonth || !inWindow -> colors.textSecondary.copy(alpha = 0.5f)
                isTaken -> colors.error
                else -> colors.textPrimary
            }
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isEdge) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
            )
        }
    }
}

@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    val colors = CortexTheme.colors
    @Composable
    fun Item(marker: @Composable () -> Unit, label: String, m: Modifier) {
        Row(m, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { marker() }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Item({ Box(Modifier.size(20.dp).border(1.dp, colors.borderSubtle, CircleShape)) }, "Available", Modifier.weight(1f))
            Item({ Text("6", style = MaterialTheme.typography.bodySmall, color = colors.error, fontWeight = FontWeight.Medium) }, "Taken", Modifier.weight(1f))
        }
        Row {
            Item({
                Box(Modifier.size(20.dp).clip(CircleShape).background(CortexPalette.BrandPrimary), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(CortexPalette.BrandOnPrimary))
                }
            }, "Selected stay", Modifier.weight(1f))
            Item({ Text("1", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary.copy(alpha = 0.5f)) }, "Outside window", Modifier.weight(1f))
        }
    }
}
