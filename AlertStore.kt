package com.radarpieces.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One saved search that is checked in the background. [primed] stays false until the first check has recorded what already exists. */
data class Alert(
    val query: String,
    val zip: String,
    val maxPrice: Double?,
    val seen: List<String>,
    val primed: Boolean,
)

object AlertStore {
    private fun prefs(c: Context) = c.getSharedPreferences("radar", Context.MODE_PRIVATE)

    fun load(c: Context): List<Alert> {
        val arr = try {
            JSONArray(prefs(c).getString("alerts", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        val out = ArrayList<Alert>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val seenArr = o.optJSONArray("seen") ?: JSONArray()
            out.add(
                Alert(
                    query = o.getString("q"),
                    zip = o.optString("zip"),
                    maxPrice = if (o.has("max")) o.getDouble("max") else null,
                    seen = (0 until seenArr.length()).map { seenArr.getString(it) },
                    primed = o.optBoolean("primed", false),
                ),
            )
        }
        return out
    }

    fun save(c: Context, list: List<Alert>) {
        val arr = JSONArray()
        list.forEach { a ->
            val o = JSONObject()
            o.put("q", a.query)
            o.put("zip", a.zip)
            a.maxPrice?.let { o.put("max", it) }
            o.put("seen", JSONArray(a.seen))
            o.put("primed", a.primed)
            arr.put(o)
        }
        prefs(c).edit().putString("alerts", arr.toString()).apply()
    }

    /** Runs the background check about every 30 minutes while the phone has a connection. */
    fun schedule(c: Context) {
        val req = PeriodicWorkRequestBuilder<AlertWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("alerts", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
