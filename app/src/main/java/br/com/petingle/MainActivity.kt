package br.com.petingle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.ui.navigation.PetIngleNavGraph
import br.com.petingle.ui.theme.PetIngleTheme
import br.com.petingle.ui.viewmodel.ThemeViewModel
import com.startapp.sdk.adsbase.StartAppSDK
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StartAppSDK.initParams(applicationContext, "207863473")
            .setReturnAdsEnabled(false)
            .init()
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

            PetIngleTheme(darkTheme = isDarkTheme) {
                PetIngleNavGraph()
            }
        }
    }
}
