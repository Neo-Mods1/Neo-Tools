package com.neomods.tools.ui.screens
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.home.HomeViewModel
import com.neomods.tools.ui.components.CategoryCard
import com.neomods.tools.ui.components.EmptyState
import com.neomods.tools.ui.components.NeoSearchBar
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.components.SectionHeader
import com.neomods.tools.ui.components.ToolCard
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Main entry screen after onboarding.
 *
 * Shows a searchable, adaptive two-column grid of categories. When the user
 * types, the grid live-filters categories and (already) surfaces matching
 * tools, demonstrating that tool search can be added without UI changes.
 */
@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onToolClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }

    val categoryResults = remember(query) { viewModel.searchCategories(query) }
    val toolResults = remember(query) { viewModel.searchTools(query) }
    val hasQuery = query.isNotBlank()

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.home_title),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NeoSearchBar(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NeoDimens.ScreenPadding,
                        end = NeoDimens.ScreenPadding,
                        top = NeoDimens.SectionSpacing,
                        bottom = NeoDimens.SectionSpacing
                    )
            )

            if (categoryResults.isEmpty() && toolResults.isEmpty()) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                    verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = NeoDimens.ScreenPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = NeoDimens.ScreenPadding
                    )
                ) {
                    if (hasQuery && (categoryResults.isNotEmpty() || toolResults.isNotEmpty())) {
                        item { SectionHeader("Categories") }
                    }
                    items(categoryResults, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                    }

                    if (hasQuery && toolResults.isNotEmpty()) {
                        item { SectionHeader("Tools") }
                    }
                    items(toolResults, key = { it.id }) { tool ->
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
