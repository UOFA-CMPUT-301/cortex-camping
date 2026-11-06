package com.example.cortex_camping.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.ui.components.ActionPair
import com.example.cortex_camping.ui.components.AvatarSlot
import com.example.cortex_camping.ui.components.BackAction
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexConfirmDialog
import com.example.cortex_camping.ui.components.CortexMediumTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.CortexTextField
import com.example.cortex_camping.ui.components.InfoActionCard
import com.example.cortex_camping.ui.components.InfoDialog
import com.example.cortex_camping.ui.components.CampsitePhoto
import com.example.cortex_camping.ui.components.ManagementCard
import com.example.cortex_camping.ui.components.SectionTitle
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

data class ManagedUser(
    val id: String,
    val name: String,
    val email: String,
    val summary: String,
    val isOwner: Boolean,
) {
    // the same device id can be both an entrant and an owner
    val key: String get() = (if (isOwner) "owner-" else "entrant-") + id
}

enum class LogType(val label: String) { Notification("Notification"), Message("Message") }

data class CommunicationLog(
    val id: String,
    val type: LogType,
    val subject: String,
    val route: String,
    val meta: String,
    val message: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScaffold(
    title: String,
    onBack: (() -> Unit)?,
    bottomBar: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { CortexMediumTopBar(title, navigationIcon = { if (onBack != null) BackAction(onBack) }, scrollBehavior = scroll) },
        bottomBar = bottomBar,
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun LazyListScope.searchItem(label: String, placeholder: String, query: String, onQuery: (String) -> Unit) {
    item { CortexTextField(query, onQuery, label, placeholder = placeholder, modifier = Modifier.padding(bottom = 4.dp)) }
}

/** Figma frame "Dashboard" in Admin */
@Composable
fun AdminDashboardScreen(
    onCampsites: () -> Unit,
    onUsers: () -> Unit,
    onImages: () -> Unit,
    onLogs: () -> Unit,
    onSpoofUser: () -> Unit,
    onSpoofOwner: () -> Unit,
) {
    AdminScaffold(
        "Admin Dashboard", onBack = null,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CortexButton("Spoof user", onSpoofUser, Modifier.weight(1f))
                CortexButton("Spoof owner", onSpoofOwner, Modifier.weight(1f))
            }
        },
    ) {
        item { SectionTitle("Manage your platform") }
        item { InfoActionCard("Manage Campsites", listOf("Review campsite listings and details."), "Open campsites", onCampsites) }
        item { InfoActionCard("Manage Users", listOf("Manage owners and user profiles."), "Open users", onUsers) }
        item { InfoActionCard("Manage Images", listOf("Review images uploaded to campsites."), "Open images", onImages) }
        item { InfoActionCard("Communications Log", listOf("View all sent notifications and messages."), "View logs", onLogs) }
    }
}

/** Figma frame "Manage Campsites" in Admin */
@Composable
fun AdminCampsitesScreen(
    campsites: List<Campsite>,
    ownerName: (String) -> String,
    onBack: () -> Unit,
    onView: (Campsite) -> Unit,
    onDelete: (Campsite) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = campsites.filter { c -> listOf(c.name, c.location, ownerName(c.ownerId)).any { it.contains(query, true) } }
    AdminScaffold("Manage Campsites", onBack) {
        searchItem("Search campsites", "Search name, location or owner", query) { query = it }
        item { SectionTitle("All campsite listings") }
        items(visible, key = { it.campsiteId }) { c ->
            ManagementCard(
                c.name, c.location, "Owner: ${ownerName(c.ownerId)} · Max ${c.maxOccupancy} guests",
                leading = { m -> CampsitePhoto(c, m) },
            ) { ActionPair("View details", { onView(c) }, "Delete", { pending = c.campsiteId }) }
        }
    }
    pending?.let { id ->
        val c = campsites.firstOrNull { it.campsiteId == id }
        if (c == null) { pending = null; return@let }
        CortexConfirmDialog("Delete campsite?", "${c.name} and its listing will be removed for all users.", "Delete", destructive = true,
            onConfirm = { onDelete(c); pending = null }, onDismiss = { pending = null })
    }
}

/** Figma frame "Manage Users" in Admin */
@Composable
fun AdminUsersScreen(
    users: List<ManagedUser>,
    onBack: () -> Unit,
    onDelete: (ManagedUser) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = users.filter { it.name.contains(query, true) || it.email.contains(query, true) }
    AdminScaffold("Manage Users", onBack) {
        searchItem("Search users", "Search name or email", query) { query = it }
        item { SectionTitle("Owners & profiles · ${visible.size} users") }
        items(visible, key = { it.key }) { u ->
            ManagementCard(u.name, u.email, u.summary, leading = { m -> AvatarSlot(m) }) {
                ActionPair("View profile", { viewing = u.key }, "Delete user", { pending = u.key })
            }
        }
    }
    viewing?.let { key ->
        val u = users.firstOrNull { it.key == key }
        if (u == null) { viewing = null; return@let }
        InfoDialog(u.name, { viewing = null }) {
            Text(u.email.ifBlank { "No email on file" }, style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textPrimary)
            Text(u.summary, style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
        }
    }
    pending?.let { key ->
        val u = users.firstOrNull { it.key == key }
        if (u == null) { pending = null; return@let }
        CortexConfirmDialog("Delete user?", "${u.name}'s ${if (u.isOwner) "owner" else "camper"} profile will be removed. Existing campsites and messages are kept.", "Delete user", destructive = true,
            onConfirm = { onDelete(u); pending = null }, onDismiss = { pending = null })
    }
}

/** Figma frame "Manage Images" in Admin */
@Composable
fun AdminImagesScreen(
    campsites: List<Campsite>,
    ownerName: (String) -> String,
    onBack: () -> Unit,
    onDelete: (Campsite) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = campsites.filter { it.name.contains(query, true) || ownerName(it.ownerId).contains(query, true) }
    AdminScaffold("Manage Images", onBack) {
        searchItem("Search images", "Search campsite or uploader", query) { query = it }
        item { SectionTitle("Uploaded images · ${visible.size} images") }
        items(visible, key = { it.campsiteId }) { c ->
            ManagementCard(c.name, "By ${ownerName(c.ownerId)}", "Cover photo", leading = { m -> CampsitePhoto(c, m) }) {
                ActionPair("View image", { viewing = c.campsiteId }, "Delete image", { pending = c.campsiteId })
            }
        }
    }
    viewing?.let { id ->
        val c = campsites.firstOrNull { it.campsiteId == id }
        if (c == null) { viewing = null; return@let }
        InfoDialog(c.name, { viewing = null }) {
            CampsitePhoto(c, Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium))
            Text("Uploaded by ${ownerName(c.ownerId)} · Cover photo", style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
        }
    }
    pending?.let { id ->
        val c = campsites.firstOrNull { it.campsiteId == id }
        if (c == null) { pending = null; return@let }
        CortexConfirmDialog("Delete image?", "The cover photo for ${c.name} will be removed.",
            "Delete image", destructive = true, onConfirm = { onDelete(c); pending = null }, onDismiss = { pending = null })
    }
}

/** Figma frame "Communications Log" in Admin */
@Composable
fun AdminLogsScreen(logs: List<CommunicationLog>, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = logs.filter {
        (filter == 0 || it.type == if (filter == 1) LogType.Notification else LogType.Message) &&
            listOf(it.subject, it.route, it.meta, it.message).any { f -> f.contains(query.trim(), true) }
    }
    AdminScaffold("Communications Log", onBack) {
        searchItem("Search logs", "Search sender, recipient or message", query) { query = it }
        item {
            CortexSegmentedToggle(
                listOf("All", "Notifications", "Messages"), filter, { filter = it },
                selectedContainerColor = CortexPalette.BrandPrimary, selectedContentColor = CortexPalette.BrandOnPrimary,
            )
        }
        item { SectionTitle("Sent activity · " + listOf("All users", "Notifications", "Messages")[filter]) }
        items(visible, key = { it.id }) { log ->
            InfoActionCard(
                title = log.message,
                lines = emptyList(),
                body = "",
                actionLabel = "View details",
                onAction = { viewing = log.id },
                headerHeight = 10.dp,
            )
        }
    }
    viewing?.let { id ->
        val log = logs.firstOrNull { it.id == id }
        if (log == null) { viewing = null; return@let }
        InfoDialog("${log.type.label} · ${log.subject}", { viewing = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(log.route, style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textPrimary)
                Text(log.meta, style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
                Text(log.message, style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
            }
        }
    }
}
