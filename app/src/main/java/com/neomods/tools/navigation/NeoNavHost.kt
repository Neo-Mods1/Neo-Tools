package com.neomods.tools.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.ui.screens.AllSetScreen
import com.neomods.tools.ui.screens.CategoryScreen
import com.neomods.tools.ui.screens.CrashOptInScreen
import com.neomods.tools.ui.screens.HomeScreen
import com.neomods.tools.ui.screens.PermissionScreen
import com.neomods.tools.ui.screens.PlaceholderToolScreen
import com.neomods.tools.ui.screens.SplashScreen
import com.neomods.tools.category.CategoryViewModel
import com.neomods.tools.tools.ToolViewModel

/**
 * Application navigation graph.
 *
 * Onboarding flow (Splash -> Crash opt-in -> Permissions -> All set) is kept
 * separate from the main flow (Home -> Category -> Tool) so the back stack can
 * be cleared once onboarding completes.
 */
@Composable
fun NeoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.CrashOptIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CrashOptIn.route) {
            CrashOptInScreen(
                onContinue = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.CrashOptIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionScreen(
                onAllGranted = {
                    navController.navigate(Screen.AllSet.route)
                }
            )
        }

        composable(Screen.AllSet.route) {
            AllSetScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.Category.create(categoryId))
                },
                onToolClick = { toolId ->
                    navController.navigate(Screen.Tool.create(toolId))
                }
            )
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument(Screen.Category.ARG) { type = NavType.StringType })
        ) { backStack ->
            val viewModel: CategoryViewModel = viewModel(backStack)
            CategoryScreen(
                onBack = { navController.popBackStack() },
                onToolClick = { toolId ->
                    navController.navigate(Screen.Tool.create(toolId))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Tool.route,
            arguments = listOf(navArgument(Screen.Tool.ARG) { type = NavType.StringType })
        ) { backStack ->
            val viewModel: ToolViewModel = viewModel(backStack)
            PlaceholderToolScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
