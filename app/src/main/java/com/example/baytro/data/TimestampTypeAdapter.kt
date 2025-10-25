package com.example.baytro.data

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.gitlive.firebase.firestore.Timestamp
import java.io.IOException

class TimestampTypeAdapter : TypeAdapter<Timestamp>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Timestamp?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.beginObject()
            out.name("_seconds").value(value.seconds)
            out.name("_nanoseconds").value(value.nanoseconds)
            out.endObject()
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Timestamp? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            Log.d("TimestampTypeAdapter", "Received NULL timestamp")
            return null
        }

        if (`in`.peek() == JsonToken.STRING) {
            val timestampString = `in`.nextString()
            Log.d("TimestampTypeAdapter", "Received STRING timestamp: $timestampString")
            return try {
                val seconds = timestampString.toLongOrNull()
                if (seconds != null) {
                    Log.d("TimestampTypeAdapter", "Parsed as seconds: $seconds")
                    Timestamp(seconds, 0)
                } else {
                    val rfc1123Formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH)
                    val date = rfc1123Formatter.parse(timestampString)
                    if (date != null) {
                        val epochSeconds = date.time / 1000
                        Log.d("TimestampTypeAdapter", "Parsed as RFC 1123: $epochSeconds")
                        Timestamp(epochSeconds, 0)
                    } else {
                        val instant = java.time.Instant.parse(timestampString)
                        Log.d("TimestampTypeAdapter", "Parsed as ISO instant: ${instant.epochSecond}, ${instant.nano}")
                        Timestamp(instant.epochSecond, instant.nano)
                    }
                }
            } catch (e: Exception) {
                Log.e("TimestampTypeAdapter", "Failed to parse timestamp string: $timestampString", e)
                null
            }
        }

        var seconds: Long = 0
        var nanoseconds = 0

        `in`.beginObject()
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "_seconds", "seconds" -> {
                    seconds = `in`.nextLong()
                    Log.d("TimestampTypeAdapter", "Read seconds: $seconds")
                }
                "_nanoseconds", "nanoseconds" -> {
                    nanoseconds = `in`.nextInt()
                    Log.d("TimestampTypeAdapter", "Read nanoseconds: $nanoseconds")
                }
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()

        Log.d("TimestampTypeAdapter", "Created Timestamp with seconds=$seconds, nanoseconds=$nanoseconds")
        return Timestamp(seconds, nanoseconds)
    }
}


