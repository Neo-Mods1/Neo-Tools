package com.neomods.tools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neomods.tools.category.CategoryViewModel
import com.neomods.tools.ui.components.EmptyState
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.components.ToolCard
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Lists the tools inside a category. Every tool opens the placeholder screen.
 */
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onToolClick: (String) -> Unit,
    viewModel: CategoryViewModel
) {
    val title = viewModel.category?.title ?: ""

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
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NeoDimens.ScreenPadding)
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = NeoDimens.SectionSpacing,
                    bottom = NeoDimens.ScreenPadding
                )
            ) {
                items(viewModel.tools, key = { it.id }) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onToolClick(tool.id) }
                    )
                }
            }
        }
    }
}
