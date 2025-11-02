package com.example.baytro

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Method
import android.util.Log

object CoverageUtils {
    fun dumpCoverage(context: Context) {
        try {
            val rt = Class.forName("org.jacoco.agent.rt.RT")
            val getAgent = rt.getMethod("getAgent")
            val agent = getAgent.invoke(null)
            val getExecutionData = agent.javaClass.getMethod("getExecutionData", Boolean::class.javaPrimitiveType)
            val data = getExecutionData.invoke(agent, false) as ByteArray

            val file = File(context.filesDir, "coverage.ec")
            file.outputStream().use { it.write(data) }

            android.util.Log.d("COVERAGE", "✅ Dumped coverage: ${file.absolutePath} (${data.size} bytes)")
        } catch (e: Exception) {
            android.util.Log.e("COVERAGE", "❌ Failed to dump coverage", e)
        }
    }
}

