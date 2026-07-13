package com.neomods.tools.data

import android.Manifest
import com.neomods.tools.R
import com.neomods.tools.model.Permission

/**
 * Static description of the permissions Neo Tools asks for.
 *
 * Kept separate from the runtime state (see [com.neomods.tools.permission.PermissionViewModel])
 * so the UI never depends on Android permission strings directly.
 */
interface PermissionRepository {
    fun getPermissions(): List<Permission>
}

internal class DefaultPermissionRepository : PermissionRepository {

    override fun getPermissions(): List<Permission> = listOf(
        Permission(
            id = "storage",
            title = "Storage",
            description = "Read and write files needed for reverse engineering tasks such as unpacking APKs and inspecting binaries.",
            iconRes = R.drawable.ic_perm_storage,
            androidPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        ),
        Permission(
            id = "notifications",
            title = "Notifications",
            description = "Show progress and completion notifications for long running operations.",
            iconRes = R.drawable.ic_perm_notification,
            androidPermission = Manifest.permission.POST_NOTIFICATIONS
        ),
        Permission(
            id = "manage_storage",
            title = "Manage External Storage",
            description = "Required on Android 11+ to access files outside your app-specific directories.",
            iconRes = R.drawable.ic_perm_manage,
            androidPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            special = true
        )
    )
}
