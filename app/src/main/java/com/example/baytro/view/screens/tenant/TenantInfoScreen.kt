package com.example.baytro.view.screens.tenant

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.IDCardImages
import com.example.baytro.view.components.PersonalInformationCard
import com.example.baytro.viewModel.tenant.TenantInfoVM
import org.koin.androidx.compose.koinViewModel

@Composable
fun TenantInfoScreen(
    viewModel: TenantInfoVM = koinViewModel()
) {
    val formState by viewModel.tenant.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { 200.dp.toPx() }.toInt()

    LaunchedEffect(Unit) {
        viewModel.getTenant()
    }

    Crossfade(
        targetState = formState,
        animationSpec = tween(durationMillis = 400), // 👈 tốc độ mượt
        label = "tenantInfoCrossfade"
    ) { tenantInfo ->
        if (tenantInfo == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (tenantInfo.profileImgUrl != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(tenantInfo.profileImgUrl)
                                        .size(sizePx, sizePx)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(300)
                                        .allowHardware(true)
                                        .build(),
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.BrokenImage,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        DividerWithSubhead(subhead = "Basic Information")
                        Spacer(modifier = Modifier.height(8.dp))

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Full Name" to tenantInfo.fullName,
                                "Date of Birth" to tenantInfo.dateOfBirth,
                                "Gender" to when (tenantInfo.gender) {
                                    Gender.MALE -> "Male"
                                    Gender.FEMALE -> "Female"
                                    Gender.OTHER -> "Other"
                                    else -> "Unknown"
                                },
                                "Phone Number" to tenantInfo.phoneNumber,
                                "Email" to tenantInfo.email,
                                "Address" to tenantInfo.address
                            )
                        )
                    }

                    if (tenantInfo.role is Role.Tenant) {
                        val roleFormState = tenantInfo.role

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            DividerWithSubhead(subhead = "Additional Information")
                            Spacer(modifier = Modifier.height(8.dp))

                            PersonalInformationCard(
                                infoMap = mapOf(
                                    "Occupation" to roleFormState.occupation,
                                    "ID Card Number" to roleFormState.idCardNumber,
                                    "ID Card Issue Date" to roleFormState.idCardIssueDate,
                                    "Emergency Contact" to roleFormState.emergencyContact
                                )
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            DividerWithSubhead(subhead = "ID Card Images")
                            Spacer(modifier = Modifier.height(8.dp))

                            IDCardImages(
                                idCardFrontImageUrl = roleFormState.idCardImageFrontUrl,
                                idCardBackImageUrl = roleFormState.idCardImageBackUrl
                            )
                        }
                    }
                }
            }
        }
    }
}
