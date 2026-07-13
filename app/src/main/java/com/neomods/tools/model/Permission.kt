package com.neomods.tools.model

import androidx.compose.runtime.Immutable

/**
 * Immutable UI model describing a single runtime permission the app needs.
 *
 * The [androidPermission] is the value passed to the system permission APIs;
 * [special] marks permissions that cannot be requested with the normal
 * `requestPermissions` flow (e.g. `MANAGE_EXTERNAL_STORAGE`) and must be sent
 * to a system settings screen instead.
 *
 * @param id                Stable unique identifier.
 * @param title             Human readable name.
 * @param description       Why the app needs this permission.
 * @param iconRes           Drawable resource for the permission icon.
 * @param androidPermission Platform permission string, or null for special
 *                          permissions handled outside the standard flow.
 * @param special           True when this permission requires a settings
 *                          redirect instead of a normal grant dialog.
 */
@Immutable
data class Permission(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val androidPermission: String? = null,
    val special: Boolean = false
)
