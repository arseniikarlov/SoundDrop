package com.alfa.shakegroan.data

import android.content.Context
import com.alfa.shakegroan.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

class AppMetricsTransport(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    fun sendEvent(
        name: String,
        params: Map<String, Any?> = emptyMap(),
        timestampMs: Long = System.currentTimeMillis(),
    ) {
        val endpoint = BuildConfig.FALL_OUCH_METRICS_ENDPOINT
        val writeKey = BuildConfig.FALL_OUCH_METRICS_WRITE_KEY
        if (endpoint.isBlank() || writeKey.isBlank()) {
            return
        }

        val payload = JSONObject()
            .put("app_instance_id", appInstanceId())
            .put("platform", "android")
            .put("app_version", BuildConfig.VERSION_NAME)
            .put(
                "events",
                JSONArray().put(
                    JSONObject()
                        .put("name", name)
                        .put("timestamp_ms", timestampMs)
                        .put("params", JSONObject(params.filterValues { it != null }))
                )
            )
            .toString()

        executor.execute {
            runCatching {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("X-Fall-Ouch-Write-Key", writeKey)
                }
                connection.outputStream.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                }
                connection.inputStream.use { it.readBytes() }
                connection.disconnect()
            }
        }
    }

    private fun appInstanceId(): String {
        preferences.getString(KEY_APP_INSTANCE_ID, null)?.let { id ->
            return id
        }
        val id = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_APP_INSTANCE_ID, id).apply()
        return id
    }

    private companion object {
        const val PREFS_NAME = "fall_ouch_metrics_transport"
        const val KEY_APP_INSTANCE_ID = "app_instance_id"
    }
}
