package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Message
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.ui.components.CampsiteSummaryHeader
import com.example.cortex_camping.ui.components.ChatBubble
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexMediumTopBar
import com.example.cortex_camping.ui.components.CortexTextField
import com.example.cortex_camping.ui.components.formatDate
import com.example.cortex_camping.ui.components.formatRange
import com.example.cortex_camping.ui.components.formatShortDate
import com.example.cortex_camping.ui.components.formatTime
import com.example.cortex_camping.ui.components.nightsGuests
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.LocalDate

/** Figma frame "Reservation messages" for campers */
@Composable
fun ChatScreen(
    campsite: Campsite,
    reservation: Reservation?,
    messages: List<Message>,
    userId: String,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) = ConversationScreen(
    campsite = campsite,
    subtitle = reservation?.let { formatRange(it.checkIn, it.checkOut) } ?: campsite.location,
    meta = reservation?.let { nightsGuests(it.nights, it.guests) } ?: "",
    status = reservation?.status?.label,
    messages = messages, userId = userId, otherLabel = "Owner",
    onSend = onSend,
    topBar = { CortexBackTopBar("Campsite Owner Chat", onBack) },
)

/** Figma frame "Reservation messages" in Owner */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerChatScreen(
    guestName: String,
    campsite: Campsite,
    reservation: Reservation?,
    messages: List<Message>,
    userId: String,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) = ConversationScreen(
    campsite = campsite,
    subtitle = reservation?.let { formatRange(it.checkIn, it.checkOut) } ?: campsite.location,
    meta = reservation?.let { nightsGuests(it.nights, it.guests) } ?: "",
    status = reservation?.status?.label,
    messages = messages, userId = userId, otherLabel = guestName.substringBefore(" "),
    onSend = onSend,
    topBar = { CortexMediumTopBar(guestName) },
    headerExtra = { CortexButton("Back to reservations", onBack, style = CortexButtonStyle.OutlinedBrand) },
)

private fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today · ${formatShortDate(date)}"
    LocalDate.now().minusDays(1) -> "Yesterday · ${formatShortDate(date)}"
    else -> formatDate(date)
}

@Composable
private fun ConversationScreen(
    campsite: Campsite,
    subtitle: String,
    meta: String,
    status: String?,
    messages: List<Message>,
    userId: String,
    otherLabel: String,
    onSend: (String) -> Unit,
    topBar: @Composable () -> Unit,
    headerExtra: (@Composable () -> Unit)? = null,
) {
    val colors = CortexTheme.colors
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }

    Scaffold(
        topBar = topBar,
        containerColor = colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(colors.surfaceDefault).navigationBarsPadding().imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CortexTextField(draft, { draft = it }, "Message", Modifier.weight(1f), imeAction = ImeAction.Send, placeholder = "Type a message…")
                CortexButton("Send", { if (draft.isNotBlank()) { onSend(draft); draft = "" } }, modifier = Modifier.padding(top = 6.dp))
            }
        },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (headerExtra != null) item { headerExtra() }
            item { CampsiteSummaryHeader(campsite = campsite, subtitle = subtitle, meta = meta, status = status) }
            if (messages.isEmpty()) item {
                Text("No messages yet. Say hello!", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            messages.forEachIndexed { i, message ->
                val day = message.sentAt.toLocalDate()
                if (i == 0 || messages[i - 1].sentAt.toLocalDate() != day) item(key = "day-$day") {
                    Text(dayLabel(day), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                item(key = message.id) {
                    val mine = message.senderId == userId
                    ChatBubble(message.text, "${if (mine) "You" else otherLabel} · ${formatTime(message.sentAt)}", incoming = !mine)
                }
            }
        }
    }
}
