package com.example.meteomate.ui.screen.wind

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WindMapView(
    modifier: Modifier = Modifier,
    lat: Double = 55.7558,
    lon: Double = 37.6173,
    zoom: Int = 5
) {
    val url = "https://www.openstreetmap.org/export/embed.html?" +
        "bbox=${lon - 10},${lat - 10},${lon + 10},${lat + 10}&" +
        "layer=mapnik&marker=$lat,$lon"

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}
