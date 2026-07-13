package com.neomods.tools.navigation

/**
 * Centralised navigation destinations.
 *
 * Keeping every route in one place makes the graph easy to extend: add a new
 * object here, register it in [NeoNavHost], and the rest of the app keeps
 * working. Argument-based routes expose `create()` helpers so callers never
 * build raw route strings.
 */
sealed class Screen(val route: String) {

    data object Splash : Screen("splash")
    data object CrashOptIn : Screen("crash_opt_in")
    data object Permissions : Screen("permissions")
    data object AllSet : Screen("all_set")
    data object Home : Screen("home")

    data object Category : Screen("category/{categoryId}") {
        const val ARG = "categoryId"
        fun create(categoryId: String) = "category/$categoryId"
    }

    data object Tool : Screen("tool/{toolId}") {
        const val ARG = "toolId"
        fun create(toolId: String) = "tool/$toolId"
    }
}
