package com.brytebee.ecomesh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brytebee.ecomesh.core.discovery.AndroidContext
import com.brytebee.ecomesh.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContext.context = applicationContext
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
