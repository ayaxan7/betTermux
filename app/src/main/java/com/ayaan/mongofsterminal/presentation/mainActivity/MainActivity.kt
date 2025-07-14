package com.ayaan.mongofsterminal.presentation.mainActivity

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.ayaan.mongofsterminal.navigation.AppNavigation
import com.ayaan.mongofsterminal.ui.theme.MongoFSTerminalTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import com.speedchecker.android.sdk.SpeedcheckerSDK

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeedcheckerSDK.init(this)
        Log.d("MainActivity", "SpeedcheckerSDK initialized : ${SpeedcheckerSDK.getSDKState(this)}")
//        enableEdgeToEdge()
        setContent {
            MongoFSTerminalTheme(darkTheme = false) {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Black,
                    )
                    systemUiController.setNavigationBarColor(
                        color = Color.Black,
                    )
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}