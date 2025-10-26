package com.example.baytro.service

import android.util.Log
import com.example.baytro.auth.FirebaseAuthRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessagingService : FirebaseMessagingService(), KoinComponent {
    private val authRepository: FirebaseAuthRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w("FCM", "FCM token is null. Cannot send to server.")
            return
        }
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    authRepository.updateFcmToken(token)
                    Log.d("FCM", "FCM token sent to server successfully.")
                } catch (e: Exception) {
                    Log.e("FCM", "Error sending FCM token to server", e)
                }
            }
        } else {
            Log.d("FCM", "No user logged in. Token will be sent on next login.")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        Log.d("FCM", "Message data: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: "Baytro"
        val body = remoteMessage.notification?.body ?: "You have a new notification"
        val contractId = remoteMessage.data["contractId"]
        val screen = remoteMessage.data["screen"]

        NotificationHelper.showNotification(
            context = this,
            title = title,
            body = body,
            contractId = contractId
        )

        if (contractId != null) {
            CoroutineScope(Dispatchers.Default).launch {
                if (screen == "ContractDetails" && title == "New join request") {
                    Log.d("FCM", "Tenant join request received for contract: $contractId")
                    TenantJoinEventBus.emitTenantJoinRequest(contractId)
                } else {
                    TenantJoinEventBus.emitContractConfirmed(contractId)
                }
            }
        }
    }
}
