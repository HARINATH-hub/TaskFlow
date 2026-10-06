package com.example.taskflow.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.taskflow.R
import com.example.taskflow.data.remote.AuthLogger
import com.example.taskflow.data.remote.AuthState
import com.example.taskflow.ui.TaskUiState
import com.example.taskflow.ui.components.PhoneAuthDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    uiState: TaskUiState,
    onGoogleSignInSuccess: (idToken: String) -> Unit,
    onSendPhoneOtp: (phoneNumber: String, activity: Activity) -> Unit,
    onVerifyPhoneOtp: (otpCode: String) -> Unit,
    onResetPhoneAuth: () -> Unit,
    onSetGoogleLoading: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog visibility states — completely separate and isolated
    var showPhoneAuthDialog by remember { mutableStateOf(false) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }
    var showGoogleSetupDialog by remember { mutableStateOf(false) }

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    val credentialManager = remember { CredentialManager.create(context) }

    // Safely retrieve the Server/Web Client ID from generated resources
    val webClientId = remember(context) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else null
    }

    // Check if google-services.json contains placeholder mock credentials
    val isMockConfig = remember(context) {
        val apiKeyId = context.resources.getIdentifier("google_api_key", "string", context.packageName)
        val apiKey = if (apiKeyId != 0) context.getString(apiKeyId) else ""
        val clientId = webClientId ?: ""
        apiKey.contains("Mock", ignoreCase = true) || clientId.contains("mock", ignoreCase = true)
    }

    // Legacy Google Sign-In Activity Launcher Fallback
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onSetGoogleLoading(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    AuthLogger.success("GoogleAuth", "Legacy GoogleSignInClient returned ID token")
                    onGoogleSignInSuccess(idToken)
                } else {
                    AuthLogger.error("GoogleAuth", "ID token was null in legacy GoogleSignInClient", "Null token")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Google Sign-In returned an empty ID token. Please verify your Web Client ID in Firebase Console."
                        )
                    }
                }
            } catch (e: ApiException) {
                AuthLogger.error("GoogleAuth", "ApiException from GoogleSignIn", "Status code: ${e.statusCode}", e)
                val msg = when (e.statusCode) {
                    10 -> "Google Sign-In configuration required (Status 10): Please add debug SHA-1 fingerprint in Firebase Console."
                    12500 -> "Google Sign-In failed (Status 12500): Please ensure Google Sign-In provider is enabled in Firebase Console."
                    else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            } catch (e: Exception) {
                AuthLogger.error("GoogleAuth", "Exception reading GoogleSignIn account", e.localizedMessage ?: "", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Sign-in error: ${e.localizedMessage}")
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            AuthLogger.stage("GoogleAuth", "Google Sign-In cancelled by user")
        } else {
            AuthLogger.error("GoogleAuth", "Google Sign-In finished with resultCode: ${result.resultCode}", "Result code error")
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Google sign-in was not completed (Result code: ${result.resultCode}).")
            }
        }
    }

    // Function to launch Google Sign-In using Credential Manager with fallback
    // NOTE: This function MUST NEVER touch phone state or phone dialogs!
    fun launchGoogleSignIn() {
        AuthLogger.stage("GoogleAuth", "launchGoogleSignIn requested")

        if (isMockConfig) {
            AuthLogger.error("GoogleAuth", "Mock configuration detected in google-services.json", "Mock Web Client ID")
            showGoogleSetupDialog = true
            return
        }

        if (webClientId.isNullOrBlank()) {
            AuthLogger.error("GoogleAuth", "Missing Web Client ID in resources", "webClientId is null")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Google Sign-In is not configured: Missing Web Client ID in google-services.json."
                )
            }
            return
        }

        coroutineScope.launch {
            onSetGoogleLoading(true)
            try {
                AuthLogger.stage("GoogleAuth", "Requesting credentials via CredentialManager with Web Client ID: $webClientId")
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val targetContext = activity ?: context
                val result = credentialManager.getCredential(
                    request = request,
                    context = targetContext
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    if (idToken.isNotBlank()) {
                        AuthLogger.success("GoogleAuth", "CredentialManager returned Google ID Token successfully")
                        onGoogleSignInSuccess(idToken)
                    } else {
                        onSetGoogleLoading(false)
                        AuthLogger.error("GoogleAuth", "ID token was empty string in credential data", "Empty token")
                        snackbarHostState.showSnackbar("Received empty Google ID Token. Please verify Firebase project configuration.")
                    }
                } else {
                    onSetGoogleLoading(false)
                    AuthLogger.error("GoogleAuth", "Unexpected credential type returned", credential.type)
                    snackbarHostState.showSnackbar("Unexpected credential type received.")
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the Google account picker — reset loading cleanly without any error
                AuthLogger.stage("GoogleAuth", "User dismissed account picker")
                onSetGoogleLoading(false)
            } catch (e: NoCredentialException) {
                AuthLogger.stage("GoogleAuth", "NoCredentialException: attempting fallback to GoogleSignInClient")
                try {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(activity ?: context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                } catch (ex: Exception) {
                    onSetGoogleLoading(false)
                    AuthLogger.error("GoogleAuth", "Fallback to GoogleSignInClient failed", ex.localizedMessage ?: "", ex)
                    snackbarHostState.showSnackbar("Google Sign-In fallback failed: ${ex.localizedMessage}")
                }
            } catch (e: GetCredentialException) {
                AuthLogger.error("GoogleAuth", "GetCredentialException from Play Services: ${e.message}", e.message ?: "", e)
                onSetGoogleLoading(false)
                val errorMsg = when {
                    e.message?.contains("10") == true || e.message?.contains("Developer console") == true ->
                        "Google Sign-In configuration required (Status 10): Please add SHA-1 fingerprint in Firebase Console."
                    e.message?.contains("12500") == true ->
                        "Google Sign-In failed (Status 12500): Please ensure Google provider is enabled in Firebase Console."
                    else -> "Google Sign-In error: ${e.message ?: "Authentication failed"}"
                }
                snackbarHostState.showSnackbar(errorMsg)
            } catch (e: Exception) {
                AuthLogger.error("GoogleAuth", "Unexpected exception during launchGoogleSignIn", e.localizedMessage ?: "", e)
                onSetGoogleLoading(false)
                snackbarHostState.showSnackbar("Google sign-in error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // Show Phone Auth Errors
    LaunchedEffect(uiState.phoneAuthError) {
        uiState.phoneAuthError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Show General Auth Errors
    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Error) {
            snackbarHostState.showSnackbar(uiState.authState.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Emblem
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "TaskFlow Logo",
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Subtitle
            Text(
                text = "TaskFlow",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cloud-Connected Productivity & Mindful Flow",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Setup Needed Warning Banner (Shown ONLY when mock placeholder config is active)
            if (isMockConfig) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSetupGuideDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firebase Project Setup Needed",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Placeholder credentials detected in google-services.json. Tap for setup guide.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Value Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "Cloud Synchronization",
                        description = "Access your tasks, streak, and categories across all your devices."
                    )
                    FeatureRow(
                        icon = Icons.Default.Security,
                        title = "Private & Secure",
                        description = "Strict Cloud Firestore security rules guarantee your data is only accessible by you."
                    )
                    FeatureRow(
                        icon = Icons.Default.Speed,
                        title = "Offline Responsive",
                        description = "Continue working smoothly with local caching even without an internet connection."
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ----------------------------------------------------
            // Sign-In Options (Completely Independent Handlers)
            // ----------------------------------------------------

            // 1. Continue with Google
            // Strictly launches Google authentication ONLY. Never opens phone dialog.
            OutlinedButton(
                onClick = { launchGoogleSignIn() },
                enabled = !uiState.isGoogleAuthLoading && !uiState.isPhoneAuthLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isGoogleAuthLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connecting to Google...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Continue with Phone
            // Launches Phone authentication dialog ONLY.
            Button(
                onClick = {
                    showPhoneAuthDialog = true
                },
                enabled = !uiState.isGoogleAuthLoading && !uiState.isPhoneAuthLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Phone",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Phone OTP Verification Dialog with Country Selector
    if (showPhoneAuthDialog || uiState.isPhoneOtpSent) {
        PhoneAuthDialog(
            isOtpSent = uiState.isPhoneOtpSent,
            isLoading = uiState.isPhoneAuthLoading,
            errorMessage = uiState.phoneAuthError,
            onSendOtp = { phone, act ->
                onSendPhoneOtp(phone, act)
            },
            onVerifyOtp = { otp ->
                onVerifyPhoneOtp(otp)
            },
            onDismiss = {
                showPhoneAuthDialog = false
                onResetPhoneAuth()
            },
            onChangeNumber = {
                onResetPhoneAuth()
            }
        )
    }

    // Google-Specific Setup Dialog (Shown ONLY when Google sign-in is tapped but mock config is active)
    if (showGoogleSetupDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSetupDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Google Sign-In Setup Needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Google Sign-In requires your active Firebase project's google-services.json containing a registered Web Client ID.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Enable 'Google' provider in Firebase Console > Authentication > Sign-in method.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Add your SHA-1 fingerprint:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "EC:DD:C0:AE:52:E3:03:9A:0E:3F:D7:90:69:35:93:B5:F9:22:77:86",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "3. Download the real google-services.json and place it in TaskFlow/app/.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showGoogleSetupDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // General Firebase Setup Guide Dialog (Shown ONLY when the warning banner is tapped)
    if (showSetupGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSetupGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Firebase Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TaskFlow requires your active Firebase project's configuration file to communicate with Google & Phone auth servers.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Package Name: com.example.taskflow",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. SHA-1 Fingerprint:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "EC:DD:C0:AE:52:E3:03:9A:0E:3F:D7:90:69:35:93:B5:F9:22:77:86",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "3. SHA-256 Fingerprint:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "5E:CE:DA:71:E4:56:0E:39:71:CF:50:D4:C6:4A:25:38:9C:9A:8C:AA:CD:CA:07:4F:39:B0:8B:33:AA:AB:25:D9",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "4. Enable 'Google' & 'Phone' in Firebase Console > Authentication > Sign-in method.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "5. Download google-services.json and place into TaskFlow/app/ folder.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSetupGuideDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
