package com.neomods.tools.imageeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val stickers = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE21", "\uD83D\uDE31",
    "\uD83E\uDD14", "\uD83D\uDE0E", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4F",
    "\uD83D\uDCAA", "\uD83C\uDF1F", "\uD83C\uDF08", "\uD83D\uDD25", "\u2B50",
    "\uD83C\uDF88", "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83D\uDCAB", "\uD83C\uDF3B",
    "\uD83C\uDF33", "\uD83C\uDF34", "\uD83C\uDF35", "\uD83C\uDF3A", "\uD83C\uDF37",
    "\u2600\uFE0F", "\uD83C\uDF19", "\u26C5", "\uD83C\uDF27\uFE0F", "\uD83C\uDF2A\uFE0F",
    "\uD83D\uDCA5", "\uD83C\uDF0B", "\uD83D\uDC8E", "\uD83D\uDC8D", "\uD83D\uDC8B",
    "\uD83C\uDF54", "\uD83C\uDF55", "\uD83C\uDF53", "\uD83C\uDF52", "\uD83C\uDF51",
    "\uD83C\uDF56", "\uD83C\uDF57", "\uD83C\uDF5C", "\uD83C\uDF59", "\uD83C\uDF5B",
    "\uD83C\uDF5D", "\uD83C\uDF5E", "\uD83C\uDF63", "\uD83C\uDF66", "\uD83C\uDF70",
    "\uD83C\uDF69", "\uD83C\uDF6A", "\uD83C\uDF6B", "\uD83C\uDF6C", "\uD83C\uDF6D",
    "\uD83D\uDC34", "\uD83D\uDC35", "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC2D",
    "\uD83D\uDC39", "\uD83D\uDC30", "\uD83E\uDD8A", "\uD83D\uDC3B", "\uD83E\uDDA8",
    "\uD83D\uDC2F", "\uD83E\uDD81", "\uD83D\uDC18", "\uD83D\uDC2C", "\uD83D\uDC0D",
)

@Composable
fun StickerTools(
    onStickerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .heightIn(max = 300.dp)
    ) {
        Text("Stickers", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(stickers) { emoji ->
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clickable { onStickerSelected(emoji) }
                        .padding(4.dp)
                )
            }
        }
    }
}
