@file:OptIn(ExperimentalMaterial3Api::class)

package com.skinsense.ai.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skinsense.ai.R
import com.skinsense.ai.data.AnalysisResult
import com.skinsense.ai.data.DiseaseRepository
import com.skinsense.ai.ui.MainViewModel
import com.skinsense.ai.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Navigation Routes
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PhotoGuidelines : Screen("photo_guidelines")
    object Upload : Screen("upload")
    object Results : Screen("results")
    object DiseaseLibrary : Screen("disease_library")
    object DiseaseDetail : Screen("disease_detail/{diseaseId}") {
        fun createRoute(diseaseId: String) = "disease_detail/$diseaseId"
    }
    object History : Screen("history")
    object HistoryDetail : Screen("history_detail/{historyId}") {
        fun createRoute(historyId: String) = "history_detail/$historyId"
    }
    object Chatbot : Screen("chatbot")

    object Profile : Screen("profile")
    object DoctorDashboard : Screen("doctor_dashboard")
    object FindDoctor : Screen("find_doctor?resultJson={resultJson}") {
        fun createRoute(resultJson: String? = null) = "find_doctor" + (resultJson?.let { "?resultJson=${Uri.encode(it)}" } ?: "")
    }
    object MyConsultations : Screen("my_consultations")
    object DoctorDetail : Screen("doctor_detail/{doctorId}?resultJson={resultJson}") {
        fun createRoute(doctorId: String, resultJson: String? = null) = 
            "doctor_detail/$doctorId" + (resultJson?.let { "?resultJson=${Uri.encode(it)}" } ?: "")
    }
    object ConsultationChat : Screen("consultation_chat/{consultationId}") { // New Route
        fun createRoute(consultationId: String) = "consultation_chat/$consultationId"
    }
    object Splash : Screen("splash") // New Route
    object PatientDetail : Screen("patient_detail/{patientId}") {
        fun createRoute(patientId: String) = "patient_detail/$patientId"
    }
    // Email Auth Routes
    object Landing : Screen("landing")
    object PatientLogin : Screen("patient_login")
    object DoctorLogin : Screen("doctor_login")
    object AdminLogin : Screen("admin_login")
    object PatientSignUp : Screen("patient_signup")
    object DoctorSignUp : Screen("doctor_signup")
    object AdminDashboard : Screen("admin_dashboard")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{chatId}/{otherName}") {
        fun createRoute(chatId: String, otherName: String) = "chat/$chatId/$otherName"
    }
    object CompleteProfile : Screen("complete_profile")
    object ChooseLanguage : Screen("choose_language")
    object MyAppointments : Screen("my_appointments")
}

/**
 * Bottom Navigation Items
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int) {
    object Home : BottomNavItem(Screen.Home.route, Icons.Default.Home, R.string.nav_home)
    object Doctors : BottomNavItem("find_doctor", Icons.Default.MedicalServices, R.string.nav_consult)
    object Scan : BottomNavItem(Screen.PhotoGuidelines.route, Icons.Default.CameraAlt, R.string.nav_scan)
    object Chatbot : BottomNavItem(Screen.Chatbot.route, Icons.Default.SmartToy, R.string.nav_chatbot)
    object MyAppointments : BottomNavItem(Screen.MyAppointments.route, Icons.Default.CalendarToday, R.string.nav_consult)
    object Chats : BottomNavItem(Screen.ChatList.route, Icons.Default.Message, R.string.nav_chats)
}

/**
 * Main Navigation Graph
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinSenseNavigation(
    navController: NavHostController = rememberNavController(),
    authRepository: com.skinsense.ai.data.AuthRepository = com.skinsense.ai.data.FirebaseAuthRepository(),
    startDestination: String? = null, // Changed to null to allow dynamic calculation if not provided
    viewModel: MainViewModel = viewModel(
        factory = com.skinsense.ai.ui.MainViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            authRepository
        )
    ),
    authViewModel: com.skinsense.ai.ui.AuthViewModel = viewModel(
        factory = com.skinsense.ai.ui.AuthViewModelFactory(
            authRepository,
            com.skinsense.ai.data.FirestoreUserRepository(),
            com.skinsense.ai.utils.SecurityManager(LocalContext.current)
        )
    )
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val userRepository = remember { com.skinsense.ai.data.FirestoreUserRepository() }
    val notificationRepository = remember { com.skinsense.ai.data.FirestoreNotificationRepository(com.skinsense.ai.utils.NotificationHelper(context)) }
    val chatRepository = remember { com.skinsense.ai.data.FirestoreChatRepository(notificationRepository) }
    // Hoist ChatbotViewModel here so it survives navigation (chat history is preserved)
    val chatbotViewModel: com.skinsense.ai.ui.ChatbotViewModel = viewModel()
    val currentUser by authRepository.currentUser.collectAsState()
    val currentUserId by authRepository.currentUserId.collectAsState()
    val userRole by authViewModel.currentUserRole.collectAsState()
    val isLoggedIn = currentUserId != null
    
    LaunchedEffect(userRole, isLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        Log.d("SkinSense", "Navigation: role=$userRole, loggedIn=$isLoggedIn, current=$currentRoute")
        
        if (isLoggedIn && userRole != null) {
            when (userRole) {
                com.skinsense.ai.data.UserRole.ADMIN ->
                    if (currentRoute != Screen.AdminDashboard.route) {
                        navController.navigate(Screen.AdminDashboard.route) { popUpTo(0) { inclusive = true } }
                    }
                com.skinsense.ai.data.UserRole.DOCTOR ->
                    if (currentRoute != Screen.DoctorDashboard.route) {
                        navController.navigate(Screen.DoctorDashboard.route) { popUpTo(0) { inclusive = true } }
                    }
                com.skinsense.ai.data.UserRole.PATIENT ->
                    // Only auto-redirect to Home if we are ON the Splash or Landing/Login/Signup screens
                    if (currentRoute == Screen.Splash.route || currentRoute == Screen.Landing.route || 
                        currentRoute == Screen.ChooseLanguage.route || currentRoute == Screen.PatientLogin.route ||
                        currentRoute == Screen.PatientSignUp.route) {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                else -> { /* Handle potentially null or future roles */ }
            }
        } else if (!isLoggedIn && currentRoute != Screen.Landing.route && currentRoute != Screen.ChooseLanguage.route && currentRoute != Screen.Splash.route) {
            // If not logged in and not on auth screens, go to Landing
            // (Splash handles the initial decision)
        }
    }
    
    // History ViewModel
    val historyViewModel: com.skinsense.ai.ui.HistoryViewModel = viewModel(
        factory = com.skinsense.ai.ui.HistoryViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            authRepository
        )
    )

    val notificationViewModel: com.skinsense.ai.ui.NotificationViewModel = viewModel(
        factory = com.skinsense.ai.ui.NotificationViewModelFactory(
            authRepository,
            chatRepository
        )
    )
    val totalUnreadCount by notificationViewModel.totalUnreadCount.collectAsState()

    // Appointment ViewModel
    val appointmentViewModel: com.skinsense.ai.ui.AppointmentViewModel = viewModel(
        factory = com.skinsense.ai.ui.AppointmentViewModelFactory(
            com.skinsense.ai.data.FirestoreAppointmentRepository(),
            com.skinsense.ai.data.FirestorePaymentRepository(),
            com.skinsense.ai.data.FirestoreConsultationRepository(notificationRepository),
            notificationRepository
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color.White // changed from AccentBlue to White to show icon clearly
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_branding_logo),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = currentUser ?: stringResource(R.string.clinical_assistant),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (userRole == com.skinsense.ai.data.UserRole.DOCTOR) {
                            Text(
                                text = stringResource(R.string.doctor_portal),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Drawer Items - Patient Only
                if (userRole == com.skinsense.ai.data.UserRole.PATIENT) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_scan)) },
                        icon = { Icon(Icons.Default.CameraAlt, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.PhotoGuidelines.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_library)) },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.DiseaseLibrary.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_history)) },
                        icon = { Icon(Icons.Default.History, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.History.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.find_doctor_title)) },
                        icon = { Icon(Icons.Default.Search, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.FindDoctor.createRoute()) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else if (userRole == com.skinsense.ai.data.UserRole.DOCTOR) {
                    // Doctor specific drawer items
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_dashboard)) },
                        icon = { Icon(Icons.Default.Dashboard, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.DoctorDashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_profile)) },
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                if (userRole == com.skinsense.ai.data.UserRole.ADMIN) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_admin_dashboard)) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.AdminDashboard.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                                
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_sign_out)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut()
                        navController.navigate(Screen.Landing.route) {
                            popUpTo(0) { inclusive = true } // Clear back stack
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Hide TopBar on Login/SignUp/Phone Routes
                val noTopBarRoutes = listOf(
                    Screen.Landing.route, Screen.PatientLogin.route, Screen.DoctorLogin.route,
                    Screen.AdminLogin.route, Screen.PatientSignUp.route, Screen.DoctorSignUp.route
                )
                if (currentDestination?.route !in noTopBarRoutes && 
                    currentDestination?.route?.startsWith("otp") != true) { // Handle param routes
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_branding_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue
                                )
                            }
                        },
                        navigationIcon = {
                             IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_description), tint = TextPrimary)
                            }
                        },
                        actions = {
                            // Profile Icon / Image
                            val userProfile by authViewModel.userProfile.collectAsState()
                            IconButton(onClick = { 
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }) {
                                if (userProfile?.profileImageUri != null) {
                                    val uri = userProfile?.profileImageUri!!
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    
                                    val imageModel = androidx.compose.runtime.remember(uri) {
                                        try {
                                            if (uri.startsWith("data:image")) {
                                                val base64Data = uri.substringAfter("base64,")
                                                android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                            } else {
                                                uri
                                            }
                                        } catch (e: Exception) {
                                            uri
                                        }
                                    }

                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Log.d("SkinSense", "Navigation: Header profileImageUri is null")
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Profile",
                                        tint = TextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MedicalBackground
                        )
                    )
                }
            },
            bottomBar = {
                // Hide BottomBar on Auth Screens OR for Doctors
                val isAuthScreen = currentDestination?.route in listOf(
                    Screen.Landing.route, Screen.PatientLogin.route, Screen.DoctorLogin.route,
                    Screen.AdminLogin.route, Screen.PatientSignUp.route, Screen.DoctorSignUp.route
                )
                
                val isPatient = userRole == com.skinsense.ai.data.UserRole.PATIENT
                
                if (!isAuthScreen && isPatient) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        val items = listOf(
                            BottomNavItem.Home,
                            BottomNavItem.Doctors,
                            BottomNavItem.Scan,
                            BottomNavItem.Chatbot,
                            BottomNavItem.Chats
                        )
                        
                        items.forEachIndexed { index, item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val isFab = index == 2 // Scan item

                            NavigationBarItem(
                                icon = {
                                    if (isFab) {
                                        // Make the center item look like a FAB
                                        Surface(
                                            shape = CircleShape,
                                            color = AccentBlue,
                                            modifier = Modifier.size(56.dp).offset(y = (-8).dp), // Slight lift
                                            shadowElevation = 8.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = stringResource(item.labelRes),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    } else if (item.route == Screen.ChatList.route) {
                                        BadgedBox(
                                            badge = {
                                                if (totalUnreadCount > 0) {
                                                    Badge { Text(text = totalUnreadCount.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = stringResource(item.labelRes))
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = stringResource(item.labelRes))
                                    }
                                },
                                label = { 
                                    if (!isFab) Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall)
                                },
                                selected = isSelected && !isFab, // Don't highlight FAB as "selected" in standard way
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id)
                                        launchSingleTop = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = if(isFab) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Redirection logic consolidated above in a single LaunchedEffect

            // ── Main Content Area ───────────────────────────────────────────
            val finalStartDestination = startDestination ?: remember(isLoggedIn) {
                if (isLoggedIn) Screen.Home.route else Screen.ChooseLanguage.route
            }

            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) Screen.Splash.route else Screen.ChooseLanguage.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Choose Language Screen
                composable(Screen.ChooseLanguage.route) {
                    ChooseLanguageScreen(
                        onLanguageSelected = {
                            navController.navigate(Screen.Landing.route)
                        }
                    )
                }

                // Admin Dashboard
                composable(Screen.AdminDashboard.route) {
                    val adminViewModel: com.skinsense.ai.ui.AdminDashboardViewModel = viewModel(
                        factory = com.skinsense.ai.ui.AdminDashboardViewModelFactory(userRepository)
                    )
                    AdminDashboardScreen(
                        viewModel = adminViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                // Unified Login / Landing Screen
                composable(Screen.Landing.route) {
                    UnifiedLoginScreen(
                        viewModel = authViewModel,
                        onNavigateToSignUp = { 
                            navController.navigate(Screen.PatientSignUp.route) 
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Splash Screen (Dynamic Redirector)
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            // Redirection happens in the main LaunchedEffect based on userRole
                            if (!isLoggedIn) {
                                navController.navigate(Screen.ChooseLanguage.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Patient Login
                composable(Screen.PatientLogin.route) {
                    RoleLoginScreen(
                        role = com.skinsense.ai.data.UserRole.PATIENT,
                        viewModel = authViewModel,
                        onNavigateToSignUp = { navController.navigate(Screen.PatientSignUp.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Doctor Login
                composable(Screen.DoctorLogin.route) {
                    RoleLoginScreen(
                        role = com.skinsense.ai.data.UserRole.DOCTOR,
                        viewModel = authViewModel,
                        onNavigateToSignUp = { navController.navigate(Screen.DoctorSignUp.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Admin Login
                composable(Screen.AdminLogin.route) {
                    RoleLoginScreen(
                        role = com.skinsense.ai.data.UserRole.ADMIN,
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Patient Sign Up
                composable(Screen.PatientSignUp.route) {
                    RoleSignUpScreen(
                        role = com.skinsense.ai.data.UserRole.PATIENT,
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Doctor Sign Up
                composable(Screen.DoctorSignUp.route) {
                    RoleSignUpScreen(
                        role = com.skinsense.ai.data.UserRole.DOCTOR,
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Doctor Dashboard
                composable(Screen.DoctorDashboard.route) {
                    val doctorViewModel: com.skinsense.ai.ui.DoctorDashboardViewModel = viewModel(
                        factory = com.skinsense.ai.ui.DoctorDashboardViewModelFactory(
                            authRepository,
                            com.skinsense.ai.data.FirestoreConsultationRepository(),
                            com.skinsense.ai.data.FirestoreUserRepository(),
                            com.skinsense.ai.data.FirestoreChatRepository(),
                            com.skinsense.ai.data.FirestoreAppointmentRepository()
                        )
                    )
                    DoctorDashboardScreen(
                        viewModel = doctorViewModel,
                        onLogout = {
                            authViewModel.signOut()
                            navController.navigate(Screen.Landing.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToChat = { consultationId ->
                            navController.navigate(Screen.ConsultationChat.createRoute(consultationId))
                        },
                        onChatSelected = { chatId, otherName ->
                            navController.navigate(Screen.Chat.createRoute(chatId, otherName))
                        },
                        onNavigateToPatientDetail = { patientId ->
                            navController.navigate(Screen.PatientDetail.createRoute(patientId))
                        }
                    )
                }

                // Patient Detail Screen
                composable(
                    route = Screen.PatientDetail.route,
                    arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                    val patientDetailViewModel: com.skinsense.ai.ui.PatientDetailViewModel = viewModel(
                        factory = com.skinsense.ai.ui.PatientDetailViewModelFactory(
                            userRepository ?: com.skinsense.ai.data.FirestoreUserRepository(),
                            com.skinsense.ai.data.FirestoreConsultationRepository(),
                            com.skinsense.ai.data.FirestoreChatRepository(),
                            authRepository
                        )
                    )
                    
                    PatientDetailScreen(
                        patientId = patientId,
                        viewModel = patientDetailViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { chatId ->
                            // We need the doctor name to be passed or fetched, but for now let's just use "Chat" 
                            // or rely on chat ID resolution. The target screen needs 'otherName'.
                            // We can fetch user again or pass it. 
                            navController.navigate(Screen.Chat.createRoute(chatId, "Patient")) 
                        }
                    )
                }

                // Find Doctor Screen
                composable(Screen.FindDoctor.route) {
                    val resultJson = it.arguments?.getString("resultJson")
                    val context = LocalContext.current
                    val findDoctorViewModel: com.skinsense.ai.ui.FindDoctorViewModel = viewModel(
                        factory = com.skinsense.ai.ui.FindDoctorViewModelFactory(
                            userRepository ?: com.skinsense.ai.data.FirestoreUserRepository(),
                            com.skinsense.ai.data.FirestoreConsultationRepository(),
                            authRepository
                        )
                    )
                    FindDoctorScreen(
                        resultJson = resultJson,
                        viewModel = findDoctorViewModel,
                        onBack = { navController.popBackStack() },
                        onDoctorClick = { doctor, resJson ->
                            navController.navigate(Screen.DoctorDetail.createRoute(doctor.uid, resJson))
                        },
                        onMessageClick = { doctor ->
                            scope.launch {
                                val patientId = authRepository.currentUserId.value
                                val patientName = currentUser
                                if (patientId != null && patientName != null) {
                                    val result = chatRepository.getOrCreateChat(patientId, doctor.uid, patientName, doctor.displayName)
                                    result.onSuccess { chatId ->
                                        navController.navigate(Screen.Chat.createRoute(chatId, doctor.displayName))
                                    }
                                }
                            }
                        }
                    )
                }

                // My Appointments Screen
                composable(Screen.MyAppointments.route) {
                    MyAppointmentsScreen(
                        appointmentViewModel = appointmentViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Doctor Detail Screen
                composable(
                    route = Screen.DoctorDetail.route,
                    arguments = listOf(
                        navArgument("doctorId") { type = NavType.StringType },
                        navArgument("resultJson") { type = NavType.StringType; nullable = true }
                    )
                ) { backStackEntry ->
                    val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
                    val resultJson = backStackEntry.arguments?.getString("resultJson")
                    val analysisResult = resultJson?.let { json ->
                        com.google.gson.Gson().fromJson(json, com.skinsense.ai.data.AnalysisResult::class.java)
                    }
                    
                    val context = LocalContext.current
                    val findDoctorViewModel: com.skinsense.ai.ui.FindDoctorViewModel = viewModel(
                        factory = com.skinsense.ai.ui.FindDoctorViewModelFactory(
                            userRepository ?: com.skinsense.ai.data.FirestoreUserRepository(),
                            com.skinsense.ai.data.FirestoreConsultationRepository(notificationRepository),
                            authRepository
                        )
                    )
                    
                    DoctorProfileDetailScreen(
                        doctorId = doctorId,
                        analysisResult = analysisResult,
                        viewModel = findDoctorViewModel,
                        appointmentViewModel = appointmentViewModel,
                        onBack = { navController.popBackStack() },
                        onConsultationInitiated = {
                            // After initiating, maybe go back to home or a success screen
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onChatClick = { doctorName ->
                            scope.launch {
                                val patientId = authRepository.currentUserId.value
                                val patientName = currentUser
                                if (patientId != null && patientName != null) {
                                    val result = chatRepository.getOrCreateChat(patientId, doctorId, patientName, doctorName)
                                    result.onSuccess { chatId ->
                                        navController.navigate(Screen.Chat.createRoute(chatId, doctorName))
                                    }
                                }
                            }
                        }
                    )
                }

                // Chatbot Screen (Groq-powered)
                composable(Screen.Chatbot.route) {
                    ChatbotScreen(viewModel = chatbotViewModel)
                }

                // Home Screen
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartAnalysis = { navController.navigate(Screen.PhotoGuidelines.route) },
                        onDiseasesClick = { navController.navigate(Screen.DiseaseLibrary.route) }
                    )
                }

                // My Consultations Screen
                composable(Screen.MyConsultations.route) {
                    val myConsultationsViewModel: com.skinsense.ai.ui.MyConsultationsViewModel = viewModel(
                        factory = com.skinsense.ai.ui.MyConsultationsViewModelFactory(
                            authRepository,
                            com.skinsense.ai.data.FirestoreConsultationRepository(notificationRepository)
                        )
                    )
                    MyConsultationsScreen(
                        viewModel = myConsultationsViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { consultationId ->
                            navController.navigate(Screen.ConsultationChat.createRoute(consultationId))
                        },
                        onNavigateToFindDoctor = {
                            navController.navigate(Screen.FindDoctor.createRoute())
                        }
                    )
                }
                
                // Consultation Chat Screen (Existing - Deprecated or Specific Use)
                composable(
                    route = Screen.ConsultationChat.route,
                    arguments = listOf(navArgument("consultationId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val context = LocalContext.current
                    val application = context.applicationContext as android.app.Application
                    val consultationId = backStackEntry.arguments?.getString("consultationId") ?: ""
                    val chatViewModel: com.skinsense.ai.ui.ConsultationChatViewModel = viewModel(
                        factory = com.skinsense.ai.ui.ConsultationChatViewModelFactory(
                            consultationId,
                            authRepository,
                            com.skinsense.ai.data.FirestoreConsultationRepository()
                        )
                    )
                    val historyViewModel: com.skinsense.ai.ui.HistoryViewModel = viewModel(
                        factory = com.skinsense.ai.ui.HistoryViewModelFactory(
                            application,
                            authRepository
                        )
                    )
                    val currentUid = authRepository.currentUserId.collectAsState().value ?: ""
                    
                    ConsultationChatScreen(
                        currentUserId = currentUid,
                        viewModel = chatViewModel,
                        historyViewModel = historyViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Chat List
                composable(Screen.ChatList.route) {
                    val chatListViewModel: com.skinsense.ai.ui.ChatListViewModel = viewModel(
                        factory = com.skinsense.ai.ui.ChatListViewModelFactory(
                            authRepository,
                            com.skinsense.ai.data.FirestoreChatRepository(),
                            userRepository
                        )
                    )
                    ChatListScreen(
                        viewModel = chatListViewModel,
                        navController = navController,
                        currentUserRole = userRole ?: com.skinsense.ai.data.UserRole.PATIENT
                    )
                }

                // General Chat Screen
                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("otherName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val context = LocalContext.current
                    val application = context.applicationContext as android.app.Application
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val otherName = backStackEntry.arguments?.getString("otherName") ?: "Chat"
                    
                    val chatViewModel: com.skinsense.ai.ui.ChatViewModel = viewModel(
                        factory = com.skinsense.ai.ui.ChatViewModelFactory(
                            chatId,
                            authRepository,
                            com.skinsense.ai.data.FirestoreChatRepository(),
                            userRepository
                        )
                    )
                    
                    val historyViewModel: com.skinsense.ai.ui.HistoryViewModel = viewModel(
                        factory = com.skinsense.ai.ui.HistoryViewModelFactory(
                            application,
                            authRepository
                        )
                    )
                    
                    ChatScreen(
                        viewModel = chatViewModel,
                        navController = navController,
                        otherUserName = otherName,
                        historyViewModel = historyViewModel
                    )
                }

                // Photo Guidelines Screen
                composable(Screen.PhotoGuidelines.route) {
                    PhotoGuidelinesScreen(
                        onContinue = { navController.navigate(Screen.Upload.route) },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Upload Screen
                composable(Screen.Upload.route) {
                    UploadScreen(
                        onImageSelected = { /* Handle image selection */ },
                        onAnalyze = { uri ->
                            viewModel.analyze(uri)
                            navController.navigate(Screen.Results.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Results Screen
                composable(route = Screen.Results.route) {
                    val analysisResult by viewModel.analysisResult.collectAsState()
                    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
                    val error by viewModel.error.collectAsState()
                    
                    if (isAnalyzing) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                            Text("Analyzing...", modifier = Modifier.padding(top = 64.dp))
                        }
                    } else if (error != null) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(error ?: "Error", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                        }
                    } else {
                        analysisResult?.let { result ->
                            ResultsScreen(
                                result = result,
                                onFindSpecialist = {
                                    val resultJson = com.google.gson.Gson().toJson(result)
                                    navController.navigate(Screen.FindDoctor.createRoute(resultJson))
                                },
                                onSave = { viewModel.saveResultAsPdf(navController.context) },
                                onShare = { viewModel.shareResult(navController.context) },
                                onBack = { navController.popBackStack() }
                            )
                        } ?: run {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("No analysis result available.")
                            }
                        }
                    }
                }
                
                // Disease Library Screen
                composable(Screen.DiseaseLibrary.route) {
                    val diseases = remember { DiseaseRepository.getAllDiseases() }
                    DiseaseLibraryScreen(
                        diseases = diseases,
                        onDiseaseClick = { disease ->
                            navController.navigate(Screen.DiseaseDetail.createRoute(disease.id))
                        }
                    )
                }
                
                // Disease Detail Screen
                composable(
                    route = Screen.DiseaseDetail.route,
                    arguments = listOf(navArgument("diseaseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val diseaseId = backStackEntry.arguments?.getString("diseaseId")
                    val disease = remember(diseaseId) { 
                        diseaseId?.let { DiseaseRepository.getDiseaseById(it) }
                    }
                    disease?.let {
                        DiseaseDetailModal(disease = it, onDismiss = { navController.popBackStack() })
                    }
                }
                
                // History Screen
                composable(Screen.History.route) {
                    val historyItems by historyViewModel.historyItems.collectAsState()
                    HistoryScreen(
                        historyItems = historyItems,
                        onItemClick = { item -> navController.navigate(Screen.HistoryDetail.createRoute(item.id)) },
                        onClearHistory = { historyViewModel.clearHistory() },
                        onDelete = { item -> historyViewModel.deleteHistoryItem(item) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // History Detail Screen
                composable(
                    route = Screen.HistoryDetail.route,
                    arguments = listOf(navArgument("historyId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val historyId = backStackEntry.arguments?.getString("historyId")
                    historyId?.let {
                        HistoryDetailScreen(
                            historyId = it,
                            viewModel = historyViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                

                // Profile Screen
                composable(Screen.Profile.route) {
                     val authState by authViewModel.authState.collectAsState()
                     
                     LaunchedEffect(Unit) {
                         authViewModel.refreshProfile()
                     }
                     
                     LaunchedEffect(authState) {
                         if (authState is com.skinsense.ai.ui.AuthState.Idle) {
                             navController.navigate(Screen.Landing.route) {
                                 popUpTo(0) { inclusive = true }
                             }
                         }
                     }
                     
                        ProfileScreen(
                            viewModel = authViewModel,
                            onCompleteProfile = { navController.navigate(Screen.CompleteProfile.route) },
                            onMyAppointmentsClick = { navController.navigate(Screen.MyAppointments.route) }
                        )
                }

                composable(Screen.CompleteProfile.route) {
                    val profileViewModel: com.skinsense.ai.ui.ProfileViewModel = viewModel(
                        factory = com.skinsense.ai.ui.ProfileViewModelFactory(userRepository)
                    )
                    CompleteProfileScreen(
                        uid = authRepository.currentUserId.collectAsState().value ?: "",
                        onComplete = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                        viewModel = profileViewModel
                    )
                }
            }
        }
    }
}
