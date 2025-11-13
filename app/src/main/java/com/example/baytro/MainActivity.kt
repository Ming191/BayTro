package com.example.baytro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.user.UserRoleCache
import com.example.baytro.data.user.UserRoleState
import com.example.baytro.navigation.AppNavigation
import com.example.baytro.navigation.Screens
import com.example.baytro.ui.theme.AppTheme
import com.example.baytro.utils.AvatarCache
import com.example.baytro.utils.LocalAvatarCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val userRepository: UserRepository by inject()
    private val roleCache: UserRoleCache by inject()
    private val avatarCache: AvatarCache by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        logFcmToken()

        val currentUser = FirebaseAuth.getInstance().currentUser
        var startDestination: String

        if (currentUser != null) {
            runBlocking {
                val cachedRoleType = roleCache.getRoleType(currentUser.uid)

                if (cachedRoleType != null) {
                    val tempRole = if (cachedRoleType == "Tenant") {
                        Role.Tenant("", "", "", "", "", "") as Role
                    } else {
                        Role.Landlord("", "") as Role
                    }
                    UserRoleState.setRole(tempRole)

                    CoroutineScope(Dispatchers.IO).launch {
                        val user = userRepository.getById(currentUser.uid)
                        if (user?.role == null) {
                            avatarCache.clearCache()
                            roleCache.clearCache()
                            UserRoleState.clearRole()
                            FirebaseAuth.getInstance().signOut()
                        } else {
                            UserRoleState.setRole(user.role)
                        }
                    }
                } else {
                    val user = userRepository.getById(currentUser.uid)
                    if (user?.role == null) {
                        avatarCache.clearCache()
                        roleCache.clearCache()
                        UserRoleState.clearRole()
                        FirebaseAuth.getInstance().signOut()
                    } else {
                        UserRoleState.setRole(user.role)
                        roleCache.setRoleType(currentUser.uid, user.role)
                    }
                }
            }
            startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                Screens.MainScreen.route
            } else {
                Screens.SignIn.route
            }
        } else {
            UserRoleState.clearRole()
            startDestination = Screens.SignIn.route
        }

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalAvatarCache provides avatarCache) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(startDestination = startDestination, onExit = {
                            finish()
                        })
                    }
                }
            }
        }
    }

    private fun logFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("MainActivity", "=== FCM TOKEN INFO ===")
                Log.d("MainActivity", "Current FCM Token: $token")
                Log.d("MainActivity", "Token length: ${token?.length ?: 0}")
                Log.d("MainActivity", "User: ${FirebaseAuth.getInstance().currentUser?.uid ?: "Not logged in"}")
                Log.d("MainActivity", "=====================")
            } else {
                Log.e("MainActivity", "Failed to get FCM token", task.exception)
            }
        }
    }

    private fun requestNotificationPermission() {
        Log.d("MainActivity", "Checking notification permission...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("MainActivity", "Should show permission rationale")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.d("MainActivity", "Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("MainActivity", "Android < 13, notification permission not needed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UserRoleState.clearRole()
    }
}