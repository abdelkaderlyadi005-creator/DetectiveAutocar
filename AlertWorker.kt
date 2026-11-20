package com.radarpieces.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Notifier {
    private const val CHANNEL = "alerts"

    fun notifyNew(c: Context, a: Alert, fresh: List<Listing>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "تنبيهات الإعلانات", NotificationManager.IMPORTANCE_DEFAULT))
        val top = fresh.first()
        val pi = PendingIntent.getActivity(
            c, a.query.hashCode(), Intent(Intent.ACTION_VIEW, Uri.parse(top.url)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("${fresh.size} إعلان جديد: ${a.query}")
            .setContentText("${top.title} · %.0f €".format(top.price))
            .setStyle(NotificationCompat.BigTextStyle().bigText("${top.title} · %.0f €".format(top.price)))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(a.query.hashCode(), n)
    }
}

class AlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        val prefs = c.getSharedPreferences("radar", Context.MODE_PRIVATE)
        val id = prefs.getString("id", "") ?: ""
        val secret = prefs.getString("secret", "") ?: ""
        val campaign = prefs.getString("campaign", "") ?: ""
        if (id.isBlank() || secret.isBlank()) return Result.success()
        val alerts = AlertStore.load(c)
        if (alerts.isEmpty()) return Result.success()

        val updated = alerts.map { a ->
            try {
                val list = withContext(Dispatchers.IO) { EbayClient.search(id, secret, a.query, a.zip, campaign) }
                    .filter { a.maxPrice == null || it.price <= a.maxPrice }
                val fresh = list.filter { it.id !in a.seen }
                // The first check only records what already exists, so the user is not flooded.
                if (a.primed && fresh.isNotEmpty()) Notifier.notifyNew(c, a, fresh)
                a.copy(seen = (fresh.map { it.id } + a.seen).take(300), primed = true)
            } catch (e: Exception) {
                a
            }
        }
        // keep alerts the user added or removed while this check was running
        val current = AlertStore.load(c)
        val merged = current.map { cur -> updated.firstOrNull { it.query == cur.query } ?: cur }
        AlertStore.save(c, merged)
        return Result.success()
    }
}
