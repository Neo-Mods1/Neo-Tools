package com.neomods.tools.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base activity for the traditional (non-Compose) screens, such as the crash
 * report screen. Provides a coroutine scope, system bar theming and a simple
 * view-binding style [bindLayout] contract.
 *
 * Ported from the CODE-IDE crash system (com.neo.ide.app.BaseActivity). also owned by me neo mods
 */
abstract class BaseActivity : AppCompatActivity() {

    val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    open val enableSystemBarTheming: Boolean = true

    open val navigationBarColor: Int
        get() = android.graphics.Color.BLACK

    open val statusBarColor: Int
        get() = android.graphics.Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        if (enableSystemBarTheming) {
            applySystemBarColors()
        }
        super.onCreate(savedInstanceState)
        setContentView(bindLayout())
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    /** Subclasses must implement this to return their root view. */
    protected abstract fun bindLayout(): View

    /** Apply system bar colors from the activity's properties. */
    protected open fun applySystemBarColors() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
            isAppearanceLightStatusBars = false
        }
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
    }
}
