package com.neomods.tools.ui.components
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.neomods.tools.model.Permission
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Card explaining a single permission and offering a Grant action.
 *
 * When [granted] is true the button switches to a disabled "Granted" state.
 */
@Composable
fun PermissionCard(
    permission: Permission,
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
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
                iconRes = permission.iconRes,
                contentDescription = permission.title
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (granted) {
                OutlinedButton(onClick = onGrant, enabled = false) {
                    Text("Granted")
                }
            } else {
                Button(onClick = onGrant) {
                    Text("Grant")
                }
            }
        }
    }
}
