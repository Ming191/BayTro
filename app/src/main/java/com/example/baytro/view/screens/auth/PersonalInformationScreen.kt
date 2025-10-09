package com.example.baytro.view.screens.auth

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.data.user.Role
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.IDCardImages
import com.example.baytro.view.components.PersonalInformationCard
import com.example.baytro.viewModel.auth.PersonalInformationVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PersonalInformationScreen(
    viewModel: PersonalInformationVM = koinViewModel(),
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit
) {
    LaunchedEffect(Unit) {
        Log.d("PersonalInformationScreen", "Screen launched - Checking if need to load")
        viewModel.loadPersonalInformation()
    }

    PersonalInformationContent(
        viewModel = viewModel,
        onNavigateToChangePassword = onNavigateToChangePassword,
        onNavigateToSignOut = onNavigateToSignOut,
        onNavigateToEditPersonalInformation = onNavigateToEditPersonalInformation
    )
}

@Composable
fun PersonalInformationContent(
    viewModel: PersonalInformationVM,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit
) {
    val user by viewModel.user.collectAsState()

    Scaffold(
        content = { innerPadding ->
            val context = LocalContext.current
            val density = LocalDensity.current

            val sizePx = with(density) { 250.dp.toPx() }.toInt()

            LazyColumn(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier.Companion.fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        if (user.profileImgUrl != null) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(user.profileImgUrl)
                                    .size(sizePx, sizePx)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(300)
                                    .allowHardware(true)
                                    .build(),
                                contentDescription = "Upload Image success",
                                contentScale = ContentScale.Companion.Crop,
                                modifier = Modifier.Companion
                                    .size(200.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.Companion.size(40.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.BrokenImage,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(150.dp)
                                                    .clip(CircleShape),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                            Text(
                                                "Image unavailable",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Companion.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .size(200.dp, 200.dp)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(150.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = "No Image",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Companion.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                item{
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onNavigateToEditPersonalInformation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Edit")
                        }

                        Spacer(
                            modifier = Modifier.width(16.dp)
                        )

                        Button(
                            onClick = onNavigateToSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Sign Out")
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Button(
                        onClick = onNavigateToChangePassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Change Password")
                    }
                }

                if (user.role is Role.Landlord) {
                    val landlord = user.role as Role.Landlord
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        DividerWithSubhead(
                            subhead = "Personal information",
                            modifier = Modifier
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Full Name:" to user.fullName,
                                "Date of birth:" to user.dateOfBirth,
                                "Gender:" to user.gender.toString(),
                                "Phone Number:" to user.phoneNumber,
                                "Email:" to user.email,
                                "Address:" to user.address
                            )
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        DividerWithSubhead(
                            subhead = "Bank information",
                            modifier = Modifier
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Bank:" to landlord.bankCode,
                                "Account Number:" to landlord.bankAccountNumber
                            )
                        )
                    }
                } else {
                    val tenant = user.role as Role.Tenant
                    item{
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        DividerWithSubhead(
                            subhead = "Personal information",
                            modifier = Modifier
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Full Name:" to user.fullName,
                                "Date of birth:" to user.dateOfBirth,
                                "Gender:" to user.gender.toString(),
                                "Phone Number:" to user.phoneNumber,
                                "Email:" to user.email,
                                "Address:" to user.address,
                                "Occupation:" to tenant.occupation,
                                "IdCardNumber:" to tenant.idCardNumber,
                                "IDCardIssueDate:" to tenant.idCardIssueDate,
                                "EmergencyContact:" to tenant.emergencyContact
                            )
                        )
                    }

                    item{
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        DividerWithSubhead(
                            subhead = "ID card images",
                            modifier = Modifier
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        IDCardImages (
                            idCardFrontImageUrl = tenant.idCardImageFrontUrl,
                            idCardBackImageUrl = tenant.idCardImageBackUrl
                        )
                    }
                }
            }
        }
    )
}