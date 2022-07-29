package com.example.nhn.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nhn.R
import com.example.nhn.const.Constants

class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WebViewActivity"
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        val url = intent.getStringExtra(Constants.Extra.URL) ?: ""
        Log.d(TAG, "url : $url")

        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                Toast.makeText(this@WebViewActivity, message, Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                Toast.makeText(this@WebViewActivity, message, Toast.LENGTH_SHORT).show()
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "onPageStarted!")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished!")
            }

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                val keyCode = event.keyCode
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && webView.canGoBack()) {
                    webView.goBack()
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && webView.canGoForward()) {
                    webView.goForward()
                    return true
                }
                return false
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                Toast.makeText(this@WebViewActivity, "onReceivedSslError", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        webView.loadUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.stopLoading()
    }
}