package com.radarpieces.app

import android.util.Base64
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Listing(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val imageUrl: String?,
    val url: String,
    val location: String,
    val seller: String,
)

/** Reads listings from the official eBay Browse API (marketplace EBAY_FR). */
object EbayClient {
    private var token: String? = null
    private var expiresAt = 0L

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun http(url: String, method: String, headers: Map<String, String>, body: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) {
            c.doOutput = true
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("eBay HTTP $code: ${text.take(200)}")
        return text
    }

    private fun getToken(clientId: String, secret: String): String {
        val now = System.currentTimeMillis()
        token?.let { if (now < expiresAt) return it }
        val basic = Base64.encodeToString("$clientId:$secret".toByteArray(), Base64.NO_WRAP)
        val body = "grant_type=client_credentials&scope=" + enc("https://api.ebay.com/oauth/api_scope")
        val res = http(
            "https://api.ebay.com/identity/v1/oauth2/token", "POST",
            mapOf("Content-Type" to "application/x-www-form-urlencoded", "Authorization" to "Basic $basic"),
            body,
        )
        val json = JSONObject(res)
        val t = json.getString("access_token")
        expiresAt = now + (json.optLong("expires_in", 7200) - 120) * 1000
        token = t
        return t
    }

    fun search(clientId: String, secret: String, query: String, zip: String, campaignId: String = ""): List<Listing> {
        val t = getToken(clientId, secret)
        val url = "https://api.ebay.com/buy/browse/v1/item_summary/search" +
            "?q=" + enc(query) + "&limit=50&sort=newlyListed&filter=" + enc("deliveryCountry:FR")
        val headers = mutableMapOf(
            "Authorization" to "Bearer $t",
            "X-EBAY-C-MARKETPLACE-ID" to "EBAY_FR",
        )
        val ctxParts = ArrayList<String>()
        if (zip.isNotBlank()) ctxParts.add("contextualLocation=" + enc("country=FR,zip=$zip"))
        if (campaignId.isNotBlank()) ctxParts.add("affiliateCampaignId=$campaignId")
        if (ctxParts.isNotEmpty()) headers["X-EBAY-C-ENDUSERCTX"] = ctxParts.joinToString(",")
        val json = JSONObject(http(url, "GET", headers))
        val arr = json.optJSONArray("itemSummaries") ?: return emptyList()
        val out = ArrayList<Listing>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val price = o.optJSONObject("price")
            val loc = o.optJSONObject("itemLocation")
            out.add(
                Listing(
                    title = o.optString("title"),
                    price = price?.optString("value")?.toDoubleOrNull() ?: 0.0,
                    currency = price?.optString("currency") ?: "EUR",
                    imageUrl = o.optJSONObject("image")?.optString("imageUrl"),
                    id = o.optString("itemId"),
                    url = o.optString("itemAffiliateWebUrl").ifBlank { o.optString("itemWebUrl") },
                    location = listOfNotNull(loc?.optString("postalCode"), loc?.optString("country"))
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    seller = o.optJSONObject("seller")?.optString("username") ?: "",
                ),
            )
        }
        return out
    }
}
