package com.example.baytro.data

import com.google.firebase.firestore.DocumentId
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
enum class Gender {
    MALE, FEMALE, OTHER
}

@Serializable
enum class RoleType {
    TENANT, LANDLORD
}

@Serializable
sealed class Role() {
    @Serializable
    data class Tenant(
        val fullName: String,
        val dateOfBirth: String,
        val gender: Gender,
        val address: String,
        val profileImgUrl: String?,
        val occupation: String,
        val idCardNumber: String,
        val idCardImageFrontUrl: String?,
        val idCardImageBackUrl: String?,
        val idCardIssueDate: String,
        val emergencyContact: String
    ): Role()

    @Serializable
    data class Landlord(
        val fullName: String,
        val dateOfBirth: String,
        val gender: Gender,
        val address: String,
        val bankCode: String,
        val bankAccountNumber: String,
        val profileImgUrl: String?,
    ): Role()
}

@Serializable
data class User (
    @kotlinx.serialization.Transient
    @DocumentId val id: String = "",
    val email: String,
    val phoneNumber: String,
    val role: Role? = null,
)