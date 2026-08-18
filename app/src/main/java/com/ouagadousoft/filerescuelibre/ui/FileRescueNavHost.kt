package com.ouagadousoft.filerescuelibre.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ouagadousoft.filerescuelibre.ui.screens.HomeScreen
import com.ouagadousoft.filerescuelibre.ui.screens.ScanHistoryScreen
import com.ouagadousoft.filerescuelibre.ui.screens.ScanProgressScreen
import com.ouagadousoft.filerescuelibre.ui.screens.ScanResultsScreen
import com.ouagadousoft.filerescuelibre.viewmodel.ScanHistoryViewModel
import com.ouagadousoft.filerescuelibre.viewmodel.ScanViewModel

private object Routes {
    const val HOME = "home"
    const val SCAN_PROGRESS = "scan_progress"
    const val SCAN_RESULTS = "scan_results"
    const val HISTORY = "history"
}

@Composable
fun FileRescueNavHost(navController: NavHostController = rememberNavController()) {
    // ScanViewModel est demandé au niveau du NavHost pour rester partagé entre
    // l'écran de progression et l'écran de résultats (même scan, deux écrans).
    val scanViewModel: ScanViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val resumableDeepScan by scanViewModel.resumableDeepScan.collectAsState()
            HomeScreen(
                onQuickScan = { zone ->
                    scanViewModel.startQuickScan(zone)
                    navController.navigate(Routes.SCAN_PROGRESS)
                },
                onDeepScan = {
                    scanViewModel.startDeepScan()
                    navController.navigate(Routes.SCAN_PROGRESS)
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                resumableDeepScan = resumableDeepScan,
                onResumeDeepScan = {
                    scanViewModel.startDeepScan(resume = resumableDeepScan)
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
                onPause = {
                    scanViewModel.pauseDeepScan()
                    navController.popBackStack(Routes.HOME, inclusive = false)
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
        composable(Routes.HISTORY) {
            val historyViewModel: ScanHistoryViewModel = viewModel()
            ScanHistoryScreen(
                viewModel = historyViewModel,
                onEntryClick = { entry ->
                    scanViewModel.loadHistoryEntry(entry)
                    navController.navigate(Routes.SCAN_RESULTS)
                },
            )
        }
    }
}
