package com.example.baytro.view.screens.tenant

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.auth.RoleFormState
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.view.components.CardComponent
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
        Log.d("TenantInfoScreen", "LaunchedEffect tenant: $formState")
        viewModel.getTenant()
    }

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
                    if (formState?.profileImgUrl != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(formState!!.profileImgUrl)
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
                                //.clickable(onClick = onAvatarClick),
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
                        "Full Name" to formState?.fullName.toString(),
                        "Date of Birth" to formState?.dateOfBirth.toString(),
                        "Gender" to when (formState?.gender) {
                            Gender.MALE -> "Male"
                            Gender.FEMALE -> "Female"
                            Gender.OTHER -> "Other"
                            else -> "Unknown"
                        },
                        "Phone Number" to formState?.phoneNumber.toString(),
                        "Email" to formState?.email.toString(),
                        "Address" to formState?.address.toString()
                    )
                )
            }

            if(formState?.role is Role.Tenant) {
                val roleFormState = formState?.role as Role.Tenant

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