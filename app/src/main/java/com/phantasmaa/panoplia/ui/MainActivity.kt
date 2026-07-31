package com.phantasmaa.panoplia.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Trampoline activity. Kept as a separate launcher so the WebViewActivity
 * stays focused and can be relaunched without recreating the entry-point
 * manifest entry.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, WebViewActivity::class.java))
        finish()
    }
}
