package com.example.baytro.data

import com.example.baytro.data.user.Gender
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FptAiOcrResponse(
    @SerialName("errorCode")
    val errorCode: Int? = null,
    @SerialName("errorMessage")
    val errorMessage: String? = null,
    @SerialName("data")
    val data: List<FptAiOcrData>? = null
)

@Serializable
data class FptAiOcrData(
    @SerialName("id")
    val id: String? = null,
    @SerialName("id_prob")
    val idProb: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("name_prob")
    val nameProb: String? = null,
    @SerialName("dob")
    val dateOfBirth: String? = null,
    @SerialName("dob_prob")
    val dobProb: String? = null,
    @SerialName("sex")
    val gender: String? = null,
    @SerialName("sex_prob")
    val genderProb: String? = null,
    @SerialName("nationality")
    val nationality: String? = null,
    @SerialName("nationality_prob")
    val nationalityProb: String? = null,
    @SerialName("home")
    val home: String? = null,
    @SerialName("home_prob")
    val homeProb: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("address_prob")
    val addressProb: String? = null,
    @SerialName("address_entities")
    val addressEntities: AddressEntities? = null,
    @SerialName("doe")
    val dateOfExpiry: String? = null,
    @SerialName("doe_prob")
    val doeProb: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("type_new")
    val typeNew: String? = null,
    // Back side fields
    @SerialName("features")
    val features: String? = null,
    @SerialName("features_prob")
    val featuresProb: String? = null,
    @SerialName("issue_date")
    val issueDate: String? = null,
    @SerialName("issue_date_prob")
    val issueDateProb: String? = null,
    @SerialName("religion")
    val religion: String? = null,
    @SerialName("religion_prob")
    val religionProb: String? = null,
    @SerialName("ethnicity")
    val ethnicity: String? = null,
    @SerialName("ethnicity_prob")
    val ethnicityProb: String? = null,
    @SerialName("issue_loc")
    val issueLoc: String? = null,
    @SerialName("issue_loc_prob")
    val issueLocProb: String? = null
)

@Serializable
data class AddressEntities(
    @SerialName("province")
    val province: String? = null,
    @SerialName("district")
    val district: String? = null,
    @SerialName("ward")
    val ward: String? = null,
    @SerialName("street")
    val street: String? = null
)

data class IdCardInfo(
    val fullName: String,
    val idCardNumber: String,
    val dateOfBirth: String,
    val gender: Gender,
    val permanentAddress: String,
    val idCardIssueDate: String
)

data class IdCardInfoWithImages(
    val idCardInfo: IdCardInfo,
    val frontImageUrl: String?,
    val backImageUrl: String?
)
