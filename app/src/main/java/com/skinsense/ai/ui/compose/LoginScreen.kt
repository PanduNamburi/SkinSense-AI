@file:OptIn(ExperimentalMaterial3Api::class)
package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.data.UserRole
import com.skinsense.ai.ui.AuthState
import com.skinsense.ai.ui.AuthViewModel
import com.skinsense.ai.ui.theme.*
import com.skinsense.ai.utils.BiometricHelper
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext

@Composable
fun UnifiedLoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }
    var showPending by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context as FragmentActivity) }
    val showBiometricSetup by viewModel.showBiometricSetupPrompt.collectAsState()

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsState()
    var hasAutoPrompted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.updateBiometricAvailability(biometricHelper.isBiometricAvailable())
    }

    // AUTOMATIC BIOMETRIC PROMPT
    LaunchedEffect(isBiometricEnabled, isBiometricAvailable) {
        if (isBiometricEnabled && isBiometricAvailable && !hasAutoPrompted) {
             android.util.Log.d("SkinSense", "UnifiedLoginScreen: Auto-prompting biometrics")
            biometricHelper.showBiometricPrompt(
                title = context.getString(R.string.login_biometric_title),
                subtitle = context.getString(R.string.login_biometric_subtitle),
                onSuccess = { 
                    android.util.Log.d("SkinSense", "UnifiedLoginScreen: Biometric success")
                    viewModel.loginWithBiometric() 
                },
                onError = { code, err -> 
                    android.util.Log.d("SkinSense", "UnifiedLoginScreen: Biometric error $code: $err")
                },
                onFailed = { 
                    android.util.Log.d("SkinSense", "UnifiedLoginScreen: Biometric failed")
                }
            )
            hasAutoPrompted = true
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.PendingApproval) {
            showPending = true
        }
    }

    if (showPending) {
        PendingApprovalDialog(onDismiss = {
            showPending = false
            viewModel.resetState()
        })
        return
    }

    if (showBiometricSetup) {
        BiometricSetupDialog(
            onEnable = { viewModel.enableBiometric(true) },
            onDismiss = { viewModel.enableBiometric(false) }
        )
    }

    Scaffold(
        containerColor = MedicalBackground,
        bottomBar = {
            SystemStatusBar(modifier = Modifier.padding(horizontal = 24.dp))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            LoginBrandingHeader()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            AccessRoleSelector(
                selectedRole = selectedRole,
                onRoleSelected = { 
                    selectedRole = it
                    viewModel.resetState()
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Animated Form Content
            AnimatedContent(
                targetState = selectedRole,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "loginForm"
            ) { role ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    UnifiedLoginForm(
                        role = role,
                        viewModel = viewModel,
                        biometricHelper = biometricHelper,
                        onNavigateToSignUp = onNavigateToSignUp
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedLoginForm(
    role: UserRole,
    viewModel: AuthViewModel,
    biometricHelper: BiometricHelper,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.login_identifier),
            placeholder = when(role) {
                UserRole.PATIENT -> stringResource(R.string.login_placeholder_patient)
                UserRole.DOCTOR -> stringResource(R.string.login_placeholder_doctor)
                UserRole.ADMIN -> stringResource(R.string.login_placeholder_admin)
            },
            leadingIcon = if (role == UserRole.PATIENT) Icons.Default.Fingerprint else Icons.Default.Badge,
            keyboardType = KeyboardType.Email
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            PremiumTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.login_access_key),
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )
            
            Text(
                text = stringResource(R.string.login_forgot_password),
                color = MedicalSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp)
                    .clickable { /* Handle forgot password */ }
            )
        }
        
        val currentAuthState = authState
        if (currentAuthState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = currentAuthState.message, color = Error, style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PrimaryButton(
            text = stringResource(R.string.login_btn),
            onClick = { viewModel.loginWithRole(email.trim(), password, role) },
            loading = authState is AuthState.Loading,
            modifier = Modifier.fillMaxWidth()
        )

        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
        val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsState()

        if (isBiometricAvailable && isBiometricEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    biometricHelper.showBiometricPrompt(
                        title = context.getString(R.string.login_biometric_title),
                        subtitle = context.getString(R.string.login_biometric_subtitle),
                        onSuccess = { viewModel.loginWithBiometric() },
                        onError = { _, _ -> /* Handle error */ },
                        onFailed = { /* Handle failure */ }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.login_biometric_btn), fontWeight = FontWeight.Bold)
            }
        }
        
        if (role != UserRole.ADMIN) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (role == UserRole.PATIENT) stringResource(R.string.login_new_patient) else stringResource(R.string.login_not_registered),
                    color = TextLight,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.login_request_access),
                    color = MedicalSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }
}

@Composable
fun RoleLoginScreen(
    role: UserRole,
    viewModel: AuthViewModel,
    onNavigateToSignUp: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    // Keep this for legacy or specialized deep links if needed, 
    // but default to the new UnifiedLoginScreen via Navigation.
    val authState by viewModel.authState.collectAsState()
    var showPending by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.PendingApproval) {
            showPending = true
        }
    }

    if (showPending) {
        PendingApprovalDialog(onDismiss = {
            showPending = false
            viewModel.resetState()
        })
        return
    }

    Scaffold(
        containerColor = MedicalBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back), tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Logo & Branding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MedicalSecondary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_branding_logo),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = when(role) {
                        UserRole.PATIENT -> stringResource(R.string.login_title_patient)
                        UserRole.DOCTOR -> stringResource(R.string.login_title_doctor)
                        UserRole.ADMIN -> stringResource(R.string.login_title_admin)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when(role) {
                        UserRole.PATIENT -> stringResource(R.string.login_subtitle_patient)
                        UserRole.DOCTOR -> stringResource(R.string.login_subtitle_doctor)
                        UserRole.ADMIN -> stringResource(R.string.login_subtitle_admin)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Login Content
            when (role) {
                UserRole.PATIENT -> PatientLoginForm(viewModel, onNavigateToSignUp)
                UserRole.DOCTOR -> DoctorLoginForm(viewModel, onNavigateToSignUp)
                UserRole.ADMIN -> AdminLoginForm(viewModel)
            }
        }
    }
}

@Composable
fun PatientLoginForm(viewModel: AuthViewModel, onNavigateToSignUp: (() -> Unit)?) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LoginFormContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        buttonText = stringResource(R.string.login_btn_patient),
        isLoading = authState is AuthState.Loading,
        onLogin = { viewModel.loginWithRole(email.trim(), password, UserRole.PATIENT) },
        error = (authState as? AuthState.Error)?.message,
        onNavigateToSignUp = onNavigateToSignUp
    )
}

@Composable
fun DoctorLoginForm(viewModel: AuthViewModel, onNavigateToSignUp: (() -> Unit)?) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LoginFormContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        buttonText = stringResource(R.string.login_btn_doctor),
        isLoading = authState is AuthState.Loading,
        onLogin = { viewModel.loginWithRole(email.trim(), password, UserRole.DOCTOR) },
        error = (authState as? AuthState.Error)?.message,
        onNavigateToSignUp = onNavigateToSignUp
    )
}

@Composable
fun AdminLoginForm(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LoginFormContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        buttonText = stringResource(R.string.login_btn_admin),
        isLoading = authState is AuthState.Loading,
        onLogin = { viewModel.loginWithRole(email.trim(), password, UserRole.ADMIN) },
        error = (authState as? AuthState.Error)?.message,
        onNavigateToSignUp = null
    )
}

@Composable
fun LoginFormContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    buttonText: String,
    isLoading: Boolean,
    onLogin: () -> Unit,
    error: String?,
    onNavigateToSignUp: (() -> Unit)?
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PremiumTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.common_email),
                placeholder = "name@example.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            PremiumTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.common_password),
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )
            
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            PrimaryButton(
                text = buttonText,
                onClick = onLogin,
                loading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (onNavigateToSignUp != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.login_new_to_skinsense), color = TextLight, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(R.string.login_create_account),
                        color = MedicalSecondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }
            }
        }
    }
}


@Composable
private fun PendingApprovalDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Warning, modifier = Modifier.size(48.dp))
        },
        title = { Text(stringResource(R.string.login_pending_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                stringResource(R.string.login_pending_msg),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.common_understood), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Color.White
    )
}

@Composable
fun BiometricSetupDialog(onEnable: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MedicalSecondary, modifier = Modifier.size(48.dp))
        },
        title = { Text(stringResource(R.string.bio_setup_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                stringResource(R.string.bio_setup_msg),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.bio_setup_enable), onClick = onEnable, modifier = Modifier.fillMaxWidth())
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.bio_setup_later), color = TextLight, fontWeight = FontWeight.Medium)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Color.White
    )
}
