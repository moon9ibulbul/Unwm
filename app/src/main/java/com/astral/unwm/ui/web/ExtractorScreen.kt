package com.astral.unwm.ui.web

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.astral.unwm.data.SampleManager
import com.astral.unwm.web.AstralSamplesInterface
import org.json.JSONObject

@Composable
fun ExtractorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sampleManager = remember { SampleManager(context) }
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val showToast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val onSampleSaved: (SampleManager.SampleMetadata) -> Unit = remember {
        { sample ->
            val payload = JSONObject()
                .put("id", sample.id)
                .put("name", sample.name)
                .toString()
            val argument = JSONObject.quote(payload)
            webViewHolder.value?.post {
                webViewHolder.value?.evaluateJavascript(
                    "window.AstralSamplesBridge.onSampleImported($argument)",
                    null
                )
            }
        }
    }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (uris.isNullOrEmpty()) {
            callback?.onReceiveValue(null)
        } else {
            callback?.onReceiveValue(uris.toTypedArray())
        }
    }

    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (uri == null) {
            callback?.onReceiveValue(null)
        } else {
            callback?.onReceiveValue(arrayOf(uri))
        }
    }

    val samplesInterface = remember(sampleManager) {
        AstralSamplesInterface(sampleManager)
    }.apply {
        onSampleSaved = onSampleSaved
        showToast = showToast
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder.value?.destroy()
            webViewHolder.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewHolder.value = this
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                addJavascriptInterface(samplesInterface, "AstralSamples")
                webViewClient = object : WebViewClient() {}
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        view: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = filePathCallback
                        val acceptTypes = fileChooserParams.acceptTypes.filter { it.isNotBlank() }
                        val mimeTypes = if (acceptTypes.isEmpty()) arrayOf("image/*") else acceptTypes.toTypedArray()
                        val allowMultiple = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                        if (allowMultiple) {
                            multiPicker.launch(mimeTypes)
                        } else {
                            singlePicker.launch(arrayOf(mimeTypes.first()))
                        }
                        return true
                    }
                }
                loadUrl("file:///android_asset/web/extractor.html")
            }
        },
        update = { webView ->
            webView.addJavascriptInterface(samplesInterface, "AstralSamples")
        }
    )
}
