package com.saheed.videodownloader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.saheed.videodownloader.download.Cookies
import com.saheed.videodownloader.download.LoginSite

/**
 * Signs in to Instagram or Facebook inside the app: the site's own login page in a *WebView*
 * (an in-app browser). You type your details on the site itself; the app never sees them.
 * "Done" keeps the site's cookies for yt-dlp (see [Cookies]).
 */
class LoginActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled") // the login pages don't work without JavaScript
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val site = LoginSite.valueOf(intent.getStringExtra(EXTRA_SITE) ?: LoginSite.INSTAGRAM.name)
        CookieManager.getInstance().setAcceptCookie(true)

        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding()) {
                        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Sign in to ${site.label}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { done(site) }) { Text("Done") }
                        }
                        // AndroidView puts a classic Android View (here the WebView) inside Compose.
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    // Keep the login inside this screen instead of opening Chrome.
                                    webViewClient = WebViewClient()
                                    loadUrl(site.loginUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun done(site: LoginSite) {
        CookieManager.getInstance().flush()
        if (Cookies.save(this, site)) {
            Toast.makeText(this, "Signed in to ${site.label}", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Not signed in yet. Finish signing in, then tap Done.", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val EXTRA_SITE = "site"

        fun intent(context: Context, site: LoginSite): Intent =
            Intent(context, LoginActivity::class.java).putExtra(EXTRA_SITE, site.name)
    }
}
