package com.neomods.tools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.home.HomeViewModel
import com.neomods.tools.ui.components.CategoryCard
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.theme.NeoDimens

@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onToolClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val categories = remember { viewModel.getCategories() }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = NeoDimens.ScreenPadding),
            contentPadding = PaddingValues(
                top = NeoDimens.SectionSpacing,
                bottom = NeoDimens.ScreenPadding
            )
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryCard(
                    category = category,
                    toolCount = viewModel.toolCount(category.id),
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}
