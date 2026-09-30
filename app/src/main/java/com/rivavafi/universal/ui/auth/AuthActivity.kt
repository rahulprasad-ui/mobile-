package com.rivavafi.universal.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.firebase.auth.FirebaseAuth
import com.rivavafi.universal.HomeActivity
import com.rivavafi.universal.R
import com.rivavafi.universal.ui.theme.*
import androidx.compose.ui.draw.shadow
import com.rivavafi.universal.ui.theme.glassMorphism
import com.rivavafi.universal.ui.components.RivavaBrandDisplay
import com.rivavafi.universal.ui.components.RivavaLoadingOverlay
import dagger.hilt.android.AndroidEntryPoint
import android.util.Patterns
import androidx.compose.foundation.shape.CircleShape
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // We now rely on AuthViewModel's init block and LaunchedEffect(authState)
        // to handle the redirection safely after verifying backend status.

        handleIntent(intent)

        setContent {
            RivavaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    AuthScreenContent(
                        viewModel = viewModel,
                        onLoginSuccess = { isNewUser -> goToHome(isNewUser) },
                        onNavigateToReset = { goToResetPassword() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            var mode = data.getQueryParameter("mode")
            var oobCode = data.getQueryParameter("oobCode")
            var token = data.getQueryParameter("token")
            var email = data.getQueryParameter("email")

            // Check if wrapped in dynamic link query parameter (e.g. link=...)
            val deepLink = data.getQueryParameter("link")
            if (!deepLink.isNullOrBlank()) {
                val parsedNested = android.net.Uri.parse(deepLink)
                if (mode.isNullOrBlank()) mode = parsedNested.getQueryParameter("mode")
                if (oobCode.isNullOrBlank()) oobCode = parsedNested.getQueryParameter("oobCode")
                if (token.isNullOrBlank()) token = parsedNested.getQueryParameter("token")
                if (email.isNullOrBlank()) email = parsedNested.getQueryParameter("email")
            }

            if (!token.isNullOrBlank() || !oobCode.isNullOrBlank()) {
                if (!token.isNullOrBlank() || mode == "resetPassword" || data.path?.contains("/reset") == true || data.toString().contains("resetPassword")) {
                    val resetIntent = Intent(this, SetNewPasswordActivity::class.java).apply {
                        putExtra("token", token)
                        putExtra("email", email)
                        putExtra("oobCode", oobCode)
                    }
                    startActivity(resetIntent)
                } else if (mode == "verifyEmail" || data.path?.contains("/verify") == true || data.toString().contains("verifyEmail")) {
                    if (!oobCode.isNullOrBlank()) {
                        viewModel.verifyEmailActionCode(oobCode) {
                            Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun goToHome(isNewUser: Boolean) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("isNewUser", isNewUser)
        }
        startActivity(intent)
        finish()
    }

    private fun goToResetPassword() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(intent)
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun AuthScreenContent(
    viewModel: AuthViewModel,
    onLoginSuccess: (Boolean) -> Unit,
    onNavigateToReset: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()
    val isNewUser by viewModel.isNewUser.collectAsState()
    val context = LocalContext.current

    // Automatically prompt user for all required runtime permissions on app launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission results received */ }

    LaunchedEffect(Unit) {
        val neededPermissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_PERMISSIONS
            )
            val requested = packageInfo.requestedPermissions ?: emptyArray()
            if (requested.contains(android.Manifest.permission.READ_SMS) &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_SMS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.READ_SMS)
            }
            if (requested.contains(android.Manifest.permission.RECEIVE_SMS) &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECEIVE_SMS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.RECEIVE_SMS)
            }
        } catch (e: Exception) {
            // Ignored safely
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    val googleSignInClient = remember(context) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                viewModel.setErrorMessage("Google Sign-in failed: missing ID token. Please confirm the Web client ID in Firebase matches this app.")
            } else {
                viewModel.onGoogleSignInSuccess(
                    idToken = idToken,
                    name = account.displayName ?: "User",
                    email = account.email ?: account.id.orEmpty(),
                    photoUrl = account.photoUrl?.toString() ?: ""
                )
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    "Google Sign-in was cancelled because the account chooser was closed before an account was selected."
                GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                    "Google Sign-in setup error: check the SHA certificate fingerprints and Web client ID in Firebase/Google Cloud."
                CommonStatusCodes.NETWORK_ERROR ->
                    "Google Sign-in failed because the device could not reach Google. Please check your connection and try again."
                else -> "Google Sign-in failed (status ${e.statusCode}): ${e.localizedMessage ?: "Please try again."}"
            }
            viewModel.setErrorMessage(message)
        }
    }

    fun launchGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    BackHandler(enabled = authState == AuthState.LOADING) {
        viewModel.resetState()
    }







    LaunchedEffect(authState) {

        if (authState == AuthState.SUCCESS) {
            onLoginSuccess(isNewUser == true)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (it.isNotEmpty()) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.setErrorMessage("") // Clear after showing
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Moving Image Carousel
            val images = listOf(
                "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=800&q=80"
            )
            val titles = listOf("Track smarter", "Invest better", "Grow faster")

            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { images.size })

            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(3000)
                    val nextPage = (pagerState.currentPage + 1) % images.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }

            val cachedUser = remember { viewModel.getCachedUser() }
            val auth = FirebaseAuth.getInstance()
            val showCachedUser = auth.currentUser == null && cachedUser != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        coil.compose.AsyncImage(
                            model = images[page],
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF0A0A0A)),
                                        startY = 100f
                                    )
                                )
                        )
                        Text(
                            text = titles[page],
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showCachedUser && cachedUser != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (cachedUser.photo != null) {
                        coil.compose.AsyncImage(
                            model = cachedUser.photo,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last logged in as ${cachedUser.name ?: "User"}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
            }

            var startAnimation by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { startAnimation = true }

            // Glassmorphism Login Container
            androidx.compose.animation.AnimatedVisibility(
                visible = startAnimation,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(1000)) +
                        androidx.compose.animation.slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = androidx.compose.animation.core.tween(1000)
                        )
            ) {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    RivavaBrandDisplay(showQuote = true)

                    Spacer(modifier = Modifier.height(16.dp))




                    // Google Login Button
                    Button(
                        onClick = {
                            launchGoogleSignIn()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // Stable height, no shadow to prevent jumpy layout on older APIs
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = authState != AuthState.LOADING
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Google",
                                modifier = Modifier.size(24.dp),
                                tint = AmoledBlack
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Continue with Google", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // Forgot Password Link
                    TextButton(
                        onClick = onNavigateToReset,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = PrimarySky,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                }
                }
            }
        }

        RivavaLoadingOverlay(isLoading = authState == AuthState.LOADING)
    }
}
