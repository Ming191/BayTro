package com.example.baytro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
import kotlin.math.min

object ImageProcessor {
    
    /**
     * Simple compression function without caching
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        quality: Int = 85
    ): File {
        return compressImageInternal(context, uri, maxWidth, quality)
    }
    
    
    /**
     * Internal compression logic
     */
    private suspend fun compressImageInternal(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        quality: Int = 85
    ): File {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            // Try efficient bitmap loading first
            val compressedFile = try {
                compressWithBitmapFactory(context, uri, maxWidth, quality)
            } catch (e: Exception) {
                Log.w("ImageProcessor", "BitmapFactory failed, falling back to Coil", e)
                compressWithCoil(context, uri, maxWidth, quality)
            }
            
            val compressionTime = System.currentTimeMillis() - startTime
            Log.d("ImageProcessor", "Image compressed in ${compressionTime}ms, size: ${compressedFile.length()} bytes")
            
            compressedFile
        }
    }
    
    /**
     * Fast compression using BitmapFactory (more efficient for large images)
     */
    private suspend fun compressWithBitmapFactory(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        quality: Int
    ): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
        
        // Get image dimensions without loading full bitmap
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()
        
        // Calculate sample size for efficient loading
        val sampleSize = calculateInSampleSize(options, maxWidth, maxWidth)
        
        // Load scaled bitmap
        val scaledOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
        }
        
        val inputStream2 = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
        
        val bitmap = BitmapFactory.decodeStream(inputStream2, null, scaledOptions)
            ?: throw IllegalArgumentException("Cannot decode bitmap from URI: $uri")
        inputStream2.close()
        
        // Final resize if needed
        val finalBitmap = if (bitmap.width > maxWidth || bitmap.height > maxWidth) {
            val ratio = min(maxWidth.toFloat() / bitmap.width, maxWidth.toFloat() / bitmap.height)
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            
            val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            if (resized != bitmap) bitmap.recycle()
            resized
        } else {
            bitmap
        }
        
        // Save compressed
        val outputFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
        FileOutputStream(outputFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        finalBitmap.recycle()
        
        outputFile
    }
    
    /**
     * Fallback compression using Coil
     */
    private suspend fun compressWithCoil(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        quality: Int
    ): File = withContext(Dispatchers.IO) {
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
    
    /**
     * Calculate efficient sample size for bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}