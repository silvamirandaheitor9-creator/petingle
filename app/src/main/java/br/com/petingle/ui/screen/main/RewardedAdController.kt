package br.com.petingle.ui.screen.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppAd.AdMode
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

private const val TAG = "PetIngleRewardedAd"

/**
 * Keeps the rewarded ad lifecycle outside of the Compose recomposition cycle.
 *
 * Start.io's rewarded flow is stateful: the same StartAppAd instance that
 * receives onReceiveAd must be the one used by showAd(). Recreating that
 * instance during screen recomposition can leave the video without a valid
 * Activity window even though banners continue to work.
 */
class RewardedAdController(
    private val activity: Activity,
    private val onReward: () -> Unit,
) {
    var preparedAd by mutableStateOf<StartAppAd?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var unavailable by mutableStateOf(false)
        private set

    private var isDisposed = false

    fun load() {
        if (isDisposed || isLoading || preparedAd != null) return

        isLoading = true
        unavailable = false
        val candidate = StartAppAd(activity)

        try {
            candidate.loadAd(
                AdMode.REWARDED_VIDEO,
                object : AdEventListener {
                    override fun onReceiveAd(ad: Ad) {
                        if (isDisposed) return

                        preparedAd = candidate
                        isLoading = false
                        unavailable = false
                        Log.d(TAG, "Rewarded ad loaded")
                    }

                    override fun onFailedToReceiveAd(ad: Ad?) {
                        if (isDisposed) return

                        preparedAd = null
                        isLoading = false
                        unavailable = true
                        Log.w(TAG, "Rewarded ad failed to load: ${ad?.errorMessage}")
                    }
                },
            )
        } catch (error: Exception) {
            preparedAd = null
            isLoading = false
            unavailable = true
            Log.w(TAG, "Rewarded ad load threw an exception", error)
        }
    }

    fun show() {
        val ad = preparedAd ?: run {
            load()
            return
        }

        preparedAd = null
        isLoading = true
        var rewardGranted = false
        var finished = false

        fun finish(success: Boolean) {
            if (finished || isDisposed) return

            finished = true
            isLoading = false
            unavailable = !success
            load()
        }

        ad.setVideoListener {
            if (!isDisposed && !rewardGranted) {
                rewardGranted = true
                onReward()
            }
        }

        try {
            val displayed = ad.showAd(
                object : AdDisplayListener {
                    override fun adDisplayed(ad: Ad) {
                        Log.d(TAG, "Rewarded ad displayed")
                    }

                    override fun adClicked(ad: Ad) {
                        Log.d(TAG, "Rewarded ad clicked")
                    }

                    override fun adHidden(ad: Ad) {
                        Log.d(TAG, "Rewarded ad hidden; completed=$rewardGranted")
                        finish(rewardGranted)
                    }

                    override fun adNotDisplayed(ad: Ad) {
                        Log.w(TAG, "Rewarded ad was not displayed")
                        finish(false)
                    }
                },
            )

            if (!displayed) {
                Log.w(TAG, "Start.io returned false from showAd()")
                finish(false)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Rewarded ad show threw an exception", error)
            finish(false)
        }
    }

    fun clearUnavailable() {
        unavailable = false
    }

    fun dispose() {
        isDisposed = true
        preparedAd = null
        isLoading = false
    }
}

fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
