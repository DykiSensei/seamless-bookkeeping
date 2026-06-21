package com.bookkeeping.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bookkeeping.app.ui.screen.budget.BudgetScreen
import com.bookkeeping.app.ui.screen.settings.SettingsScreen
import com.bookkeeping.app.ui.screen.stats.StatsScreen
import com.bookkeeping.app.ui.screen.transactions.TransactionListScreen

// 根 NavHost：底部 tab 切换
@Composable
fun RootNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                BottomNavItem.ALL.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Transactions.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomNavItem.Transactions.route) { TransactionListScreen() }
            composable(BottomNavItem.Stats.route) { StatsScreen() }
            composable(BottomNavItem.Budget.route) { BudgetScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen() }
        }
    }
}

private sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Transactions : BottomNavItem("transactions", "账单", Icons.AutoMirrored.Filled.List)
    data object Stats : BottomNavItem("stats", "统计", Icons.Filled.PieChart)
    data object Budget : BottomNavItem("budget", "预算", Icons.Filled.AccountBalanceWallet)
    data object Settings : BottomNavItem("settings", "设置", Icons.Filled.Settings)

    companion object {
        val ALL = listOf(Transactions, Stats, Budget, Settings)
    }
}
