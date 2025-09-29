package com.example.baytro.service

import android.util.Log
import com.example.baytro.BuildConfig
import com.example.baytro.data.FptAiOcrResponse
import com.example.baytro.data.Gender
import com.example.baytro.data.IdCardInfo
import com.example.baytro.data.MediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import java.io.File

class FptAiService(private val client: HttpClient, private val mediaRepository: MediaRepository) {
    companion object {
        private const val TAG = "FptAiService"
        private const val FPT_AI_URL = "https://api.fpt.ai/vision/idr/vnm"
        private const val FORM_FIELD_IMAGE = "image"
        private const val HEADER_VALUE_CONTENT_TYPE = "image/jpeg"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun uploadImage(firebaseUrl: String, cacheDir: File): String {
        Log.d(TAG, "uploadImage: Starting upload to FPT AI with URL: $firebaseUrl")
        var tempFile: File? = null
        try {
            tempFile = mediaRepository.getImageFromUrl(firebaseUrl, cacheDir)
            Log.d(TAG, "uploadImage: Downloaded temp file: ${tempFile.name}, size: ${tempFile.length()} bytes")

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = FPT_AI_URL,
                formData = formData {
                    append(FORM_FIELD_IMAGE, tempFile.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, HEADER_VALUE_CONTENT_TYPE)
                        append(HttpHeaders.ContentDisposition, "filename=\"${tempFile.name}\"")
                    })
                }
            ) {
                headers.append("api-key", BuildConfig.FPT_API_KEY)
            }
            val responseText = response.bodyAsText()
            Log.v(TAG, "uploadImage: FPT AI response: $responseText")
            return responseText
        } catch (e: Exception) {
            Log.e(TAG, "uploadImage: Failed to upload to FPT AI", e)
            throw e
        } finally {
            tempFile?.delete()
            Log.d(TAG, "uploadImage: Deleted temp file")
        }
    }

    suspend fun extractIdCardInfo(firebaseUrl: String, cacheDir: File): Result<IdCardInfo> {
        Log.d(TAG, "extractIdCardInfo: Starting OCR extraction for URL: $firebaseUrl")

        return try {
            val responseText = uploadImage(firebaseUrl, cacheDir)
            Log.d(TAG, "extractIdCardInfo: Parsing OCR response")

            val ocrResponse = json.decodeFromString<FptAiOcrResponse>(responseText)
            Log.d(TAG, "extractIdCardInfo: OCR response parsed - errorCode: ${ocrResponse.errorCode}")

            if (ocrResponse.errorCode != 0) {
                val msg = ocrResponse.errorMessage ?: "OCR failed with code ${ocrResponse.errorCode}"
                Log.w(TAG, msg)
                return Result.failure(Exception(msg))
            } else {
                val data = ocrResponse.data?.firstOrNull()
                if (data == null) {
                    Log.w(TAG, "extractIdCardInfo: No data found in OCR response")

                    return Result.failure(Exception("No ID card data found"))
                }

                Log.d(TAG, "extractIdCardInfo: Processing OCR data - name: ${data.name}, id: ${data.id}")

                val idCardInfo = IdCardInfo(
                    fullName = formatName(data.name?.trim() ?: ""),
                    idCardNumber = data.id?.trim() ?: "",
                    dateOfBirth = formatDate(data.dateOfBirth?.trim() ?: ""),
                    gender = mapGender(data.gender?.trim() ?: ""),
                    permanentAddress = formatAddress(data.home?.trim() ?: ""),
                    idCardIssueDate = formatDate(data.issueDate?.trim() ?: "")
                )

                Log.d(TAG, "extractIdCardInfo: Successfully created IdCardInfo: $idCardInfo")
                Result.success(idCardInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractIdCardInfo: Failed to extract ID card info", e)
            Result.failure(e)
        }
    }


    private fun formatAddress(rawAddress: String): String {
        Log.v(TAG, "formatAddress: Formatting address: '$rawAddress'")
        if (rawAddress.isBlank()) return "".also { Log.v(TAG, "formatAddress: Empty address, returning empty string") }

        return rawAddress.split(Regex("[,;-]"))
            .map { part ->
                part.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { it.titlecase() }
                    }
            }
            .filter { it.isNotEmpty() }
            .joinToString(", ").also { Log.v(TAG, "formatAddress: '$rawAddress' -> '$it'") }
    }

    private fun formatName(rawName: String): String {
        Log.v(TAG, "formatName: Formatting name: '$rawName'")
        if (rawName.isBlank()) return "".also { Log.v(TAG, "formatName: Empty name, returning empty string") }

        return rawName.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.titlecase() }
            }.also { Log.v(TAG, "formatName: '$rawName' -> '$it'") }
    }

    private fun formatDate(raw: String): String =
        raw.filter { it.isDigit() || it == '/' }

    private fun mapGender(rawGender: String) : Gender = when (rawGender.trim().lowercase()) {
        "nam", "male" -> Gender.MALE
        "nữ", "nu", "female" -> Gender.FEMALE
        else -> Gender.OTHER
    }
}