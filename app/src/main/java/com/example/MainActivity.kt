package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoSearchViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val searchViewModel: VideoSearchViewModel = viewModel()
      val uiState by searchViewModel.uiState.collectAsState()

      // 1. Notification Permission Request (Android 13+)
      val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
      ) { /* Permission result handled */ }

      LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(
                      this@MainActivity,
                      Manifest.permission.POST_NOTIFICATIONS
                  ) != PackageManager.PERMISSION_GRANTED
              ) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
          }
      }

      // 2. Dynamic Theme Switching (Light / Dark / System Default)
      val isDarkTheme = when (uiState.selectedTheme) {
          "Dark" -> true
          "Light" -> false
          else -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        HomeScreen(viewModel = searchViewModel)
      }
    }
  }
}

