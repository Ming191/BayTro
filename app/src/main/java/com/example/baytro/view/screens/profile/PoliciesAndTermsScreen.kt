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
fun PoliciesAndTermsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Read HTML from res/raw/policies_terms.html
    val htmlContent = remember {
        context.resources.openRawResource(R.raw.policies_terms).bufferedReader().use { it.readText() }
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = false
            webViewClient = WebViewClient()
            loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { webView }
    )
}
