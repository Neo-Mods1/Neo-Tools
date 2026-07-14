package com.neomods.tools.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.neomods.tools.NeoToolsApplication
import com.neomods.tools.permission.PermissionManager
import com.neomods.tools.ui.screens.AllSetScreen
import com.neomods.tools.ui.screens.Base64DecoderScreen
import com.neomods.tools.ui.screens.Base64EncoderScreen
import com.neomods.tools.ui.screens.CategoryScreen
import com.neomods.tools.ui.screens.CppHeaderDecoderScreen
import com.neomods.tools.ui.screens.CppHeaderGeneratorScreen
import com.neomods.tools.ui.screens.CrashOptInScreen
import com.neomods.tools.ui.screens.HomeScreen
import com.neomods.tools.ui.screens.about.AboutScreen
import com.neomods.tools.ui.screens.settings.SettingsScreen
import com.neomods.tools.ui.screens.PermissionScreen
import com.neomods.tools.ui.screens.PlaceholderToolScreen
import com.neomods.tools.ui.screens.SplashScreen
import com.neomods.tools.ui.screens.WelcomeIntroScreen
import com.neomods.tools.category.CategoryViewModel
import com.neomods.tools.imageeditor.ImageEditorScreen
import com.neomods.tools.apk.ApkInfoScreen
import com.neomods.tools.tools.ToolViewModel
import com.neomods.tools.ui.screens.BackgroundRemoverScreen

/**
 * Application navigation graph.
 *
 * Onboarding (Splash -> Welcome -> Crash opt-in -> Permissions -> All set) is
 * shown only when needed: each step is skipped once completed, so on
 * subsequent launches the app goes straight to Home.
 */
@Composable
fun NeoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NeoToolsApplication
    val onboarding = app.onboardingRepository
    val permissions = remember { app.permissionRepository.getPermissions() }

    fun nextOnboardingStep(): String = when {
        !onboarding.isWelcomeSeen() -> Screen.Welcome.route
        !onboarding.isCrashOptInDecided() -> Screen.CrashOptIn.route
        !PermissionManager.allGranted(context, permissions) -> Screen.Permissions.route
        else -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(nextOnboardingStep()) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeIntroScreen(
                onFinished = {
                    onboarding.setWelcomeSeen()
                    val next = when {
                        !onboarding.isCrashOptInDecided() -> Screen.CrashOptIn.route
                        !PermissionManager.allGranted(context, permissions) -> Screen.Permissions.route
                        else -> Screen.Home.route
                    }
                    navController.navigate(next) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CrashOptIn.route) {
            CrashOptInScreen(
                onContinue = {
                    onboarding.setCrashOptInDecided()
                    val next = if (PermissionManager.allGranted(context, permissions)) {
                        Screen.Home.route
                    } else {
                        Screen.Permissions.route
                    }
                    navController.navigate(next) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
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

        composable(
            Screen.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            HomeScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.Category.create(categoryId))
                },
                onToolClick = { toolId ->
                    navController.navigate(Screen.Tool.create(toolId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(250)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) }
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(
            Screen.About.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(250)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) }
        ) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument(Screen.Category.ARG) { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(150)) }
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
            arguments = listOf(navArgument(Screen.Tool.ARG) { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) { backStack ->
            val toolId = backStack.arguments?.getString(Screen.Tool.ARG) ?: ""
            when (toolId) {
                "enc_base64" -> Base64EncoderScreen(onBack = { navController.popBackStack() })
                "dec_base64" -> Base64DecoderScreen(onBack = { navController.popBackStack() })
                "enc_cpp_header" -> CppHeaderGeneratorScreen(onBack = { navController.popBackStack() })
                "dec_cpp_header" -> CppHeaderDecoderScreen(onBack = { navController.popBackStack() })
                "img_editor" -> ImageEditorScreen(onBack = { navController.popBackStack() })
                "bg_remover" -> BackgroundRemoverScreen(onBack = { navController.popBackStack() })
                "apk_info" -> ApkInfoScreen(onBack = { navController.popBackStack() })
                else -> {
                    val viewModel: ToolViewModel = viewModel(backStack)
                    PlaceholderToolScreen(
                        onBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
