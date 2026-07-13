package com.neomods.tools.ui.components
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.neomods.tools.R
import com.neomods.tools.model.Category
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Grid cell representing a category on the Home screen.
 *
 * Tapping it opens the category screen. The trailing chevron communicates that
 * the card navigates one level deeper.
 */
@Composable
fun CategoryCard(
    category: Category,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(NeoDimens.CardPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing)
        ) {
            IconContainer(
                iconRes = category.iconRes,
                contentDescription = category.title
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = stringResource(R.string.next_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Square, tinted icon container shared by [CategoryCard] and [ToolCard].
 */
@Composable
fun IconContainer(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = NeoDimens.IconContainerSize
) {
    Surface(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size / 2)
        )
    }
}
