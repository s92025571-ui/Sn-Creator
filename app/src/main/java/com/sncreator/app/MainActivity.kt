package com.sncreator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.sncreator.app.data.AndroidConnectivityMonitor
import com.sncreator.app.data.DataStorePreferencesRepository
import com.sncreator.app.data.FirebaseGoogleAuthRepository
import com.sncreator.app.data.NetworkContentGenerationRepository
import com.sncreator.app.ui.SnCreatorApp
import com.sncreator.app.ui.theme.SnCreatorTheme
import com.sncreator.app.viewmodel.SnCreatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#080810"))
        )
        super.onCreate(savedInstanceState)

        val appContext = applicationContext
        val connectivityMonitor = AndroidConnectivityMonitor(appContext)
        val authRepository = FirebaseGoogleAuthRepository(appContext)
        val preferencesRepository = DataStorePreferencesRepository(appContext)
        val contentRepository = NetworkContentGenerationRepository(
            backendBaseUrl = BuildConfig.BACKEND_BASE_URL,
            authRepository = authRepository,
            connectivityMonitor = connectivityMonitor
        )

        val factory = SnCreatorViewModel.Factory(
            authRepository = authRepository,
            contentRepository = contentRepository,
            preferencesRepository = preferencesRepository,
            connectivityMonitor = connectivityMonitor
        )
        val viewModel = ViewModelProvider(this, factory)[SnCreatorViewModel::class.java]

        setContent {
            SnCreatorTheme {
                SnCreatorApp(viewModel = viewModel)
            }
        }
    }
}
