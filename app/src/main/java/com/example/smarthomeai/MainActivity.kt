package com.example.smarthomeai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthomeai.data.SmartHomeViewModel
import com.example.smarthomeai.face.FaceStore
import com.example.smarthomeai.ui.screens.*
import com.example.smarthomeai.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SmartHomeTheme { AppRoot() }
        }
    }
}

@Composable
fun AppRoot(vm: SmartHomeViewModel = viewModel()) {
    val nav = rememberNavController()
    val state by vm.state.collectAsState()
    val loggedIn by vm.loggedIn.collectAsState()

    val context = LocalContext.current
    val activity = context as ComponentActivity
    LaunchedEffect(Unit) { vm.setRegisteredUsers(FaceStore(context).names()) }

    fun go(route: String) = nav.navigateTab(route)

    NavHost(navController = nav, startDestination = if (loggedIn) "home" else "login") {
        composable("login") {
            LoginScreen(
                demoMode = !vm.firebaseConfigured,
                onSubmit = { email, pass, register, done ->
                    if (register) vm.register(email, pass, done) else vm.login(email, pass, done)
                },
                onGoogle = { done -> vm.loginWithGoogle(activity, done) },
                onSuccess = { nav.navigate("home") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("home") { HomeScreen(state, vm::setDevice, ::go) }
        composable("devices") { DevicesScreen(state, vm::setDevice, { nav.popBackStack() }, ::go) }
        composable("env") { EnvironmentScreen(state, { nav.popBackStack() }, ::go) }
        composable("camera") {
            CameraScreen(
                state = state,
                onRecognized = vm::onFaceRecognized,
                onRegister = { nav.navigate("register") },
                onBack = { nav.popBackStack() },
                onNavigate = ::go
            )
        }
        composable("register") {
            FaceRegisterScreen(
                onBack = { nav.popBackStack() },
                onRegistered = { vm.registerFace(it) }
            )
        }
        composable("alerts") { AlertsScreen(state, { nav.popBackStack() }, ::go) }
        composable("settings") {
            SettingsScreen(
                state = state,
                onIpChange = vm::setEsp32Ip,
                onAutoAlarm = vm::setAutoAlarm,
                onNotify = vm::setNotify,
                onLogout = {
                    vm.logout()
                    nav.navigate("login") { popUpTo(0) }
                },
                onBack = { nav.popBackStack() },
                onNavigate = ::go
            )
        }
    }
}

/** Chuyển tab ở thanh dưới mà không chồng màn hình. */
private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
