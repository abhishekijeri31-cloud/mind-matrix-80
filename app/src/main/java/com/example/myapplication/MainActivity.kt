package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.AuthViewModel
import com.example.myapplication.ui.viewmodel.ReportViewModel
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure Firebase is initialized before UI
        FirebaseApp.initializeApp(this)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val authViewModel: AuthViewModel = viewModel()
                val reportViewModel: ReportViewModel = viewModel()
                val userViewModel: UserViewModel = viewModel()
                
                AppNavigation(authViewModel, reportViewModel, userViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    reportViewModel: ReportViewModel,
    userViewModel: UserViewModel
) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else {
        val startDestination = if (authViewModel.currentUser != null) "main" else "login"
        
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterNavigate = {
                        navController.navigate("register")
                    },
                    viewModel = authViewModel
                )
            }
            composable("register") {
                RegisterScreen(
                    onBackToLogin = {
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable("main") {
                MainScaffold(authViewModel, reportViewModel, userViewModel)
            }
        }
    }
}

@Composable
fun MainScaffold(
    authViewModel: AuthViewModel,
    reportViewModel: ReportViewModel,
    userViewModel: UserViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { destination ->
                    item(
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = currentDestination == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestinations.DASHBOARD.route,
                modifier = Modifier.padding()
            ) {
                composable(AppDestinations.DASHBOARD.route) { DashboardScreen(navController, reportViewModel, userViewModel) }
                composable(AppDestinations.REPORT.route) { ReportWasteScreen(reportViewModel, authViewModel) }
                composable(AppDestinations.MAPS.route) { MapScreen(reportViewModel) }
                composable(AppDestinations.REWARDS.route) { RewardsProfileScreen(userViewModel) }
                composable(AppDestinations.VOLUNTEER.route) { VolunteerScreen(reportViewModel, authViewModel) }
            }
        }

        // Profile button positioned safely
        if (currentDestination != AppDestinations.REWARDS.route) {
            IconButton(
                onClick = {
                    navController.navigate(AppDestinations.REWARDS.route)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class AppDestinations(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Home", Icons.Default.Dashboard),
    REPORT("report", "Report", Icons.Default.Report),
    MAPS("maps", "Map", Icons.Default.Map),
    REWARDS("rewards", "Rewards", Icons.Default.EmojiEvents),
    VOLUNTEER("volunteer", "Volunteer", Icons.Default.VolunteerActivism),
}
