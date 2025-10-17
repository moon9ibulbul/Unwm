package com.astral.unwm.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.astral.unwm.data.SampleManager
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONArray
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class AstralSamplesInterfaceTest {

    private lateinit var context: Context
    private lateinit var sampleManager: SampleManager
    private lateinit var interfaceUnderTest: AstralSamplesInterface

    private var lastToast: String? = null
    private var lastSavedId: String? = null
    private var importRequested = false

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("astral_samples", Context.MODE_PRIVATE).edit().clear().commit()
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        lastToast = null
        lastSavedId = null
        importRequested = false
        sampleManager = SampleManager(context)
        interfaceUnderTest = AstralSamplesInterface(sampleManager, Handler(Looper.getMainLooper())).apply {
            onImportRequest = { importRequested = true }
            onSampleSaved = { lastSavedId = it.id }
            showToast = { lastToast = it }
        }
    }

    @Test
    fun getUserSamples_reflectsCurrentState() {
        val first = sampleManager.addSample("Sample A", byteArrayOf(1), "png")
        val second = sampleManager.addSample("Sample B", byteArrayOf(2), "png")

        val json = interfaceUnderTest.getUserSamples()

        val array = JSONArray(json)
        assertThat(array.length(), equalTo(2))
        val ids = (0 until array.length()).map { array.getJSONObject(it).getString("id") }
        assertThat(ids.contains(first.id) && ids.contains(second.id), equalTo(true))
    }

    @Test
    fun requestImportSample_invokesCallbackOnMainThread() {
        interfaceUnderTest.requestImportSample()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(importRequested, equalTo(true))
    }

    @Test
    fun saveSample_persistsDataAndSignalsUi() {
        val payload = "hello".toByteArray()
        val dataUrl = "data:image/png;base64," + android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP)

        interfaceUnderTest.saveSample("From Web", dataUrl)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val samples = sampleManager.getSamples()
        assertThat(samples.size, equalTo(1))
        val metadata = samples.first()
        assertThat(metadata.name, equalTo("From Web"))
        assertThat(lastSavedId, equalTo(metadata.id))
        assertThat(lastToast, notNullValue())
        val stored = sampleManager.loadSampleData(metadata.id)
        assertThat(stored, notNullValue())
    }
}
