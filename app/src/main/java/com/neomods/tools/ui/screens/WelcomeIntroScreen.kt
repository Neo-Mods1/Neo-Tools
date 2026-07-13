package com.neomods.tools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neomods.tools.R
import com.neomods.tools.ui.components.LeadIcon
import com.neomods.tools.ui.theme.NeoDimens

/**
 * First-run introduction shown after the splash and before the permission /
 * crash-reporting steps. Slides introduce the app's purpose and toolkit so the
 * user understands what Neo Tools is before any prompts appear.
 */
@Composable
fun WelcomeIntroScreen(onFinished: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val slide = welcomeSlides[page]
    val isLast = page == welcomeSlides.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NeoDimens.ScreenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished) {
                    Text(stringResource(R.string.welcome_skip))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LeadIcon(
                    iconRes = slide.iconRes,
                    contentDescription = slide.title,
                    size = 88.dp,
                    tint = if (page == 0) Color.Unspecified else MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = slide.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = NeoDimens.SectionSpacing),
                horizontalArrangement = Arrangement.Center
            ) {
                welcomeSlides.forEachIndexed { index, _ ->
                    val selected = index == page
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Button(
                onClick = { if (isLast) onFinished() else page++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isLast) stringResource(R.string.welcome_get_started)
                    else stringResource(R.string.welcome_next)
                )
            }
        }
    }
}

private data class WelcomeSlide(
    val iconRes: Int,
    val title: String,
    val body: String
)

private val welcomeSlides = listOf(
    WelcomeSlide(
        R.mipmap.ic_launcher,
        "Welcome to Neo Tools",
        "The complete reverse engineering toolkit for Android, built for professionals."
    ),
    WelcomeSlide(
        R.drawable.ic_cat_apk,
        "Inspect & Rebuild",
        "Dive into Dex, smali, manifests and signing — everything in one place."
    ),
    WelcomeSlide(
        R.drawable.ic_cat_binary,
        "Every Tool You Need",
        "Binary, image, XML, encoding, crypto and developer utilities at your fingertips."
    )
)
