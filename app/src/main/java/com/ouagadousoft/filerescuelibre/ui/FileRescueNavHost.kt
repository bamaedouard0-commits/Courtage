package com.ouagadousoft.filerescuelibre.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ouagadousoft.filerescuelibre.ui.screens.HomeScreen
import com.ouagadousoft.filerescuelibre.ui.screens.ScanProgressScreen
import com.ouagadousoft.filerescuelibre.ui.screens.ScanResultsScreen
import com.ouagadousoft.filerescuelibre.viewmodel.ScanViewModel

private object Routes {
    const val HOME = "home"
    const val SCAN_PROGRESS = "scan_progress"
    const val SCAN_RESULTS = "scan_results"
}

@Composable
fun FileRescueNavHost(navController: NavHostController = rememberNavController()) {
    // ScanViewModel est demandé au niveau du NavHost pour rester partagé entre
    // l'écran de progression et l'écran de résultats (même scan, deux écrans).
    val scanViewModel: ScanViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onQuickScan = {
                    scanViewModel.startQuickScan()
                    navController.navigate(Routes.SCAN_PROGRESS)
                },
                onDeepScan = {
                    scanViewModel.startDeepScan()
                    navController.navigate(Routes.SCAN_PROGRESS)
                },
            )
        }
        composable(Routes.SCAN_PROGRESS) {
            ScanProgressScreen(
                viewModel = scanViewModel,
                onScanCompleted = {
                    navController.navigate(Routes.SCAN_RESULTS) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.SCAN_RESULTS) {
            ScanResultsScreen(
                viewModel = scanViewModel,
                onNewScan = {
                    scanViewModel.reset()
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
