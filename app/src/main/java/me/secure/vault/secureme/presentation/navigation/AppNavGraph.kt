package me.secure.vault.secureme.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.secure.vault.secureme.presentation.auth.login.LoginScreen
import me.secure.vault.secureme.presentation.fileviewer.FileViewerScreen
import me.secure.vault.secureme.presentation.home.HomeScreen
import me.secure.vault.secureme.presentation.onboarding.OnboardingScreen
import me.secure.vault.secureme.presentation.splash.SplashScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.SPLASH,
    ) {
        composable(NavigationRoutes.SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(
            route = NavigationRoutes.ONBOARDING,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            OnboardingScreen(navController = navController)
        }
        composable(
            route = NavigationRoutes.LOGIN,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            LoginScreen(navController = navController)
        }
        composable(
            route = NavigationRoutes.HOME,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            HomeScreen(navController = navController)
        }
        composable(
            route = "${NavigationRoutes.FILE_VIEWER}/{fileId}",
            arguments = listOf(androidx.navigation.navArgument("fileId") { type = androidx.navigation.NavType.StringType }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
            FileViewerScreen(fileId = fileId, navController = navController)
        }
    }
}
