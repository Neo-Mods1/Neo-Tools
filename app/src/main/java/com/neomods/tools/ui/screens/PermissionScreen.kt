package com.neomods.tools.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.model.Permission
import com.neomods.tools.permission.PermissionManager
import com.neomods.tools.permission.PermissionViewModel
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.components.PermissionCard
import com.neomods.tools.ui.theme.NeoDimens

/**
 * Onboarding step that explains and requests each required permission.
 *
 * Normal permissions use the standard request dialog; special permissions
 * (e.g. all-files access) redirect to system settings. The screen refreshes
 * its granted state on every resume so redirects are reflected automatically.
 */
@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PermissionViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refresh(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refresh(context)
        if (viewModel.allGranted) onAllGranted()
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refresh(context)
        if (viewModel.allGranted) onAllGranted()
    }

    fun handleGrant(permission: Permission) {
        if (PermissionManager.isGranted(context, permission)) {
            viewModel.refresh(context)
            return
        }
        if (permission.special) {
            settingsLauncher.launch(PermissionManager.settingsIntent(context, permission))
        } else if (permission.androidPermission != null) {
            requestPermissionLauncher.launch(permission.androidPermission)
        }
    }

    val allGranted = viewModel.allGranted

    Scaffold(
        topBar = {
            NeoTopBar(title = stringResource(R.string.permission_title))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NeoDimens.ScreenPadding,
                        end = NeoDimens.ScreenPadding,
                        top = NeoDimens.ScreenPadding,
                        bottom = NeoDimens.SectionSpacing
                    )
            ) {
                Text(
                    text = stringResource(R.string.permission_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NeoDimens.ScreenPadding,
                    end = NeoDimens.ScreenPadding
                ),
                verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(viewModel.permissions, key = { it.id }) { permission ->
                    PermissionCard(
                        permission = permission,
                        granted = viewModel.granted[permission.id] == true,
                        onGrant = { handleGrant(permission) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NeoDimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (allGranted) {
                        stringResource(R.string.permission_all_granted)
                    } else {
                        stringResource(R.string.permission_pending)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onAllGranted,
                    enabled = allGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = NeoDimens.SectionSpacing)
                ) {
                    Text(stringResource(R.string.permission_continue))
                }
            }
        }
    }
}
