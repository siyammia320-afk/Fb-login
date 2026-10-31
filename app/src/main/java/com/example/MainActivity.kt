package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SecurityGuard

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        SecurityGuard.verifySecurityOrExit(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SecurityGuard.verifySecurityOrExit(this)
        viewModel.refreshRemoteSwitchNow()
        if (viewModel.requiredUpdate.value == null && !viewModel.isDownloadingUpdate.value) {
            viewModel.checkForAppUpdate()
        }
    }
}
