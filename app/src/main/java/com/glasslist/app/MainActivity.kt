package com.glasslist.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glasslist.app.notify.Notifier
import com.glasslist.app.ui.AppRoot
import com.glasslist.app.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private var openTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openTaskId = intent?.getStringExtra(Notifier.X_TASK)
        setContent {
            val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(application as GlasslistApp) } })
            AppRoot(vm, openTaskId) { openTaskId = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTaskId = intent.getStringExtra(Notifier.X_TASK)
    }
}
