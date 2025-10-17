package com.astral.unwm.data

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SampleManagerTest {

    private lateinit var context: Context
    private lateinit var sampleManager: SampleManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("astral_samples", Context.MODE_PRIVATE).edit().clear().commit()
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        sampleManager = SampleManager(context)
    }

    @Test
    fun addSample_persistsMetadataAndFile() {
        val bytes = "sample-data".toByteArray()

        val metadata = sampleManager.addSample("  Demo Sample  ", bytes, "JPG")

        assertThat(metadata.name, equalTo("Demo Sample"))
        val stored = File(context.filesDir, metadata.fileName)
        assertThat(stored.exists(), equalTo(true))
        assertThat(sampleManager.getSamples().map { it.id }, hasItem(metadata.id))
    }

    @Test
    fun loadSampleData_returnsDataUrlWithExpectedPayload() {
        val payload = ByteArray(8) { it.toByte() }
        val metadata = sampleManager.addSample("Pixel", payload, "png")

        val dataUrl = sampleManager.loadSampleData(metadata.id)

        requireNotNull(dataUrl)
        val expectedPrefix = "data:image/png;base64,"
        assertThat(dataUrl.startsWith(expectedPrefix), equalTo(true))
        val encoded = dataUrl.removePrefix(expectedPrefix)
        val decoded = Base64.decode(encoded, Base64.DEFAULT)
        assertThat(decoded.toList(), equalTo(payload.toList()))
    }

    @Test
    fun removeSample_deletesMetadataAndFile() {
        val metadata = sampleManager.addSample("Watermark", byteArrayOf(1, 2, 3), "webp")
        val stored = File(context.filesDir, metadata.fileName)
        require(stored.exists())

        sampleManager.removeSample(metadata.id)

        val remainingIds = sampleManager.getSamples().map { it.id }
        assertThat(remainingIds, not(hasItem(metadata.id)))
        assertThat(stored.exists(), equalTo(false))
    }
}
