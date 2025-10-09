package com.example.baytro.view.screens.auth

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.data.user.User
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.BankCode
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.IDCardImages
import com.example.baytro.view.components.PersonalInformationCard
import com.example.baytro.viewModel.auth.PersonalInformationVM

@Preview
@Composable
fun PersonalInformationPreview(

) {
//    PersonalInformationContent(
//        user = User(
//            fullName = "Landlord lor",
//            dateOfBirth = "19/11/1990",
//            gender = Gender.MALE,
//            phoneNumber = "0123456789",
//            email = "blah",
//            address = "Gia Lam, Ha Noi",
//            profileImgUrl = "https://firebasestorage.googleapis.com/v0/b/baytro-473008.firebasestorage.app/o/users%2FM8Dx0vr4Lvc57gDHU9LXRdGeDGh2%2Fprofile.jpg?alt=media&token=b922af15-67e5-4a11-80d4-5ae2375e02d3",
//            role =  Role.Landlord(
//                bankCode = "MB",
//                bankAccountNumber = "0123456789"
//            )
//        ),
//        onEditClick = {},
//        onChangePasswordClick = {},
//        onSignOutClick = {}
//    )

    PersonalInformationContent(
        user = User(
            fullName = "Tenant lor",
            dateOfBirth = "19/11/1990",
            gender = Gender.MALE,
            phoneNumber = "0123456789",
            email = "blah",
            address = "Gia Lam, Ha Noi",
            profileImgUrl = "https://firebasestorage.googleapis.com/v0/b/baytro-473008.firebasestorage.app/o/users%2FM8Dx0vr4Lvc57gDHU9LXRdGeDGh2%2Fprofile.jpg?alt=media&token=b922af15-67e5-4a11-80d4-5ae2375e02d3",
            role =  Role.Tenant(
                occupation = "bro",
                idCardNumber = "001002003004",
                idCardImageFrontUrl = null,
                idCardImageBackUrl = null,
                idCardIssueDate = "01/01/2025",
                emergencyContact = "0123456789"
            )
        ),
        onEditClick = {},
        onChangePasswordClick = {},
        onSignOutClick = {}
    )
}

@Composable
fun PersonalInformationScreen(

) {
}

@Composable
fun PersonalInformationContent(
    user: User,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Scaffold(
        content = { innerPadding ->
            val context = LocalContext.current
            val density = LocalDensity.current

            val sizePx = with(density) { 150.dp.toPx() }.toInt()

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
                                    .size(120.dp)
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
                                            .height(150.dp),
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
                                                    .size(48.dp)
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
                                        .size(150.dp, 150.dp)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
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
                            onClick = onEditClick,
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
                            onClick = onSignOutClick,
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
                        onClick = onChangePasswordClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Change Password")
                    }
                }

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

                    if(user.role is Role.Landlord) {
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
                    } else if (user.role is Role.Tenant){
                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Full Name:" to user.fullName,
                                "Date of birth:" to user.dateOfBirth,
                                "Gender:" to user.gender.toString(),
                                "Phone Number:" to user.phoneNumber,
                                "Email:" to user.email,
                                "Address:" to user.address,
                                "Occupation:" to user.role.occupation,
                                "IdCardNumber:" to user.role.idCardNumber,
                                "IDCardIssueDate:" to user.role.idCardIssueDate,
                                "EmergencyContact:" to user.role.emergencyContact,
                            )
                        )
                    }
                }

                item{
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    if(user.role is Role.Landlord) {
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
                                "Bank:" to user.role.bankCode,
                                "Account Number:" to user.role.bankAccountNumber
                            )
                        )
                    } else if(user.role is Role.Tenant) {
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
                            idCardFrontImageUrl = user.role.idCardImageFrontUrl,
                            idCardBackImageUrl = user.role.idCardImageBackUrl,
                        )
                    }
                }
            }
        }
    )
}