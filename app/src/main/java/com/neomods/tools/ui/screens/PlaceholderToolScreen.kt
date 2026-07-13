package com.neomods.tools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.tools.ToolViewModel
import com.neomods.tools.ui.components.IconContainer
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Placeholder screen for every tool.
 *
 * Uses the final navigation architecture (deep-link by `toolId`) so each tool
 * can later be mapped to a real implementation without restructuring. For now
 * it presents the tool's identity and a "Coming Soon" state.
 */
@Composable
fun PlaceholderToolScreen(
    onBack: () -> Unit,
    viewModel: ToolViewModel
) {
    val tool = viewModel.tool

    Scaffold(
        topBar = {
            NeoTopBar(
                title = tool?.title ?: "",
                onBack = onBack
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(NeoDimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (tool != null) {
                    IconContainer(
                        iconRes = tool.iconRes,
                        contentDescription = tool.title,
                        size = 96.dp
                    )

                    Spacer(Modifier.height(NeoDimens.CardPadding))

                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(NeoDimens.SectionSpacing))

                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(NeoDimens.CardPadding))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.clip(MaterialTheme.shapes.medium)
                    ) {
                        Text(
                            text = stringResource(R.string.coming_soon),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.empty_state_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
