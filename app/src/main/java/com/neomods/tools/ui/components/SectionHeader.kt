package com.neomods.tools.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Small uppercase section label used to separate groups of content
 * (e.g. "Categories" vs "Tools" in search results).
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = 4.dp,
            top = NeoDimens.SectionSpacing,
            bottom = NeoDimens.SectionSpacing
        )
    )
}
