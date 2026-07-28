package com.ajimsjames.wearbaroalt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ajimsjames.wearbaroalt.ui.BaroAltScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaroAltScreen()
        }
    }
}
