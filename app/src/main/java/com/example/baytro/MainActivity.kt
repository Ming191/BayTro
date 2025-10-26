package com.example.baytro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val userRepository: UserRepository by inject()
    private val roleCache: UserRoleCache by inject()
    private val avatarCache: AvatarCache by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        AppNavigation(startDestination = startDestination)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UserRoleState.clearRole()
    }
}