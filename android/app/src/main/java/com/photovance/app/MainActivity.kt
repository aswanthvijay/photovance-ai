package com.photovance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.photovance.app.network.ApiClient
import com.photovance.app.ui.PhotovanceApp
import com.photovance.app.ui.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.init(this)
        val studio = ViewModelProvider(this)[StudioViewModel::class.java]
        setContent { PhotovanceApp(studio) }
    }
}
