
package com.doomx.optimizer

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

object AccessKeyManager {

    private const val API_URL =
        "https://doomx-key-api.doomxcheats.workers.dev"

    private const val PREFS = "doomx_access"
    private const val DEVICE_ID = "device_id"

    data class Result(
        val valid: Boolean,
        val message: String,
        val expiresAt: Long? = null
    )

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        )

        var id = prefs.getString(DEVICE_ID, null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(DEVICE_ID, id).apply()
        }

        return id
    }

    fun activate(
        context: Context,
        key: String,
        callback: (Result) -> Unit
    ) {
        request(context, "/activate", key, callback)
    }

    fun validate(
        context: Context,
        key: String,
        callback: (Result) -> Unit
    ) {
        request(context, "/validate", key, callback)
    }

    private fun request(
        context: Context,
        path: String,
        key: String,
        callback: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val deviceId = getDeviceId(appContext)

        thread {
            val result = try {
                val connection =
                    URL("$API_URL$path").openConnection()
                        as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type", "application/json"
                    )

                    val body = JSONObject()
                        .put("key", key.trim())
                        .put("deviceId", deviceId)
                        .toString()

                    connection.outputStream.use {
                        it.write(body.toByteArray(Charsets.UTF_8))
                    }

                    val status = connection.responseCode
                    val stream = if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val responseText = stream?.bufferedReader()
                        ?.use { it.readText() } ?: "{}"

                    val json = JSONObject(responseText)

                    if (status in 200..299 &&
                        json.optBoolean("valid", false)
                    ) {
                        Result(
                            true,
                            "Access granted",
                            json.optLong("expiresAt")
                                .takeIf { it > 0 }
                        )
                    } else {
                        Result(
                            false,
                            json.optString("error", "Access denied")
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Result(
                    false,
                    "Connection failed. Check your internet and try again."
                )
            }

            Handler(Looper.getMainLooper()).post {
                callback(result)
            }
        }
    }
}
