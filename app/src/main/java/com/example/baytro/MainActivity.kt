package com.example.baytro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.baytro.navigation.AppNavigation
import com.example.baytro.navigation.Screens
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseAuth.getInstance().signOut()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) {
            Screens.MainScreen.route
        } else {
            Screens.SignIn.route
        }
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation(
                    startDestination = startDestination
                )
            }
        }
    }
}

