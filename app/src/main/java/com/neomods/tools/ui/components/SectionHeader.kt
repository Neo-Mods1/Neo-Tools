package com.neomods.tools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/**
 * A labelled divider used to section tools inside a category screen
 * (e.g. "Analysis", "Editing", "Build").
 */
@Composable
fun GroupHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                top = NeoDimens.GroupSpacing,
                bottom = NeoDimens.GroupSpacing
            )
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
