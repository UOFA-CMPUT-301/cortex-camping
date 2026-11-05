package com.example.cortex_camping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

@Composable
fun CortexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isError: Boolean = false,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    val colors = CortexTheme.colors
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val state = rememberTextFieldState(value)
    LaunchedEffect(value) { if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value) }
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.collect { if (it != value) currentOnValueChange(it) } }
    OutlinedTextField(
        state = state,
        label = { Text(label) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = placeholder != null),
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary) } },
        modifier = modifier.fillMaxWidth(),
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = MaterialTheme.shapes.extraSmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CortexPalette.BrandPrimary,
            unfocusedBorderColor = colors.borderDefault,
            focusedLabelColor = CortexPalette.BrandPrimary,
            unfocusedLabelColor = colors.textSecondary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = CortexPalette.BrandPrimary,
        ),
    )
}

data class ProfileFormState(val fullName: String, val email: String, val phone: String) {
    val emailValid get() = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isComplete get() = fullName.isNotBlank() && email.isNotBlank() && emailValid
}

@Composable
fun ProfileFormCard(
    state: ProfileFormState,
    onChange: (ProfileFormState) -> Unit,
    initials: String,
    avatarHint: String,
    onEditAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ProfileAvatar(initials.ifBlank { "?" })
                IconButton(onClick = onEditAvatar) {
                    Icon(Icons.Filled.Edit, contentDescription = "Change profile picture", tint = CortexPalette.BrandPrimary)
                }
                Column {
                    Text("Your profile", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text(avatarHint, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            CortexTextField(state.fullName, { onChange(state.copy(fullName = it)) }, "Full name")
            CortexTextField(
                state.email, { onChange(state.copy(email = it)) }, "Email address",
                keyboardType = KeyboardType.Email, isError = !state.emailValid,
            )
            CortexTextField(
                state.phone, { onChange(state.copy(phone = it)) }, "Phone number",
                keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
            )
            footer()
        }
    }
}
