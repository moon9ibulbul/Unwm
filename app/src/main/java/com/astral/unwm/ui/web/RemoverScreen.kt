package com.astral.unwm.ui.web

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
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
import java.io.BufferedInputStream

@Composable
fun RemoverScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sampleManager = remember { SampleManager(context) }
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }
    var pendingImport by remember { mutableStateOf(false) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val displayToast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val handleSampleSaved: (SampleManager.SampleMetadata) -> Unit = remember {
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

    val sampleImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (!pendingImport) return@rememberLauncherForActivityResult
        pendingImport = false
        if (uri == null) {
            displayToast("Import dibatalkan.")
            return@rememberLauncherForActivityResult
        }
        val resolver = context.contentResolver
        val name = resolver.queryDisplayName(uri) ?: "Watermark"
        val bytes = resolver.openInputStream(uri)?.use { stream ->
            BufferedInputStream(stream).use { it.readBytes() }
        }
        if (bytes == null) {
            displayToast("Tidak dapat membaca file watermark.")
            return@rememberLauncherForActivityResult
        }
        val extension = guessExtension(name, resolver.getType(uri))
        val cleanedName = name.substringBeforeLast('.')
        val sample = sampleManager.addSample(cleanedName, bytes, extension)
        handleSampleSaved(sample)
        displayToast("Sample '${sample.name}' tersimpan.")
    }

    val multiImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (uris.isNullOrEmpty()) {
            callback?.onReceiveValue(null)
        } else {
            callback?.onReceiveValue(uris.toTypedArray())
        }
    }

    val singleImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
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
        onSampleSaved = handleSampleSaved
        showToast = displayToast
        onImportRequest = {
            pendingImport = true
            sampleImportLauncher.launch(arrayOf("image/*"))
        }
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
                            multiImagePicker.launch(mimeTypes)
                        } else {
                            singleImagePicker.launch(arrayOf(mimeTypes.first()))
                        }
                        return true
                    }
                }
                loadUrl("file:///android_asset/web/unwatermarker.html")
            }
        },
        update = { webView ->
            webView.addJavascriptInterface(samplesInterface, "AstralSamples")
        }
    )
}

private fun ContentResolver.queryDisplayName(uri: Uri): String? {
    return runCatching {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}

private fun guessExtension(name: String?, mime: String?): String {
    val fromName = name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
    return when {
        fromName != null -> fromName
        mime == null -> "png"
        mime.contains("jpeg", ignoreCase = true) -> "jpg"
        mime.contains("png", ignoreCase = true) -> "png"
        mime.contains("webp", ignoreCase = true) -> "webp"
        else -> "png"
    }
}
