package com.example.baytro.service

import com.example.baytro.BuildConfig
import com.example.baytro.data.MediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File

class FptAiService(private val client: HttpClient, private val mediaRepository: MediaRepository) {

    suspend fun uploadImage(firebaseUrl: String, cacheDir: File): String {
        val tempFile = mediaRepository.getImageFromUrl(firebaseUrl, cacheDir)
        val response: HttpResponse = client.submitFormWithBinaryData(
            url = "https://api.fpt.ai/vision/idr/vnm",
            formData = formData {
                append("image", tempFile.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"${tempFile.name}\"")
                })
            }
        ) {
            headers.append("api-key", BuildConfig.FPT_API_KEY)
        }

        return response.bodyAsText()
    }
}