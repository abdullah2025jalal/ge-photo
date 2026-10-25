package com.abodycode.photomagic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abodycode.photomagic.ui.MainStudioScreen
import com.abodycode.photomagic.ui.auth.AuthApi
import com.abodycode.photomagic.ui.auth.AuthError
import com.abodycode.photomagic.ui.auth.AuthOutcome
import com.abodycode.photomagic.ui.auth.AuthPalette
import com.abodycode.photomagic.ui.auth.AuthStore
import com.abodycode.photomagic.ui.auth.AuthenticatedHeader
import com.abodycode.photomagic.ui.auth.ContactSheet
import com.abodycode.photomagic.ui.auth.CreateAccountScreen
import com.abodycode.photomagic.ui.auth.LoginScreen
import com.abodycode.photomagic.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Root phases of the mandatory login gate. */
private enum class GateScreen { LOADING, LOGIN, CREATE_ACCOUNT, AUTHENTICATED }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoMagicApp()
        }
    }
}

/**
 * PhotoMagic root — the app CANNOT be used without a successful sign-in
 * against `/accounts`, protected exactly like p2p dish:
 *
 *  * Wrong/garbled credentials or a missing account → "Incorrect username
 *    or password" (exponential lockout after 5 bad attempts).
 *  * Suspended account → "الحساب موقوف..." / expired → "انتهت الصلاحية".
 *  * No internet → the offline banner (never the wrong-password banner).
 *  * A saved session is restored optimistically; the 30 s live monitor
 *    re-validates it against the database and signs the user out the
 *    moment the account is deleted / suspended / expired.
 *
 * UI chrome (AR/EN toggle, dark/light toggle, "تواصل معي" sheet) matches
 * p2p dish; the persisted prefs drive the WHOLE app, including direction.
 */
@Composable
private fun PhotoMagicApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var locale by rememberSaveable { mutableStateOf(AuthStore.loadLocale(context)) }
    var darkMode by rememberSaveable { mutableStateOf(AuthStore.loadTheme(context) == "dark") }
    var screen by rememberSaveable { mutableStateOf(GateScreen.LOADING) }
    var session by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(AuthError.NONE) }
    var contactSheetVisible by rememberSaveable { mutableStateOf(false) }

    var monitorJob by remember { mutableStateOf<Job?>(null) }

    fun stopMonitorJob() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun signOut() {
        stopMonitorJob()
        AuthStore.clearSession(context)
        session = null
        error = AuthError.NONE
        screen = GateScreen.LOGIN
    }

    fun startMonitor(name: String) {
        stopMonitorJob()
        monitorJob = scope.launch {
            // Live account monitor (p2p dish _startMonitor): every 30 s the
            // saved session is re-checked; deletion / suspension / expiry
            // ends the session immediately. Offline ticks keep the session.
            while (isActive) {
                delay(30_000)
                when (AuthApi.checkAlive(name)) {
                    false -> {
                        signOut()
                        break
                    }
                    else -> Unit // alive or offline — keep the session
                }
            }
        }
    }

    fun signIn(rawUsername: String, rawPassword: String) {
        if (busy) return
        scope.launch {
            busy = true
            error = AuthError.NONE

            // Brute-force gate: exponential lockout after 5 failures
            // (30 s → 60 s → 120 s… reset only by a successful login).
            val lockRemaining = AuthStore.lockUntilMs(context) - System.currentTimeMillis()
            if (lockRemaining > 0) {
                error = AuthError.INVALID
                busy = false
                return@launch
            }

            val (outcome, _) = AuthApi.verifyCredentials(rawUsername, rawPassword)
            when (outcome) {
                AuthOutcome.SUCCESS -> {
                    AuthStore.resetLockout(context)
                    val key = rawUsername.trim().lowercase()
                    AuthStore.saveSession(context, key)
                    session = key
                    busy = false
                    error = AuthError.NONE
                    screen = GateScreen.AUTHENTICATED
                    startMonitor(key)
                }
                AuthOutcome.INVALID -> {
                    AuthStore.registerFailure(context)
                    error = AuthError.INVALID
                    busy = false
                }
                AuthOutcome.EXPIRED -> {
                    error = AuthError.EXPIRED
                    busy = false
                }
                AuthOutcome.SUSPENDED -> {
                    error = AuthError.SUSPENDED
                    busy = false
                }
                AuthOutcome.OFFLINE -> {
                    error = AuthError.OFFLINE
                    busy = false
                }
                AuthOutcome.SERVER_ERROR -> {
                    error = AuthError.SERVER
                    busy = false
                }
            }
        }
    }

    // Restore the locally saved session once at startup: optimistic (the
    // home screen opens immediately, the monitor re-validates it).
    LaunchedEffect(Unit) {
        val saved = AuthStore.restoreSession(context)
        if (saved != null) {
            session = saved
            screen = GateScreen.AUTHENTICATED
            startMonitor(saved)
        } else {
            screen = GateScreen.LOGIN
        }
    }

    val setLocale: (String) -> Unit = { next ->
        locale = next
        AuthStore.saveLocale(context, next)
    }
    val toggleTheme: () -> Unit = {
        darkMode = !darkMode
        AuthStore.saveTheme(context, if (darkMode) "dark" else "light")
    }

    val direction = if (locale == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    MyApplicationTheme(darkTheme = darkMode) {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    when (screen) {
                        GateScreen.LOADING -> {
                            LoadingView(dark = darkMode)
                        }
                        GateScreen.LOGIN -> {
                            LoginScreen(
                                dark = darkMode,
                                locale = locale,
                                busy = busy,
                                error = error,
                                onLocaleChange = setLocale,
                                onToggleTheme = toggleTheme,
                                onOpenContact = { contactSheetVisible = true },
                                onOpenCreate = {
                                    error = AuthError.NONE
                                    screen = GateScreen.CREATE_ACCOUNT
                                },
                                onSubmit = { rawUsername, rawPassword -> signIn(rawUsername, rawPassword) },
                            )
                        }
                        GateScreen.CREATE_ACCOUNT -> {
                            CreateAccountScreen(
                                dark = darkMode,
                                locale = locale,
                                onBack = { screen = GateScreen.LOGIN },
                            )
                        }
                        GateScreen.AUTHENTICATED -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                            ) {
                                AuthenticatedHeader(
                                    dark = darkMode,
                                    locale = locale,
                                    onLocaleChange = setLocale,
                                    onToggleTheme = toggleTheme,
                                    onOpenContact = { contactSheetVisible = true },
                                    onSignOut = { signOut() },
                                )
                                // The studio content takes the remaining height
                                // below the ABODY CODE header.
                                Box(modifier = Modifier.weight(1f)) {
                                    MainStudioScreen()
                                }
                            }
                        }
                    }

                    if (contactSheetVisible) {
                        ContactSheet(
                            dark = darkMode,
                            locale = locale,
                            onDismiss = { contactSheetVisible = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingView(dark: Boolean) {
    val palette = AuthPalette(dark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = palette.cyan,
            modifier = Modifier.size(34.dp),
        )
    }
}
