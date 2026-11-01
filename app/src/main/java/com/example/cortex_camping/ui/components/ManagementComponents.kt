package com.example.cortex_camping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun ManagementCard(
    title: String,
    subtitle: String,
    meta: String,
    leading: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 96.dp,
    trailing: (@Composable () -> Unit)? = null,
    footerPadding: Dp = 8.dp,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier) {
        Row(Modifier.fillMaxWidth().height(headerHeight)) {
            leading?.invoke(Modifier.width(80.dp).height(headerHeight).clip(RoundedCornerShape(topStart = 12.dp)))
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    trailing?.invoke()
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        CortexDivider()
        Column(Modifier.padding(horizontal = 12.dp, vertical = footerPadding), content = footer)
    }
}

@Composable
fun InfoActionCard(
    title: String,
    lines: List<String>,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    headerHeight: Dp = 80.dp,
) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier) {
        Column(Modifier.heightIn(min = headerHeight).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            lines.forEachIndexed { i, line ->
                Text(line, style = if (i == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        CortexDivider()
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            CortexButton(actionLabel, onAction, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand)
        }
    }
}

@Composable
fun ActionPair(
    primary: String, onPrimary: () -> Unit,
    danger: String, onDanger: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CortexButton(primary, onPrimary, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
        CortexButton(danger, onDanger, Modifier.weight(1f), style = CortexButtonStyle.Danger)
    }
}

@Composable
fun AvatarSlot(modifier: Modifier) {
    Box(modifier.background(CortexTheme.colors.surfaceDefault), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = CortexPalette.OnSurface, modifier = Modifier.size(40.dp))
    }
}

@Composable
fun FormActionBar(primaryLabel: String, onPrimary: () -> Unit, onCancel: () -> Unit, primaryEnabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CortexTheme.colors.surfaceDefault)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CortexButton("Cancel", onCancel, Modifier.weight(0.35f), style = CortexButtonStyle.Danger)
        CortexButton(primaryLabel, onPrimary, Modifier.weight(0.65f), enabled = primaryEnabled)
    }
}

private val dateTimeFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy · h:mm a")
private val dateOnlyFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

@Composable
fun DateTimeField(label: String, value: LocalDateTime, onChange: (LocalDateTime) -> Unit, modifier: Modifier = Modifier) {
    PickerField(label, value.format(dateTimeFormat), value.toLocalDate(), value.toLocalTime(), { d, t -> onChange(LocalDateTime.of(d, t!!)) }, modifier)
}

@Composable
fun DateField(label: String, value: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    PickerField(label, value.format(dateOnlyFormat), value, null, { d, _ -> onChange(d) }, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    text: String,
    date: LocalDate,
    time: LocalTime?,
    onPicked: (LocalDate, LocalTime?) -> Unit,
    modifier: Modifier,
) {
    var step by remember { mutableIntStateOf(0) } // 0 closed, 1 picking the date, 2 picking the time
    var pickedDate by remember { mutableStateOf(date) }
    val colors = CortexTheme.colors
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick $label", tint = CortexPalette.BrandPrimary) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.extraSmall,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.borderDefault, focusedBorderColor = CortexPalette.BrandPrimary,
                unfocusedLabelColor = colors.textSecondary, unfocusedTextColor = colors.textPrimary, focusedTextColor = colors.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { step = 1 })
    }
    if (step == 1) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                CortexButton(if (time == null) "OK" else "Next", {
                    state.selectedDateMillis?.let { pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    if (time == null) { onPicked(pickedDate, null); step = 0 } else step = 2
                }, style = CortexButtonStyle.Text)
            },
            dismissButton = { CortexButton("Cancel", { step = 0 }, style = CortexButtonStyle.Text) },
        ) { DatePicker(state) }
    }
    if (step == 2 && time != null) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { step = 0 },
            containerColor = colors.surfaceHigh,
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = { CortexButton("OK", { onPicked(pickedDate, LocalTime.of(state.hour, state.minute)); step = 0 }, style = CortexButtonStyle.Text) },
            dismissButton = { CortexButton("Cancel", { step = 0 }, style = CortexButtonStyle.Text) },
        )
    }
}

@Composable
fun MultiSelectDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CortexTheme.colors.surfaceHigh,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { current = if (id in current) current - id else current + id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(id in current, null, colors = CheckboxDefaults.colors(checkedColor = CortexPalette.BrandPrimary), modifier = Modifier.padding(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = CortexTheme.colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = { CortexButton("Done", { onConfirm(current) }, style = CortexButtonStyle.Text) },
        dismissButton = { CortexButton("Cancel", onDismiss, style = CortexButtonStyle.Text) },
    )
}

@Composable
fun InfoDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CortexTheme.colors.surfaceHigh,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = CortexTheme.colors.textPrimary) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content) },
        confirmButton = { CortexButton("Close", onDismiss, style = CortexButtonStyle.Text) },
    )
}

@Composable
fun QrCodeDialog(campsite: Campsite, onDismiss: () -> Unit) {
    val link = "cortexcamping://campsite/${campsite.campsiteId}"
    InfoDialog("${campsite.name} QR code", onDismiss) {
        // TODO: draw a QR code of link here
        Box(Modifier.size(240.dp).background(CortexTheme.colors.surfaceDefault).align(Alignment.CenterHorizontally))
        Text("Guests can scan this to open the campsite details.", style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
        Text(link, style = MaterialTheme.typography.bodySmall, color = CortexPalette.BrandPrimary)
    }
}

@Composable
fun AvailabilityDialog(
    title: String,
    blocked: Set<LocalDate>,
    editable: Boolean,
    onDismiss: () -> Unit,
    onSave: (Set<LocalDate>) -> Unit = {},
) {
    var month by remember { mutableStateOf(YearMonth.from(blocked.filter { !it.isBefore(LocalDate.now()) }.minOrNull() ?: LocalDate.now())) }
    var current by remember { mutableStateOf(blocked) }
    CortexContentDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            if (editable) {
                CortexButton("Cancel", onDismiss, Modifier.weight(1f), style = CortexButtonStyle.Danger)
                CortexButton("Save dates", { onSave(current) }, Modifier.weight(1f))
            } else {
                CortexButton("Close", onDismiss, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
            }
        },
    ) {
        Text(
            if (editable) "Tap a date to block or reopen it." else "Red dates are booked or blocked.",
            style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        DateRangeCalendar(
            month = month, windowStart = month.atDay(1), windowEnd = month.atEndOfMonth(), taken = current,
            start = null, end = null,
            onDayClick = { d -> if (editable) current = if (d in current) current - d else current + d },
            onPrevMonth = { month = month.minusMonths(1) }, onNextMonth = { month = month.plusMonths(1) },
            allowTakenClicks = editable,
        )
    }
}
