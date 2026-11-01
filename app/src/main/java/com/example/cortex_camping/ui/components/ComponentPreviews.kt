package com.example.cortex_camping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.preview.PreviewData
import com.example.cortex_camping.ui.theme.CortexTheme

@Preview(name = "Buttons", showBackground = true, backgroundColor = 0xFFF4F7F2)
@Composable
private fun ButtonsPreview() = CortexTheme {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CortexButton("Message Owner", {})
            CortexButton("Cancel", {}, style = CortexButtonStyle.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CortexButton("Filter", {}, style = CortexButtonStyle.Outlined, leadingIcon = Icons.Outlined.FilterList)
            CortexButton("Edit Profile", {}, style = CortexButtonStyle.Outlined, size = CortexButtonSize.XSmall, leadingIcon = Icons.Filled.Edit)
            CortexButton("Lottery full", {}, enabled = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CortexButton("Mark all read", {}, style = CortexButtonStyle.Text)
            CortexButton("Delete Profile", {}, style = CortexButtonStyle.TextDanger)
        }
    }
}

@Preview(name = "Segmented + chips", showBackground = true, backgroundColor = 0xFFF4F7F2)
@Composable
private fun SelectionPreview() = CortexTheme {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CortexSegmentedToggle(listOf("Upcoming", "Past"), tab, { tab = it })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Tent site", "Water", "Fire ring").forEach { AmenityChip(it) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusPill("Open"); ProfileAvatar("CC") }
    }
}

@Preview(name = "Cards", showBackground = true, backgroundColor = 0xFFF4F7F2, heightDp = 900)
@Composable
private fun CardsPreview() = CortexTheme {
    val site = PreviewData.campsite
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CampsiteListCard(site, {})
        ReservationCard(PreviewData.reservation, site, {}, {})
        LotteryEntryCard(PreviewData.lottery, site, PreviewData.USER_ID, {})
        CampsiteLotteryCard(PreviewData.lottery, PreviewData.USER_ID, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Bars", showBackground = true, backgroundColor = 0xFFF4F7F2)
@Composable
private fun BarsPreview() = CortexTheme {
    Column {
        CortexBackTopBar("Campsite details", {})
        CortexBottomBar(TopLevelTab.Browse, {})
    }
}
