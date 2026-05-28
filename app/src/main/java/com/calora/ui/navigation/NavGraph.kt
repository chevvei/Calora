package com.calora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calora.ui.screen.CameraRoute
import com.calora.ui.screen.CameraViewModel
import com.calora.ui.screen.HomeRoute
import com.calora.ui.screen.ResultRoute
import com.calora.ui.screen.SettingsRoute
import com.calora.ui.screen.SplashRoute

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CAMERA = "camera"
    const val RESULT = "result"
    const val SETTINGS = "settings"
}

@Composable
fun CaloraNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashRoute(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeRoute(
                onNavigateToCamera = { navController.navigate(Routes.CAMERA) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CAMERA) {
            val cameraViewModel: CameraViewModel = hiltViewModel()
            CameraRoute(
                viewModel = cameraViewModel,
                onFoodRecognized = { navController.navigate(Routes.RESULT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RESULT) {
            val cameraViewModel: CameraViewModel = hiltViewModel()
            val result = cameraViewModel.result.value
            ResultRoute(
                result = result,
                onConfirm = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onRetake = { navController.popBackStack(Routes.CAMERA, inclusive = false) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
