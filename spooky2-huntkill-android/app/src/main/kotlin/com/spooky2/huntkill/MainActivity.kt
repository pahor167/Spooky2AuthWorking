package com.spooky2.huntkill

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.spooky2.huntkill.ui.HuntKillNavHost
import com.spooky2.huntkill.ui.theme.SignalLabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalLabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HuntKillNavHost()
                }
            }
        }
    }

    /**
     * singleTop: the OS re-delivers the launch intent here instead of creating a second
     * Activity instance when the app is already running (e.g. plugging USB while a scan
     * is active). The USB attach intent-filter handling that is already wired elsewhere
     * remains unaffected — we just forward to super and let the system handle it.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep getIntent() pointing at the latest delivery (super doesn't do this).
        setIntent(intent)
    }
}
