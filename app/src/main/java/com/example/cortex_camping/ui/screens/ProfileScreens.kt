package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Entrant
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonSize
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexConfirmDialog
import com.example.cortex_camping.ui.components.CortexDivider
import com.example.cortex_camping.ui.components.CortexOutlinedCard
import com.example.cortex_camping.ui.components.GroupedSurface
import com.example.cortex_camping.ui.components.LinkListItem
import com.example.cortex_camping.ui.components.ProfileAvatar
import com.example.cortex_camping.ui.components.ProfileFormCard
import com.example.cortex_camping.ui.components.ProfileFormState
import com.example.cortex_camping.ui.components.SectionTitle
import com.example.cortex_camping.ui.components.SwitchRow
import com.example.cortex_camping.ui.theme.CortexTheme

/** Figma frames "Profile & Settings" and "Profile & Settings · Delete confirmation" */
@Composable
fun ProfileScreen(
    entrant: Entrant,
    onPrefsChange: (Entrant) -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onPolicy: () -> Unit,
    onHelp: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    startWithDeleteDialog: Boolean = false,
) {
    val colors = CortexTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(startWithDeleteDialog) }
    Scaffold(
        topBar = { CortexBackTopBar("Profile & Settings", onBack) },
        containerColor = colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Profile information")
                CortexOutlinedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileAvatar(entrant.initials)
                            Column(Modifier.weight(1f)) {
                                Text("Full name", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text(entrant.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            }
                            CortexButton("Edit Profile", onEditProfile, style = CortexButtonStyle.Outlined, size = CortexButtonSize.XSmall, leadingIcon = Icons.Filled.Edit)
                        }
                        CortexDivider()
                        Row {
                            LabeledValue("Email address", entrant.email, Modifier.weight(1f))
                            LabeledValue("Phone number", entrant.phoneNumber.ifBlank { "Not added" }, Modifier.weight(0.6f))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Notification settings")
                GroupedSurface {
                    val all = entrant.notificationsEnabled
                    SwitchRow("Enable notifications", "Manage all app notifications", all, { onPrefsChange(entrant.copy(notificationsEnabled = it)) })
                    CortexDivider()
                    SwitchRow(
                        "Lottery notifications", "Draw results and entry updates · ${if (entrant.lotteryNotifications) "On" else "Off"}",
                        all && entrant.lotteryNotifications, { onPrefsChange(entrant.copy(lotteryNotifications = it)) }, enabled = all,
                    )
                    SwitchRow(
                        "Reservation notifications", "Booking updates and reminders · ${if (entrant.reservationNotifications) "On" else "Off"}",
                        all && entrant.reservationNotifications, { onPrefsChange(entrant.copy(reservationNotifications = it)) }, enabled = all,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Account management")
                GroupedSurface {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Deleting your profile removes your personal profile information. This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                        )
                        CortexButton("Delete Profile", { confirmDelete = true }, style = CortexButtonStyle.Danger, size = CortexButtonSize.XSmall)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("App policy & guidelines")
                GroupedSurface {
                    LinkListItem("Policy & Guidelines", "Privacy, app policies and community guidelines", onPolicy)
                    CortexDivider()
                    LinkListItem("Help Docs", "Answers and guidance for your next stay", onHelp)
                }
            }
        }
    }
    if (confirmDelete) {
        CortexConfirmDialog(
            title = "Delete your profile?",
            text = "Your full name, email address, phone number and profile picture will be removed from your Cortex Camping profile. This cannot be undone.\n\nAre you sure you want to delete your profile?",
            confirmLabel = "Delete Profile",
            destructive = true,
            onConfirm = { confirmDelete = false; onDeleteProfile() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textPrimary)
    }
}

/** Figma frame "Edit Profile" */
@Composable
fun EditProfileScreen(
    entrant: Entrant,
    onSave: (name: String, email: String, phone: String) -> Unit,
    onCancel: () -> Unit,
    onEditAvatar: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var form by rememberSaveable(stateSaver = ProfileFormSaver) { mutableStateOf(ProfileFormState(entrant.name, entrant.email, entrant.phoneNumber)) }
    Scaffold(
        topBar = { CortexBackTopBar("Edit Profile", onCancel) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).imePadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormIntro()
            ProfileFormCard(
                state = form, onChange = { form = it },
                initials = Entrant(name = form.fullName).initials,
                avatarHint = "Using your initials · Profile picture is optional",
                onEditAvatar = onEditAvatar,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CortexButton(
                        "Save Changes", { onSave(form.fullName.trim(), form.email.trim(), form.phone.trim()) },
                        Modifier.weight(1f), enabled = form.isComplete,
                    )
                    CortexButton("Cancel Changes", onCancel, Modifier.weight(1f), style = CortexButtonStyle.Danger)
                }
            }
            Text(
                "Changes are only applied when you select Save Changes. Cancel Changes keeps your current profile information.",
                style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary,
            )
        }
    }
}

/** Figma frame "Register" */
@Composable
fun RegisterScreen(
    onRegister: (name: String, email: String, phone: String) -> Unit,
    onPolicy: () -> Unit,
    onEditAvatar: () -> Unit,
) {
    var form by rememberSaveable(stateSaver = ProfileFormSaver) { mutableStateOf(ProfileFormState("", "", "")) }
    val colors = CortexTheme.colors
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            "Register", style = CortexTheme.emphasized.headlineMedium, color = colors.textPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FormIntro()
            ProfileFormCard(
                state = form, onChange = { form = it },
                initials = Entrant(name = form.fullName).initials,
                avatarHint = "Profile picture is optional",
                onEditAvatar = onEditAvatar,
            ) {
                SectionTitle("App policy & guidelines", Modifier.padding(top = 8.dp))
                GroupedSurface {
                    LinkListItem("Policy & Guidelines", "Privacy, app policies and community guidelines", onPolicy)
                }
                CortexButton(
                    "Register", { onRegister(form.fullName.trim(), form.email.trim(), form.phone.trim()) },
                    Modifier.align(Alignment.CenterHorizontally), enabled = form.isComplete,
                )
            }
        }
    }
}

@Composable
private fun FormIntro() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Profile information")
        Text(
            "Keep your contact details up to date for your next outdoor adventure.",
            style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
        )
    }
}

private val ProfileFormSaver = androidx.compose.runtime.saveable.listSaver<ProfileFormState, String>(
    save = { listOf(it.fullName, it.email, it.phone) },
    restore = { ProfileFormState(it[0], it[1], it[2]) },
)
