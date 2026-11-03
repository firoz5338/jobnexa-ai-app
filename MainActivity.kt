package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.supabase.AuthState
import com.example.ui.auth.JobNexaAuthBottomSheet
import com.example.ui.components.JobNexaBrandHeader
import com.example.ui.screens.CandidateProfileScreen
import com.example.ui.screens.DevDiagnosticsDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobDetailBottomSheet
import com.example.ui.screens.MyApplicationsScreen
import com.example.ui.screens.SearchJobsScreen
import com.example.ui.theme.JobNexaAiBlue
import com.example.ui.theme.JobNexaNavy
import com.example.ui.theme.JobNexaTheme
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.viewmodel.GlobalHireViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JobNexaTheme(darkTheme = false) {
                JobNexaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobNexaApp(viewModel: GlobalHireViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedJob by viewModel.selectedJobForDetail.collectAsStateWithLifecycle()
    val matches by viewModel.allMatches.collectAsStateWithLifecycle()
    val notification by viewModel.userNotification.collectAsStateWithLifecycle()
    val applications by viewModel.allApplications.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val showAuthSheet by viewModel.showAuthSheet.collectAsStateWithLifecycle()
    val showDevDiagnostics by viewModel.showDevDiagnostics.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileMenu by remember { mutableStateOf(false) }

    val isAuthenticated = authState is AuthState.Authenticated || authState is AuthState.SandboxDemo

    // Handle back button for sub-screens
    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.navigateTo(Screen.HOME)
    }

    LaunchedEffect(notification) {
        notification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    JobNexaBrandHeader(
                        symbolSize = 32.dp,
                        titleSize = 17.sp,
                        showTagline = false
                    )
                },
                actions = {
                    if (!isAuthenticated) {
                        // Right when logged out: Sign In and Sign Up buttons
                        TextButton(
                            onClick = { viewModel.openSignIn() },
                            modifier = Modifier.testTag("header_signin_button")
                        ) {
                            Text(
                                text = "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = JobNexaNavy,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.openSignUp() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JobNexaNavy),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("header_signup_button")
                        ) {
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // Right when logged in: profile avatar and simple profile menu
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            val userInitial = when (val s = authState) {
                                is AuthState.Authenticated -> s.user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                                else -> "C"
                            }

                            Surface(
                                shape = CircleShape,
                                color = JobNexaNavy,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { showProfileMenu = true }
                                    .testTag("profile_avatar_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userInitial,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = JobNexaAiBlue) },
                                    onClick = {
                                        viewModel.navigateTo(Screen.PROFILE)
                                        showProfileMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("System Diagnostics", fontWeight = FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Slate600) },
                                    onClick = {
                                        viewModel.showDevDiagnostics.value = true
                                        showProfileMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        viewModel.logOut()
                                        showProfileMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = JobNexaNavy
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            // Main Navigation (4 tabs only: Home, Search Jobs, My Applications, Profile)
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                // 1. Home
                NavigationBarItem(
                    selected = currentScreen == Screen.HOME || currentScreen == Screen.DASHBOARD,
                    onClick = { viewModel.navigateTo(Screen.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JobNexaNavy,
                        selectedTextColor = JobNexaNavy,
                        indicatorColor = Slate100,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_home")
                )

                // 2. Search Jobs
                NavigationBarItem(
                    selected = currentScreen == Screen.SEARCH_JOBS || currentScreen == Screen.JOB_DISCOVERY,
                    onClick = { viewModel.navigateTo(Screen.SEARCH_JOBS) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search Jobs") },
                    label = { Text("Search Jobs", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JobNexaNavy,
                        selectedTextColor = JobNexaNavy,
                        indicatorColor = Slate100,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_search")
                )

                // 3. My Applications
                NavigationBarItem(
                    selected = currentScreen == Screen.MY_APPLICATIONS || currentScreen == Screen.APPLICATIONS_PIPELINE || currentScreen == Screen.RESPONSES_AND_OFFERS,
                    onClick = { viewModel.navigateTo(Screen.MY_APPLICATIONS) },
                    icon = {
                        if (applications.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = JobNexaAiBlue) {
                                        Text("${applications.size}", color = Color.White)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = "My Applications")
                            }
                        } else {
                            Icon(Icons.Default.Assignment, contentDescription = "My Applications")
                        }
                    },
                    label = { Text("My Applications", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JobNexaNavy,
                        selectedTextColor = JobNexaNavy,
                        indicatorColor = Slate100,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_my_applications")
                )

                // 4. Profile
                NavigationBarItem(
                    selected = currentScreen == Screen.PROFILE || currentScreen == Screen.PROFILE_AND_CV,
                    onClick = { viewModel.navigateTo(Screen.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JobNexaNavy,
                        selectedTextColor = JobNexaNavy,
                        indicatorColor = Slate100,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (currentScreen) {
                Screen.HOME, Screen.DASHBOARD -> HomeScreen(viewModel = viewModel)
                Screen.SEARCH_JOBS, Screen.JOB_DISCOVERY -> SearchJobsScreen(viewModel = viewModel)
                Screen.MY_APPLICATIONS, Screen.APPLICATIONS_PIPELINE, Screen.RESPONSES_AND_OFFERS -> MyApplicationsScreen(viewModel = viewModel)
                Screen.PROFILE, Screen.PROFILE_AND_CV -> CandidateProfileScreen(viewModel = viewModel)
                Screen.DEV_DIAGNOSTICS, Screen.ADMIN_PANEL -> {
                    HomeScreen(viewModel = viewModel)
                    viewModel.showDevDiagnostics.value = true
                }
            }
        }
    }

    // Modal Bottom Sheet when viewing a job
    if (selectedJob != null) {
        val jobMatch = matches.firstOrNull { it.jobId == selectedJob!!.id }
        JobDetailBottomSheet(
            viewModel = viewModel,
            job = selectedJob!!,
            match = jobMatch,
            onDismiss = { viewModel.closeJobDetail() }
        )
    }

    // Modal Bottom Sheet for Authentication (Sign In / Sign Up)
    if (showAuthSheet) {
        JobNexaAuthBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowAuthSheet(false) }
        )
    }

    // Developer & Diagnostics Modal
    if (showDevDiagnostics) {
        DevDiagnosticsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showDevDiagnostics.value = false }
        )
    }
}
