package com.neomods.tools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neomods.tools.category.CategoryViewModel
import com.neomods.tools.ui.components.EmptyState
import com.neomods.tools.ui.components.GroupHeader
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.components.ToolCard
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Lists the tools inside a category, grouped by [com.neomods.tools.model.Tool.group].
 *
 * Every tool opens the placeholder screen. Each group gets a labelled divider
 * header so the screen scales as the catalogue grows.
 */
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onToolClick: (String) -> Unit,
    viewModel: CategoryViewModel
) {
    val title = viewModel.category?.title ?: ""
    val description = viewModel.category?.description ?: ""
    val groups = viewModel.groupedTools

    Scaffold(
        topBar = {
            NeoTopBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        if (viewModel.tools.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NeoDimens.ScreenPadding)
                    .padding(padding),
                contentPadding = PaddingValues(
                    top = NeoDimens.SectionSpacing,
                    bottom = NeoDimens.ScreenPadding
                )
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                groups.forEach { group ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GroupHeader(group.title)
                    }
                    items(group.tools, key = { it.id }) { tool ->
                        ToolCard(
                            tool = tool,
                            onClick = { onToolClick(tool.id) }
                        )
                    }
                }
            }
        }
    }
}
