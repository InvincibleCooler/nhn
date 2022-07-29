package com.example.nhn.net

import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class RequestManager {
    companion object {
        private const val TAG = "RequestManager"

        private const val METHOD_GET = "GET"
        private const val METHOD_POST = "POST"
        private const val METHOD_PUT = "PUT"
        private const val METHOD_DELETE = "DELETE"

        private const val CONNECTION_TIMEOUT = 10 * 1000    // 10초
        private const val READ_TIMEOUT = 10 * 1000 // 10초

        private const val CHARSET_NAME_UTF8 = "UTF-8"

        private const val PROTOCOL_HTTP = "http"
        private const val PROTOCOL_HTTPS = "https"

        fun request(
            apiPath: String,
            method: String = METHOD_GET,
            values: Map<String, String>? = null,
            connectionTimeout: Int = CONNECTION_TIMEOUT,
            readTimeout: Int = READ_TIMEOUT,
            header: Map<String, String>? = null
        ): String {
            val targetUrl = URL(apiPath)
            val connection = if (targetUrl.protocol.lowercase() == PROTOCOL_HTTPS) {
                targetUrl.openConnection() as HttpsURLConnection
            } else {
                targetUrl.openConnection() as HttpURLConnection
            }

            connection.requestMethod = method
            connection.readTimeout = readTimeout
            connection.connectTimeout = connectionTimeout
            connection.useCaches = false
            connection.doInput = true

            if (header != null) {
                for ((key, value) in header.entries) {
                    connection.setRequestProperty(key, value)
                }
            }

            if (method == METHOD_POST
                || method == METHOD_PUT
                || method == METHOD_DELETE) {
                connection.doOutput = true

                val os = connection.outputStream
                val bw = BufferedWriter(OutputStreamWriter(os, CHARSET_NAME_UTF8))

                val urlQuery = getURLQuery(values)
                if (urlQuery == null) {
                    Log.e("RequestMessenger", "urlQuery is null")
                    return ""
                }

                bw.write(urlQuery)
                bw.flush()
                bw.close()
                os?.close()
            }
            connection.connect()

            val responseSb = StringBuilder()
            val inputStream = if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                InputStreamReader(connection.inputStream)
            } else {
                InputStreamReader(connection.errorStream)
            }

            var buffer: String?

            val bufferReader = BufferedReader(inputStream)
            while (bufferReader.readLine().also { buffer = it } != null) {
                responseSb.append(buffer)
                responseSb.append("\n")
            }

            bufferReader.close()
            connection.disconnect()

            return responseSb.toString()
        }

        private fun getURLQuery(params: Map<String, String>?): String? {
            if (params == null) {
                Log.e(TAG, "getURLQuery params is null")
                return null
            }
            val sb = StringBuilder()
            var first = true

            for ((key, value) in params.entries) {
                if (first) {
                    first = false
                } else {
                    sb.append("&")
                }
                try {
                    if (key.isEmpty()) {
                        Log.e(TAG, "getURLQuery key is null")
                        return null
                    }
                    sb.append(URLEncoder.encode(key, CHARSET_NAME_UTF8))
                    sb.append("=")
                    sb.append(URLEncoder.encode(value, CHARSET_NAME_UTF8))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                    return null
                }
            }
            return sb.toString()
        }
    }

}