package com.neomods.tools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.neomods.tools.R
import com.neomods.tools.model.Category
import com.neomods.tools.model.Tool
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Large monochrome lead icon used across the premium "toolbox" cards.
 *
 * Per the design language the icon has no tinted background — it floats above
 * the text and uses the single accent colour (the theme [primary]).
 */
@Composable
fun LeadIcon(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = NeoDimens.LeadIconSize,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * Grid cell representing a category on the Home screen.
 *
 * Designed to feel like a premium toolbox card: a large lead icon floating
 * above a bold title, a short "what's inside" tag line, and a tool-count footer
 * with a navigation chevron. Tapping it opens the category screen.
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

    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                size = NeoDimens.LeadIconSizeLarge
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
 * Mirrors [CategoryCard] visually but without the tool-count footer: a large
 * lead icon floats above a bold title (with a chevron) and a short description.
 * A tool is a leaf in the navigation graph (it opens the placeholder screen).
 */
@Composable
fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                size = NeoDimens.LeadIconSize
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
