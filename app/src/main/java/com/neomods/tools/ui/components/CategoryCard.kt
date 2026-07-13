package com.neomods.tools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.neomods.tools.R
import com.neomods.tools.model.Category
import com.neomods.tools.model.Tool
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Picks a stable, theme-aware accent (container + on-color) for a given key so
 * category/tool icons get a little variety while still following the active
 * Material You / dynamic colour scheme.
 */
@Composable
private fun accentFor(key: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val accents = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer
    )
    val index = kotlin.math.abs(key.hashCode()) % accents.size
    return accents[index]
}

/**
 * Large lead icon used across the premium "toolbox" cards.
 *
 * Rendered inside a theme-coloured rounded chip and tinted with the matching
 * on-colour, so every icon follows the active Material You / dynamic colour
 * scheme instead of a fixed colour. The drawable is tinted (SrcIn) so both
 * monochrome XML assets and coloured PNGs resolve to the same on-brand glyph.
 */
@Composable
fun LeadIcon(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = NeoDimens.LeadIconSize,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Surface(
        modifier = modifier.size(size + 22.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = NeoDimens.CardElevation
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Grid cell representing a category on the Home screen.
 *
 * Premium toolbox card: a theme-coloured icon chip floats above a bold title,
 * a short "what's inside" tag line, and a tool-count footer with a navigation
 * chevron. Tapping it opens the category screen.
 */
@Composable
fun CategoryCard(
    category: Category,
    toolCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countText = LocalContext.current.resources.getQuantityString(
        R.plurals.tools_count,
        toolCount,
        toolCount
    )
    val (container, onContainer) = accentFor(category.id)

    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NeoDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NeoDimens.CardPadding)
        ) {
            LeadIcon(
                iconRes = category.iconRes,
                contentDescription = category.title,
                size = NeoDimens.LeadIconSizeLarge,
                containerColor = container,
                tint = onContainer
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category.tags.take(3).joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(R.string.next_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(NeoDimens.IconSize)
                )
            }
        }
    }
}

/**
 * Grid cell representing a tool inside a category screen.
 *
 * Mirrors [CategoryCard] visually but without the tool-count footer: a
 * theme-coloured icon chip floats above a bold title (with a chevron) and a
 * short description. A tool is a leaf in the navigation graph.
 */
@Composable
fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, onContainer) = accentFor(tool.id)

    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NeoDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NeoDimens.CardPadding)
        ) {
            LeadIcon(
                iconRes = tool.iconRes,
                contentDescription = tool.title,
                size = NeoDimens.LeadIconSize,
                containerColor = container,
                tint = onContainer
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing)
            ) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(R.string.next_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(NeoDimens.IconSize)
                )
            }
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
