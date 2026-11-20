package com.radarpieces.app

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class VehicleInfo(
    val make: String,
    val model: String,
    val year: String,
    val fuel: String,
    val fiscalHp: String,
    val cc: String,
    val gearbox: String,
)

/** Looks a French plate up through the third-party apiplaqueimmatriculation.com service (needs the user's own token). */
object PlateClient {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun normalize(raw: String): String {
        val p = raw.uppercase().filter { it.isLetterOrDigit() }
        return if (Regex("^[A-Z]{2}\\d{3}[A-Z]{2}$").matches(p)) "${p.substring(0, 2)}-${p.substring(2, 5)}-${p.substring(5)}" else raw.trim().uppercase()
    }

    private fun post(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        c.doOutput = true
        c.outputStream.use { it.write(ByteArray(0)) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("HTTP $code")
        return text
    }

    fun lookup(plate: String, token: String, host: String): VehicleInfo {
        val q = "?host_name=${enc(host)}&immatriculation=${enc(normalize(plate))}&token=${enc(token)}&format=json"
        val base = "api.apiplaqueimmatriculation.com/carte-grise"
        val text = try {
            post("https://$base$q")
        } catch (e: IOException) {
            post("http://$base$q")
        }
        val data = JSONObject(text).optJSONObject("data") ?: throw IOException("جواب غير متوقع من الخدمة")
        val err = data.optString("erreur")
        if (err.isNotBlank() && err != "null") throw IOException(err)
        val make = data.optString("marque")
        if (make.isBlank()) throw IOException("ما لقيت هاد اللوحة")
        val date = data.optString("date1erCir_fr")
        val year = if (date.length >= 4 && date.takeLast(4).all(Char::isDigit)) date.takeLast(4) else ""
        return VehicleInfo(
            make = make,
            model = data.optString("modele").ifBlank { data.optString("sra_commercial") },
            year = year,
            fuel = data.optString("energie"),
            fiscalHp = data.optString("puisFisc"),
            cc = data.optString("cylindres"),
            gearbox = data.optString("boite_vitesse"),
        )
    }
}
