package com.cszyapp.cmal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cszyapp.cmal.CMalApp
import com.cszyapp.cmal.R
import com.cszyapp.cmal.ui.home.HomeScreen
import com.cszyapp.cmal.ui.localapk.LocalApkScreen
import com.cszyapp.cmal.ui.market.MarketScreen
import com.cszyapp.cmal.ui.onboarding.OnboardingScreen
import com.cszyapp.cmal.ui.profile.ProfileScreen
import com.cszyapp.cmal.ui.resources.ResourcesScreen
import com.cszyapp.cmal.ui.servers.ServersScreen

/** 底部 Tab 定义 */
private enum class MainTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    LOCAL_APK("local_apk", R.string.tab_local_apk, Icons.Filled.InstallMobile),
    MARKET("market", R.string.tab_market, Icons.Filled.Storefront),
    RESOURCES("resources", R.string.tab_resources, Icons.Filled.Inventory2),
    SERVERS("servers", R.string.tab_servers, Icons.Filled.Public),
    PROFILE("profile", R.string.tab_profile, Icons.Filled.Person),
}

/**
 * CMAL 根导航
 * 首次启动显示引导页，之后进入主界面
 */
@Composable
fun CMALRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = CMalApp.of(context)
    val navController = rememberNavController()

    val showOnboarding = !container.settingsRepository.onboarded

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) "onboarding" else "main"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onDone = {
                    container.settingsRepository.onboarded = true
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScaffold()
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.HOME.route) {
                HomeScreen()
            }
            composable(MainTab.LOCAL_APK.route) {
                LocalApkScreen()
            }
            composable(MainTab.MARKET.route) {
                MarketScreen()
            }
            composable(MainTab.RESOURCES.route) {
                ResourcesScreen()
            }
            composable(MainTab.SERVERS.route) {
                ServersScreen()
            }
            composable(MainTab.PROFILE.route) {
                ProfileScreen()
            }
        }
    }
}
