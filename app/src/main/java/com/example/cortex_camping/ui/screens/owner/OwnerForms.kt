package com.example.cortex_camping.ui.screens.owner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.cortex_camping.model.AvailabilitySchedule
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.NO_WAITLIST_LIMIT
import com.example.cortex_camping.ui.components.AvailabilityDialog
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexMediumTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.CortexTextField
import com.example.cortex_camping.ui.components.DateTimeField
import com.example.cortex_camping.ui.components.FormActionBar
import com.example.cortex_camping.ui.components.CampsitePhoto
import com.example.cortex_camping.ui.components.DateField
import com.example.cortex_camping.ui.components.MultiSelectDialog
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FormScaffold(
    title: String,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = { CortexMediumTopBar(title) },
        bottomBar = { FormActionBar(primaryLabel, onPrimary, onCancel, primaryEnabled) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).imePadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Figma frames "Edit Campsite" and "Add New Campsite" */
@Composable
fun CampsiteFormScreen(
    existing: Campsite?,
    onSave: (Campsite) -> Unit,
    onCancel: () -> Unit,
    saving: Boolean = false,
) {
    val isNew = existing == null
    val context = LocalContext.current
    val today = LocalDate.now()
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var capacity by rememberSaveable { mutableStateOf(existing?.maxOccupancy?.toString() ?: "6") }
    var price by rememberSaveable { mutableStateOf(existing?.price?.let { "%.2f".format(it) } ?: "") }
    var tags by rememberSaveable { mutableStateOf(existing?.tag?.joinToString(", ") ?: "") }
    var rulesUrl by rememberSaveable { mutableStateOf(existing?.rulesUrl ?: "") }
    // only the picked Uri is saveable: stored photos are too big for saved instance state
    var pickedImage by rememberSaveable { mutableStateOf<String?>(null) }
    val imageUrl = pickedImage ?: existing?.imageUrl
    var availableFrom by remember { mutableStateOf(existing?.availability?.availableFrom ?: today) }
    var availableUntil by remember { mutableStateOf(existing?.availability?.availableUntil ?: today.plusYears(1)) }
    var blocked by remember { mutableStateOf(existing?.availability?.unavailableDates ?: emptySet()) }
    var editDates by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // keep read access to the photo after the app restarts
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pickedImage = uri.toString()
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    val priceValue = price.toDoubleOrNull()
    val datesValid = !availableUntil.isBefore(availableFrom)

    FormScaffold(
        title = if (isNew) "Add New Campsite" else "Edit Campsite",
        primaryLabel = if (saving) "Saving..." else if (isNew) "Add campsite" else "Save changes",
        primaryEnabled = name.isNotBlank() && priceValue != null && datesValid && !saving,
        onPrimary = {
            val base = existing ?: Campsite(price = 0.0, availability = AvailabilitySchedule(availableFrom, availableUntil))
            onSave(
                base.copy(
                    name = name.trim(),
                    description = description.trim(),
                    location = location.trim(),
                    maxOccupancy = capacity.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    price = priceValue ?: 0.0,
                    tag = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    rulesUrl = rulesUrl.trim(),
                    imageUrl = imageUrl,
                    availability = AvailabilitySchedule(availableFrom, availableUntil, blocked),
                ),
            )
        },
        onCancel = onCancel,
    ) {
        if (imageUrl.isNullOrBlank()) {
            CortexButton("Upload campsite image", ::pick, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CampsitePhoto(imageUrl, Modifier.size(width = 136.dp, height = 100.dp).clip(MaterialTheme.shapes.medium))
                CortexButton("Change image", ::pick, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
            }
        }
        CortexTextField(name, { name = it }, "Campsite name", placeholder = "Enter campsite name")
        CortexTextField(description, { description = it }, "Description", singleLine = false,
            placeholder = "Describe the campsite, facilities,\nand what guests can expect.", modifier = Modifier.height(88.dp))
        CortexTextField(location, { location = it }, "Location (address)", placeholder = "Enter location")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CortexTextField(capacity, { capacity = it.filter(Char::isDigit) }, "Maximum guests", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            CortexTextField(price, { price = it }, "Price per night (CAD)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal, placeholder = "0.00")
        }
        CortexTextField(tags, { tags = it }, "Tags", placeholder = "Separate tags with commas")
        CortexTextField(rulesUrl, { rulesUrl = it }, "Rules & guidelines URL", keyboardType = KeyboardType.Uri, placeholder = "https://")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField("Available from", availableFrom, { availableFrom = it }, Modifier.weight(1f))
            DateField("Available until", availableUntil, { availableUntil = it }, Modifier.weight(1f))
        }
        if (!datesValid) {
            Text("The end date must be on or after the start date.", style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.error)
        }
        CortexButton("Block dates", { editDates = true }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand)
    }
    if (editDates) {
        AvailabilityDialog("Blocked dates", blocked, editable = true, onDismiss = { editDates = false }, onSave = { blocked = it; editDates = false })
    }
}

/** Figma frames "Edit Lottery" and "Create New Lottery" */
@Composable
fun LotteryFormScreen(
    existing: Lottery?,
    campsites: List<Campsite>,
    onSave: (Lottery) -> Unit,
    onCancel: () -> Unit,
    saving: Boolean = false,
) {
    val isNew = existing == null
    val defaults = existing ?: Lottery()
    val options = campsites.map { it.campsiteId to it.name }
    var name by rememberSaveable { mutableStateOf(defaults.name) }
    var selected by remember { mutableStateOf(defaults.campsiteIds.toSet()) }
    var capacity by rememberSaveable { mutableStateOf(defaults.capacity.toString()) }
    var winners by rememberSaveable { mutableStateOf(defaults.winners.toString()) }
    var waitlistOn by rememberSaveable { mutableStateOf(defaults.hasWaitlist) }
    var waitlistCap by rememberSaveable { mutableStateOf(if (defaults.waitlistCapacity > 0) defaults.waitlistCapacity.toString() else "") }
    var opens by remember { mutableStateOf(defaults.opensAt) }
    var closes by remember { mutableStateOf(defaults.closesAt) }
    var draw by remember { mutableStateOf(defaults.drawAt) }
    var stayFrom by remember { mutableStateOf(defaults.stayFrom) }
    var stayUntil by remember { mutableStateOf(defaults.stayUntil) }
    var nights by rememberSaveable { mutableStateOf(defaults.nights.toString()) }
    var pickCampsites by remember { mutableStateOf(false) }
    val selectedNames = options.filter { it.first in selected }.joinToString(", ") { it.second }
    val lottery = defaults.copy(
        name = name.trim(), campsiteIds = options.map { it.first }.filter { it in selected },
        capacity = capacity.toIntOrNull() ?: 0, winners = winners.toIntOrNull() ?: 0,
        // the capacity is optional, leaving it blank means the waitlist has no limit
        waitlistCapacity = if (waitlistOn) waitlistCap.toIntOrNull()?.takeIf { it > 0 } ?: NO_WAITLIST_LIMIT else 0,
        opensAt = opens, closesAt = closes, drawAt = draw,
        stayFrom = stayFrom, stayUntil = stayUntil, nights = nights.toIntOrNull() ?: 0,
    )
    val error = lottery.settingsError

    FormScaffold(
        title = if (isNew) "Create New Lottery" else "Edit Lottery",
        primaryLabel = if (saving) "Saving..." else if (isNew) "Create lottery" else "Save changes",
        primaryEnabled = name.isNotBlank() && selected.isNotEmpty() && error == null && !saving,
        onPrimary = { onSave(lottery) },
        onCancel = onCancel,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CortexTextField(name, { name = it }, "Lottery name", placeholder = "Enter lottery name")
            Box {
                CortexTextField(selectedNames, {}, "Campsites (select multiple)", placeholder = "Choose one or more campsites")
                Box(Modifier.matchParentSize().clickableNoRipple { pickCampsites = true })
            }
            CortexButton("Add / remove campsites", { pickCampsites = true }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand, touchTarget = 40.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CortexTextField(capacity, { capacity = it.filter(Char::isDigit) }, "Lottery capacity", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                CortexTextField(winners, { winners = it.filter(Char::isDigit) }, "Number of winners", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            CortexSegmentedToggle(
                listOf("Waitlist off", "Waitlist on"), if (waitlistOn) 1 else 0, { waitlistOn = it == 1 },
                selectedContainerColor = CortexTheme.colors.surfaceDefault, selectedContentColor = CortexTheme.colors.textPrimary,
            )
            OutlinedTextField(
                value = if (waitlistOn) waitlistCap else "", onValueChange = { waitlistCap = it.filter(Char::isDigit) },
                label = { Text("Waitlist capacity (optional)") }, placeholder = { Text(if (waitlistOn) "No limit" else "Disabled") },
                enabled = waitlistOn, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.extraSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = CortexTheme.colors.borderDefault, focusedBorderColor = CortexPalette.BrandPrimary,
                    disabledBorderColor = CortexTheme.colors.borderSubtle,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            DateTimeField("Entry opens", opens, { opens = it })
            DateTimeField("Entry closes", closes, { closes = it })
            DateTimeField("Draw date & time", draw, { draw = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Stays from", stayFrom, { stayFrom = it }, Modifier.weight(1f))
                DateField("Stays until", stayUntil, { stayUntil = it }, Modifier.weight(1f))
            }
            CortexTextField(nights, { nights = it.filter(Char::isDigit) }, "Nights per stay", keyboardType = KeyboardType.Number)
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.error) }
        }
    }
    if (pickCampsites) {
        MultiSelectDialog("Campsites", options, selected, onConfirm = { selected = it; pickCampsites = false }, onDismiss = { pickCampsites = false })
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.clickable(
    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    indication = null, onClick = onClick,
)
