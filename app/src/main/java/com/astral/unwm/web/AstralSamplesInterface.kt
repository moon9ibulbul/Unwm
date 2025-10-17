package com.astral.unwm.web

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import com.astral.unwm.data.SampleManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class AstralSamplesInterface(
    private val sampleManager: SampleManager,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    var onImportRequest: () -> Unit = {}
    var onSampleSaved: (SampleManager.SampleMetadata) -> Unit = {}
    var showToast: (String) -> Unit = {}

    @JavascriptInterface
    fun getUserSamples(): String {
        val samples = sampleManager.getSamples()
        val array = JSONArray()
        samples.forEach { sample ->
            val obj = JSONObject()
            obj.put("id", sample.id)
            obj.put("name", sample.name)
            array.put(obj)
        }
        return array.toString()
    }

    @JavascriptInterface
    fun loadUserSample(id: String): String? {
        return sampleManager.loadSampleData(id)
    }

    @JavascriptInterface
    fun requestImportSample() {
        mainHandler.post(onImportRequest)
    }

    @JavascriptInterface
    fun saveSample(name: String, dataUrl: String) {
        val (mime, payload) = parseDataUrl(dataUrl)
        if (payload.isEmpty()) {
            mainHandler.post { showToast("Gagal menyimpan watermark baru.") }
            return
        }
        val extension = when (mime.lowercase(Locale.ENGLISH)) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            else -> "png"
        }
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        val sample = sampleManager.addSample(name, bytes, extension)
        mainHandler.post {
            onSampleSaved(sample)
            showToast("Sample '${sample.name}' tersimpan.")
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post { showToast(message) }
    }

    private fun parseDataUrl(dataUrl: String): Pair<String, String> {
        val parts = dataUrl.split(',', limit = 2)
        if (parts.size != 2) return "" to ""
        val header = parts[0]
        val mime = header.substringAfter("data:").substringBefore(';', missingDelimiterValue = "image/png")
        return mime to parts[1]
    }
}
