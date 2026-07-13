package com.neomods.tools.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.neomods.tools.model.Permission

/**
 * Pure Android permission helpers used by [PermissionViewModel] and the
 * permission screen. Kept free of Compose so it can be unit tested and reused.
 */
object PermissionManager {

    /** True when the given [permission] is currently granted. */
    fun isGranted(context: Context, permission: Permission): Boolean {
        val name = permission.androidPermission ?: return true
        return if (permission.special) {
            isSpecialGranted(context, name)
        } else {
            ContextCompat.checkSelfPermission(context, name) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isSpecialGranted(context: Context, name: String): Boolean = when (name) {
        Manifest.permission.MANAGE_EXTERNAL_STORAGE ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()
        else -> false
    }

    /**
     * Intent that opens the correct system screen to grant a permission.
     * Special permissions cannot use the normal dialog and must be granted from
     * system settings.
     */
    fun settingsIntent(context: Context, permission: Permission): Intent {
        val pkg = context.packageName
        return when {
            permission.special &&
                permission.androidPermission == Manifest.permission.MANAGE_EXTERNAL_STORAGE &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$pkg")
                }
            }

            else -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                }
            }
        }
    }
}
