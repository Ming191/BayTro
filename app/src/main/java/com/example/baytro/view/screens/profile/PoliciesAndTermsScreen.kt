package com.example.baytro.view.screens.profile

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.baytro.R

@Composable
fun PoliciesAndTermsScreen() {
    val context = LocalContext.current

    val htmlContent = remember {
        context.resources.openRawResource(R.raw.policies_terms)
            .bufferedReader()
            .use { it.readText() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(it).apply {
                settings.javaScriptEnabled = false
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                isVerticalScrollBarEnabled = true
                overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
            }
        },
        update = { /* no-op */ }
    )
}