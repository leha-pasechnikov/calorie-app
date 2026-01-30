package com.example.calorie

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class LegalActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HTML_FILE = "html_file"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        val toolbar = findViewById<Toolbar>(R.id.toolbar) // не нужно, если без тулбара
        // Но проще — использовать полноэкранный WebView

        val fileName = intent.getStringExtra(EXTRA_HTML_FILE) ?: "privacy_policy.html"
        webView.loadUrl("file:///android_asset/$fileName")
    }
}