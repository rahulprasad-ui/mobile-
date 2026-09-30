package com.rivavafi.universal

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.theme.glassMorphism
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.ui.analytics.AnalyticsScreen
import com.rivavafi.universal.ui.history.TransactionsScreen
import com.rivavafi.universal.ui.home.HomeScreen
import com.rivavafi.universal.ui.onboarding.GreetingScreen
import com.rivavafi.universal.ui.onboarding.PhoneInputScreen
import com.rivavafi.universal.ui.onboarding.WelcomeScreen
import com.rivavafi.universal.ui.settings.SettingsScreen
import com.rivavafi.universal.ui.portfolio.RivavaPortfolioScreen
import com.rivavafi.universal.ui.profile.ProfileScreen
import com.rivavafi.universal.ui.theme.RivavaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.layout.fillMaxSize
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Auth : Screen("auth", "Auth", Icons.Outlined.Home)
    object Welcome : Screen("welcome", "Welcome", Icons.Outlined.Home)
    object Greeting : Screen("greeting", "Greeting", Icons.Outlined.Home)
    object PhoneInput : Screen("phone_input", "Phone Input", Icons.Outlined.Home)
    object OtpVerification : Screen("otp_verification", "OTP Verification", Icons.Outlined.Home)
    object SmsOptIn : Screen("sms_opt_in", "SmsOptIn", Icons.Outlined.Home)
    object SmsConsent : Screen("sms_consent", "SmsConsent", Icons.Outlined.Home)
    object Scanning : Screen("scanning", "Scanning", Icons.Outlined.Home)
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Transactions : Screen("transactions", "History", Icons.AutoMirrored.Outlined.ListAlt)
    object Analytics : Screen("analytics", "Insights", Icons.Outlined.Analytics)
    object AiReview : Screen("ai_review", "AI Review", Icons.Outlined.AutoAwesome)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
    object Profile : Screen("profile", "Profile", Icons.Outlined.Person)
    object RivavaPortfolio : Screen("portfolio_screen", "Rivava Portfolio", Icons.Outlined.AccountBalanceWallet)
    object HelpCenter : Screen("help_center", "Help Center", Icons.Outlined.Info)
    object StockDetail : Screen("stock_detail", "Stock Detail", Icons.Outlined.AccountBalanceWallet)
    object TransactionDetail : Screen("transaction_detail", "Transaction Detail", Icons.AutoMirrored.Outlined.ListAlt)
    object Calculators : Screen("calculators", "Tools", Icons.Outlined.Calculate)
    object VerifyEmail : Screen("verify_email", "Verify Email", Icons.Outlined.Home)
    object ResetPassword : Screen("reset_password", "Reset Password", Icons.Outlined.Home)
}

val BaseBottomNavigationItems = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Calculators,
    Screen.RivavaPortfolio,
    Screen.Profile
)

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                val isNewUser = intent.getBooleanExtra("isNewUser", false)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isPremiumUser = preferencesRepository.isPremiumUserFlow.collectAsState(initial = false).value
            val hasCompletedOnboardingState = preferencesRepository.hasCompletedOnboardingFlow.collectAsState(initial = null)
            val hasCompletedOnboarding = hasCompletedOnboardingState.value

            RivavaTheme(isPremium = isPremiumUser) {
                if (hasCompletedOnboarding != null) {
                    RivavaAppContent(hasCompletedOnboarding, preferencesRepository, isNewUser)
                } else {
                    // Show a minimal loading state or nothing while reading preferences
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun RivavaAppContent(hasCompletedOnboarding: Boolean, preferencesRepository: UserPreferencesRepository? = null, isNewUser: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSecureRoute = currentRoute?.startsWith(Screen.RivavaPortfolio.route) == true ||
            currentRoute?.startsWith(Screen.StockDetail.route) == true ||
            currentRoute?.startsWith("pdf_viewer") == true

    val localCtx = androidx.compose.ui.platform.LocalContext.current
    val activity = localCtx as? android.app.Activity ?: (localCtx as? android.content.ContextWrapper)?.baseContext as? android.app.Activity

    LaunchedEffect(isSecureRoute) {
        if (isSecureRoute) {
            activity?.window?.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }


    val isPremiumUser = preferencesRepository?.isPremiumUserFlow?.collectAsState(initial = false)?.value ?: false

    // Request any missing permissions on Home screen launch
    val homePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled */ }

    LaunchedEffect(Unit) {
        val neededPermissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    localCtx,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        try {
            val packageInfo = localCtx.packageManager.getPackageInfo(
                localCtx.packageName,
                android.content.pm.PackageManager.GET_PERMISSIONS
            )
            val requested = packageInfo.requestedPermissions ?: emptyArray()
            if (requested.contains(android.Manifest.permission.READ_SMS) &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    localCtx,
                    android.Manifest.permission.READ_SMS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.READ_SMS)
            }
            if (requested.contains(android.Manifest.permission.RECEIVE_SMS) &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    localCtx,
                    android.Manifest.permission.RECEIVE_SMS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.RECEIVE_SMS)
            }
        } catch (e: Exception) {
            // Ignored safely
        }

        if (neededPermissions.isNotEmpty()) {
            homePermissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    val bottomNavigationItems = listOf(Screen.Home, Screen.Transactions, Screen.Calculators, Screen.RivavaPortfolio, Screen.Analytics, Screen.Profile)

    val isBottomBarVisible = currentRoute in bottomNavigationItems.map { it.route }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", android.content.Context.MODE_PRIVATE)
    val isPortfolioUnlocked = isPremiumUser || prefs.getBoolean("portfolio_unlocked", false)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isBottomBarVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF161616).copy(alpha = 0.85f))
                            .glassMorphism(cornerRadius = 999f, alpha = 0.05f, strokeAlpha = 0.05f)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavigationItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val isLocked = screen.route == Screen.RivavaPortfolio.route && !isPortfolioUnlocked

                            CustomBottomNavItem(
                                screen = screen,
                                isSelected = isSelected,
                                isLocked = isLocked,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFF0A0A0A)
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = if (hasCompletedOnboarding || !isNewUser) Screen.Home.route else Screen.Welcome.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.95f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                androidx.compose.animation.scaleOut(
                    targetScale = 1.05f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 1.05f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                androidx.compose.animation.scaleOut(
                    targetScale = 0.95f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            }
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(onNavigateNext = {
                    navController.navigate(Screen.PhoneInput.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.PhoneInput.route) {
                PhoneInputScreen(onNavigateNext = {
                    if (BuildConfig.FLAVOR == "play") {
                        navController.navigate(Screen.Greeting.route) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.SmsConsent.route) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    }
                })
            }

            composable(Screen.SmsConsent.route) {
                com.rivavafi.universal.ui.onboarding.SmsConsentScreen(onNavigateNext = {
                    navController.navigate(Screen.Greeting.route) {
                        popUpTo(Screen.SmsConsent.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Greeting.route) {
                GreetingScreen(onNavigateNext = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Greeting.route) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToRivavaPortfolio = {
                        navController.navigate(Screen.RivavaPortfolio.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTransactionDetail = { transactionId ->
                        navController.navigate("${Screen.TransactionDetail.route}/$transactionId")
                    },
                    onNavigateToCalculators = {
                        navController.navigate(Screen.Calculators.route)
                    }
                )
            }
            composable(Screen.Profile.route) {
                com.rivavafi.universal.ui.profile.ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHelpCenter = { navController.navigate(Screen.HelpCenter.route) }
                )
            }
            composable(Screen.HelpCenter.route) {
                com.rivavafi.universal.ui.help.HelpCenterScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onNavigateToDetail = { transactionId ->
                        navController.navigate("${Screen.TransactionDetail.route}/$transactionId")
                    }
                )
            }
            composable(
                route = "${Screen.TransactionDetail.route}/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                com.rivavafi.universal.ui.history.TransactionDetailScreen(
                    transactionId = transactionId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AiReview.route) {
                com.rivavafi.universal.ui.aireview.AiReviewScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Calculators.route) {
                com.rivavafi.universal.ui.calculator.CalculatorsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onRestartApp = {
                    val intent = android.content.Intent(context, com.rivavafi.universal.ui.auth.AuthActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                })
            }
            composable(Screen.RivavaPortfolio.route) {
                RivavaPortfolioScreen(onBack = { navController.popBackStack() }, onNavigateToDetail = { ticker, focus ->
                    val focusParam = focus ?: "none"
                    navController.navigate("${Screen.StockDetail.route}/$ticker?focus=$focusParam")
                })
            }
            composable(
                route = "${Screen.StockDetail.route}/{ticker}?focus={focus}",
                arguments = listOf(
                    navArgument("ticker") { type = NavType.StringType },
                    navArgument("focus") {
                        type = NavType.StringType
                        defaultValue = "none"
                    }
                )
            ) { backStackEntry ->
                val ticker = backStackEntry.arguments?.getString("ticker") ?: ""
                com.rivavafi.universal.ui.portfolio.StockPortfolioDetailScreen(
                    ticker = ticker,
                    onBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { assetName ->
                        navController.navigate("pdf_viewer/$assetName")
                    }
                )
            }
            composable(
                "pdf_viewer/{assetName}",
                arguments = listOf(navArgument("assetName") { type = NavType.StringType })
            ) { backStackEntry ->
                val assetName = backStackEntry.arguments?.getString("assetName") ?: "portfolio_ireda.pdf"
                com.rivavafi.universal.ui.portfolio.PdfViewerScreen(
                    assetName = assetName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val iconColor by animateColorAsState(
        targetValue = if (isLocked && !isSelected) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        } else if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
                if (isLocked) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = com.rivavafi.universal.ui.theme.SecondaryPink,
                        modifier = Modifier
                            .size(12.dp)
                            .offset(x = 6.dp, y = (-2).dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .padding(1.dp)
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
