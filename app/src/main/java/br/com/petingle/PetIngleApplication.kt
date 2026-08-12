package br.com.petingle

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.petingle.data.notifications.NotificationChannels
import com.startapp.sdk.adsbase.StartAppSDK
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PetIngleApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // O modo de teste garante inventário recompensado durante o debug sem
        // gerar cliques/impressões inválidos na conta de produção. BuildConfig
        // remove esse comportamento automaticamente nas versões release.
        if (BuildConfig.DEBUG) {
            StartAppSDK.setTestAdsEnabled(true)
        }
        StartAppSDK.initParams(this, "207863473")
            .setReturnAdsEnabled(false)
            .init()
        // Registra os canais de notificação obrigatórios no Android 8+ (API 26+).
        // Sem isso o sistema descarta todas as notificações silenciosamente.
        NotificationChannels.createChannels(this)
    }
}
