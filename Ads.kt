package com.radarpieces.app

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object Ads {
    // Google's public TEST ad unit. Before publishing, replace it with the real banner unit id from your AdMob account
    // (and the test app id in AndroidManifest.xml with the real one).
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    var ready by mutableStateOf(false)
    private var started = false

    /** Asks for consent first (required in the EU/EEA), and only then starts the ads SDK. */
    fun start(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)

        fun init() {
            if (info.canRequestAds() && !started) {
                started = true
                MobileAds.initialize(activity) {}
                ready = true
            }
        }

        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { init() } },
            { init() },
        )
        init()
    }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!Ads.ready) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = Ads.BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
