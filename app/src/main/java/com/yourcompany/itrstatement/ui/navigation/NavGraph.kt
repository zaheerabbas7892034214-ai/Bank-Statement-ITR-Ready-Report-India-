package com.yourcompany.itrstatement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yourcompany.itrstatement.ui.screens.*
import com.yourcompany.itrstatement.ui.viewmodel.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Import : Screen("import")
    object Dashboard : Screen("dashboard/{sessionId}") {
        fun createRoute(sessionId: Long) = "dashboard/$sessionId"
    }
    object TransactionList : Screen("transactions/{sessionId}") {
        fun createRoute(sessionId: Long) = "transactions/$sessionId"
    }
    object CategoryBreakdown : Screen("categories/{sessionId}") {
        fun createRoute(sessionId: Long) = "categories/$sessionId"
    }
    object MonthlyCharts : Screen("charts/{sessionId}") {
        fun createRoute(sessionId: Long) = "charts/$sessionId"
    }
    object Export : Screen("export/{sessionId}") {
        fun createRoute(sessionId: Long) = "export/$sessionId"
    }
    object Paywall : Screen("paywall")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToImport = {
                    navController.navigate(Screen.Import.route)
                },
                onNavigateToDashboard = { sessionId ->
                    navController.navigate(Screen.Dashboard.createRoute(sessionId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                }
            )
        }

        composable(Screen.Import.route) {
            val viewModel: ImportViewModel = viewModel()
            ImportScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = { sessionId ->
                    navController.navigate(Screen.Dashboard.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val viewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.TransactionList.createRoute(sessionId))
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.CategoryBreakdown.createRoute(sessionId))
                },
                onNavigateToCharts = {
                    navController.navigate(Screen.MonthlyCharts.createRoute(sessionId))
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.TransactionList.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val viewModel: TransactionViewModel = viewModel()
            TransactionListScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.CategoryBreakdown.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val viewModel: DashboardViewModel = viewModel()
            CategoryBreakdownScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTransactions = { category ->
                    navController.navigate(Screen.TransactionList.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.MonthlyCharts.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val viewModel: DashboardViewModel = viewModel()
            MonthlyChartsScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val viewModel: ExportViewModel = viewModel()
            ExportScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                }
            )
        }

        composable(Screen.Paywall.route) {
            val viewModel: BillingViewModel = viewModel()
            PaywallScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubscriptionSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: BillingViewModel = viewModel()
            SettingsScreen(
                billingViewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                }
            )
        }
    }
}
