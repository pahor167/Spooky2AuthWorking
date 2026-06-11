package com.spooky2.huntkill

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
}
