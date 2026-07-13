package com.neomods.tools.permission

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import com.neomods.tools.NeoToolsApplication
import com.neomods.tools.model.Permission

/**
 * Holds the list of required permissions and their live granted state.
 *
 * The granted map is observable so the UI updates immediately after a grant
 * result or when the screen resumes (e.g. returning from a settings redirect
 * for a special permission).
 */
class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as NeoToolsApplication).permissionRepository

    val permissions: List<Permission> = repository.getPermissions()

    private val _granted = mutableStateMapOf<String, Boolean>()
    val granted: Map<String, Boolean> get() = _granted

    val allGranted: Boolean
        get() = permissions.isNotEmpty() && permissions.all { _granted[it.id] == true }

    /** Recompute every permission's granted state from the system. */
    fun refresh(context: android.content.Context) {
        for (permission in permissions) {
            _granted[permission.id] = PermissionManager.isGranted(context, permission)
        }
    }
}
