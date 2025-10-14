package com.example.baytro.service

import android.graphics.Bitmap
import android.util.Log
import com.example.baytro.data.MeterReadingResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

class MeterReadingApiService(private val client: HttpClient) {
    companion object {
        private const val TAG = "MeterReadingApiService"
        private const val API_URL = "https://sunni-unbrief-phyliss.ngrok-free.dev/predict_meter"
        private const val FORM_FIELD_FILE = "file"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    suspend fun predictMeterReading(bitmap: Bitmap): Result<MeterReadingResponse> {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            Log.d(TAG, "predictMeterReading: Image compressed, size: ${imageBytes.size} bytes")

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = API_URL,
                formData = formData {
                    append(FORM_FIELD_FILE, imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"meter.jpg\"")
                    })
                }
            )

            val responseText = response.bodyAsText()
            val meterResponse = json.decodeFromString<MeterReadingResponse>(responseText)
            Result.success(meterResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
