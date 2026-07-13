package com.neomods.tools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.neomods.tools.R
import com.neomods.tools.crash.CrashReporter
import com.neomods.tools.ui.components.IconContainer
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Onboarding step asking the user to opt in to anonymous crash reporting.
 *
 * The choice is persisted via [CrashReporter] so the global [com.neomods.tools.crash.CrashHandler]
 * only uploads reports when the user has agreed.
 */
@Composable
fun CrashOptInScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var optedIn by remember { mutableStateOf(CrashReporter.isOptedIn(context)) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NeoDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconContainer(
                iconRes = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.app_name),
                size = 64.dp
            )

            Text(
                text = "Help improve Neo Tools",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = NeoDimens.CardPadding)
            )

            Text(
                text = "Send anonymous crash reports when the app closes unexpectedly. " +
                    "No personal data or file contents are ever collected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = NeoDimens.SectionSpacing)
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NeoDimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Send anonymous crash reports",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = optedIn,
                    onCheckedChange = {
                        optedIn = it
                        CrashReporter.setOptedIn(context, it)
                    }
                )
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NeoDimens.CardPadding)
            ) {
                Text("Continue")
            }
        }
    }
}
