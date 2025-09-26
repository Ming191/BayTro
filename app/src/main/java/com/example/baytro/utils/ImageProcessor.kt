package com.example.baytro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


object ImageProcessor {
    suspend fun compressImageWithCoil(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        quality: Int = 85
    ): File {
        return withContext(Dispatchers.IO) {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(Size(maxWidth, maxWidth))
                .allowHardware(false)
                .build()

            val result = (imageLoader.execute(request) as SuccessResult).image

            val bitmap: Bitmap = when (result) {
                is BitmapImage -> result.bitmap
                else -> throw IllegalStateException("Unsupported image type: $result")
            }

            val outputFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            outputFile
        }
    }
}