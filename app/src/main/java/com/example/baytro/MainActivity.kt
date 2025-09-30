package com.example.baytro

//import com.example.baytro.navigation.ContentType
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.baytro.navigation.AppNavigation
import com.example.baytro.navigation.Screens
import com.example.baytro.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) {
            Screens.ContractDetails.passContractId("vbTH4cDdlhBPKFpRChXC")
        } else {
            Screens.SignIn.route
        }
        setContent {
            AppTheme {
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
}