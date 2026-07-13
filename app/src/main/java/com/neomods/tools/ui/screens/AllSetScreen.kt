package com.neomods.tools.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.neomods.tools.R
import com.neomods.tools.ui.theme.NeoDimens
import kotlinx.coroutines.delay

/**
 * Final onboarding step shown after permissions are granted. Plays a short
 * success animation and then automatically continues into the app.
 */
@Composable
fun AllSetScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2600)
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.all_set)
            )
            LottieAnimation(
                composition = composition,
                iterations = 1,
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "You're all set",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(NeoDimens.SectionSpacing))

            Button(onClick = onFinished) {
                Text("Enter")
            }
        }
    }
}
