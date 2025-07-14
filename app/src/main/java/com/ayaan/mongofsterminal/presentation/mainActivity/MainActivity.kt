package com.ayaan.mongofsterminal.presentation.mainActivity

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ayaan.mongofsterminal.navigation.AppNavigation
import com.ayaan.mongofsterminal.ui.theme.MongoFSTerminalTheme
import com.speedchecker.android.sdk.SpeedcheckerSDK
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Black.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Black.toArgb())
        )
        SpeedcheckerSDK.init(this)
        Log.d("MainActivity", "SpeedcheckerSDK initialized: ${SpeedcheckerSDK.getSDKState(this)}")
        setContent {
            MongoFSTerminalTheme(darkTheme = false) {

                Scaffold(
                    modifier = Modifier.fillMaxSize().padding(top =22.dp),
//                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}