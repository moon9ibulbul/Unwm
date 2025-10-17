package com.astral.unwm.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SampleManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getSamples(): List<SampleMetadata> {
        val json = preferences.getString(KEY_SAMPLES, "[]") ?: "[]"
        val array = JSONArray(json)
        val result = mutableListOf<SampleMetadata>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                SampleMetadata(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    fileName = obj.getString("fileName")
                )
            )
        }
        return result
    }

    fun addSample(name: String, data: ByteArray, extension: String = "png"): SampleMetadata {
        val sanitizedName = name.trim().ifBlank { "Sample ${System.currentTimeMillis()}" }
        val id = "user:${UUID.randomUUID()}"
        val ext = extension.lowercase().takeIf { it in allowedExtensions } ?: "png"
        val fileName = "$id.$ext"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { stream ->
            stream.write(data)
        }
        val sample = SampleMetadata(id = id, name = sanitizedName, fileName = fileName)
        val samples = getSamples().toMutableList()
        samples.add(sample)
        saveSamples(samples)
        return sample
    }

    fun loadSampleData(id: String): String? {
        val sample = getSamples().firstOrNull { it.id == id } ?: return null
        val file = File(context.filesDir, sample.fileName)
        if (!file.exists()) return null
        val data = file.readBytes()
        val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
        val mime = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        return "data:$mime;base64,$base64"
    }

    fun removeSample(id: String) {
        val samples = getSamples().toMutableList()
        val iterator = samples.iterator()
        var removed: SampleMetadata? = null
        while (iterator.hasNext()) {
            val sample = iterator.next()
            if (sample.id == id) {
                removed = sample
                iterator.remove()
                break
            }
        }
        removed?.let {
            val file = File(context.filesDir, it.fileName)
            if (file.exists()) {
                file.delete()
            }
        }
        saveSamples(samples)
    }

    private fun saveSamples(samples: List<SampleMetadata>) {
        val array = JSONArray()
        samples.forEach { sample ->
            val obj = JSONObject()
            obj.put("id", sample.id)
            obj.put("name", sample.name)
            obj.put("fileName", sample.fileName)
            array.put(obj)
        }
        preferences.edit().putString(KEY_SAMPLES, array.toString()).apply()
    }

    data class SampleMetadata(
        val id: String,
        val name: String,
        val fileName: String
    )

    companion object {
        private const val PREF_NAME = "astral_samples"
        private const val KEY_SAMPLES = "samples"
        private val allowedExtensions = setOf("png", "jpg", "jpeg", "webp")
    }
}
