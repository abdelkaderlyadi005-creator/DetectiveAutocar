@file:OptIn(ExperimentalMaterial3Api::class)

package com.radarpieces.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ads.start(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0B4F8A))) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) { RadarApp() }
                        AdBanner()
                    }
                }
            }
        }
    }
}

private fun open(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

/** Keeps a listing when its title has no year, or any year in the title falls inside the range. */
private fun yearOk(title: String, min: Int?, max: Int?): Boolean {
    if (min == null && max == null) return true
    val years = Regex("\\b(19[89]\\d|20[0-2]\\d)\\b").findAll(title).map { it.value.toInt() }.toList()
    if (years.isEmpty()) return true
    return years.any { (min == null || it >= min) && (max == null || it <= max) }
}

private fun fuelFromPlate(e: String): String {
    val u = e.uppercase()
    return when {
        u.contains("GAZ") || u.contains("DIES") -> "diesel"
        u.contains("HYB") -> "hybride"
        u.contains("ELEC") || u.contains("ÉLEC") -> "électrique"
        u.contains("ESS") -> "essence"
        else -> ""
    }
}

@Composable
fun Pick(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.ifEmpty { "الكل" },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("الكل") }, onClick = { onSelect(""); expanded = false })
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); expanded = false })
            }
        }
    }
}

@Composable
fun RadarApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("radar", Context.MODE_PRIVATE) }
    var clientId by remember { mutableStateOf(prefs.getString("id", "") ?: "") }
    var secret by remember { mutableStateOf(prefs.getString("secret", "") ?: "") }
    var plateToken by remember { mutableStateOf(prefs.getString("plateToken", "") ?: "") }
    var plateHost by remember { mutableStateOf(prefs.getString("plateHost", "apiplaqueimmatriculation.com") ?: "apiplaqueimmatriculation.com") }
    var zip by remember { mutableStateOf(prefs.getString("zip", "") ?: "") }

    var cat by remember { mutableStateOf("parts") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var yearMin by remember { mutableStateOf("") }
    var yearMax by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var partName by remember { mutableStateOf("") }
    var fuelGroup by remember { mutableStateOf("") }
    var engineType by remember { mutableStateOf("") }
    var carFuel by remember { mutableStateOf("") }
    var gearbox by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }

    var plate by remember { mutableStateOf("") }
    var plateInfo by remember { mutableStateOf<VehicleInfo?>(null) }
    var plateLoading by remember { mutableStateOf(false) }
    var plateError by remember { mutableStateOf<String?>(null) }

    var showKeys by remember { mutableStateOf(clientId.isBlank()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Listing>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var campaign by remember { mutableStateOf(prefs.getString("campaign", "") ?: "") }
    var alerts by remember { mutableStateOf(AlertStore.load(ctx)) }
    var alertMsg by remember { mutableStateOf<String?>(null) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val eng = engineType.ifEmpty { fuelGroup.lowercase() }
    val pieces: List<String> = when (cat) {
        "parts" -> listOf(make, model, year, partName, extra)
        "engines" -> listOf(if (eng.startsWith("moteur")) "" else "moteur", eng, make, model, year, extra)
        else -> listOf(make, model, carFuel, gearbox, extra)
    }
    val query = pieces.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
    val limit = maxPrice.replace(',', '.').toDoubleOrNull()
    val yMin = yearMin.toIntOrNull()
    val yMax = yearMax.toIntOrNull()
    val shown = results.filter { l -> (limit == null || l.price <= limit) && (cat != "cars" || yearOk(l.title, yMin, yMax)) }

    fun search() {
        if (query.isEmpty()) return
        if (clientId.isBlank() || secret.isBlank()) {
            showKeys = true
            return
        }
        prefs.edit().putString("zip", zip.trim()).apply()
        loading = true
        error = null
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) { EbayClient.search(clientId.trim(), secret.trim(), query, zip.trim(), campaign.trim()) }
                results = list
                if (list.isEmpty()) error = "ما لقيت حتى إعلان. جرب كلمات أخرى."
            } catch (e: Exception) {
                error = e.message ?: "وقع مشكل فالاتصال"
            } finally {
                loading = false
            }
        }
    }

    fun lookupPlate() {
        if (plate.isBlank()) return
        if (plateToken.isBlank()) {
            showKeys = true
            plateError = "خاصك تحط token ديال خدمة اللوحة فالإعدادات تحت."
            return
        }
        plateLoading = true
        plateError = null
        plateInfo = null
        scope.launch {
            try {
                val v = withContext(Dispatchers.IO) { PlateClient.lookup(plate, plateToken.trim(), plateHost.trim()) }
                plateInfo = v
                val mk = Catalog.makes.keys.firstOrNull { v.make.uppercase().contains(it.uppercase().substringBefore("-").take(5)) }
                make = mk ?: v.make
                val models = Catalog.makes[mk] ?: emptyList()
                model = models.firstOrNull { v.model.uppercase().contains(it.uppercase()) } ?: v.model
                year = v.year
                val f = fuelFromPlate(v.fuel)
                carFuel = f
                fuelGroup = Catalog.engineFuels.keys.firstOrNull { it.equals(f, ignoreCase = true) } ?: ""
                engineType = ""
            } catch (e: Exception) {
                plateError = e.message ?: "وقع مشكل فالاتصال"
            } finally {
                plateLoading = false
            }
        }
    }

    fun addAlert() {
        if (query.isEmpty()) return
        if (clientId.isBlank() || secret.isBlank()) {
            showKeys = true
            return
        }
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        alerts = listOf(Alert(query, zip.trim(), limit, emptyList(), false)) + alerts.filter { it.query != query }
        AlertStore.save(ctx, alerts)
        AlertStore.schedule(ctx)
        alertMsg = "تفعل التنبيه. غادي نخبرك ملي يطلع إعلان جديد."
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DÉTECTIVE AUTO", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("قطع الغيار · المحركات · السيارات — فرنسا", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // plate lookup
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عندك رقم اللوحة؟", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            plate, { plate = it.uppercase().take(10) },
                            label = { Text("AA-123-AA") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { lookupPlate() }, enabled = plate.isNotBlank() && !plateLoading) { Text("جيب المعلومات") }
                    }
                    if (plateLoading) CircularProgressIndicator(Modifier.size(24.dp))
                    plateError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    plateInfo?.let { v ->
                        Text(
                            listOf(v.make, v.model, v.year, v.fuel, if (v.fiscalHp.isNotBlank()) "${v.fiscalHp} CV" else "", if (v.cc.isNotBlank()) "${v.cc} cm³" else "", v.gearbox)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 14.sp,
                        )
                        Text("تعمرو الاختيارات تحت بهاد المعلومات.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("parts" to "قطع الغيار", "engines" to "المحركات", "cars" to "السيارات").forEach { (k, label) ->
                    FilterChip(selected = cat == k, onClick = { cat = k }, label = { Text(label) })
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pick("الماركة", Catalog.makes.keys.toList(), make, { make = it; model = "" }, Modifier.weight(1f))
                    Pick("الموديل", Catalog.makes[make] ?: emptyList(), model, { model = it }, Modifier.weight(1f), enabled = make.isNotEmpty() || model.isNotEmpty())
                }

                when (cat) {
                    "parts" -> {
                        Pick("السنة", Catalog.years, year, { year = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick("نوع القطعة", Catalog.partGroups.keys.toList(), group, { group = it; partName = "" }, Modifier.weight(1f))
                            Pick("القطعة", Catalog.partGroups[group] ?: emptyList(), partName, { partName = it }, Modifier.weight(1f), enabled = group.isNotEmpty())
                        }
                    }
                    "engines" -> {
                        Pick("السنة", Catalog.years, year, { year = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick("الطاقة", Catalog.engineFuels.keys.toList(), fuelGroup, { fuelGroup = it; engineType = "" }, Modifier.weight(1f))
                            Pick("نوع المحرك", Catalog.engineFuels[fuelGroup] ?: emptyList(), engineType, { engineType = it }, Modifier.weight(1f), enabled = fuelGroup.isNotEmpty())
                        }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick("من سنة", Catalog.years, yearMin, { yearMin = it }, Modifier.weight(1f))
                            Pick("حتى سنة", Catalog.years, yearMax, { yearMax = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick("الطاقة", Catalog.carFuels, carFuel, { carFuel = it }, Modifier.weight(1f))
                            Pick("العلبة", Catalog.gearboxes, gearbox, { gearbox = it }, Modifier.weight(1f))
                        }
                    }
                }

                OutlinedTextField(extra, { extra = it }, label = { Text("كلمات زايدة (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        zip, { zip = it.filter(Char::isDigit).take(5) },
                        label = { Text("الكود البريدي") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        maxPrice, { maxPrice = it },
                        label = { Text("أقصى ثمن €") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (query.isNotEmpty()) Text("البحث: $query", fontSize = 12.sp, color = Color.Gray)
                Button(onClick = { search() }, enabled = query.isNotEmpty() && !loading, modifier = Modifier.fillMaxWidth()) {
                    Text("قلب فـ eBay")
                }
                OutlinedButton(onClick = { addAlert() }, enabled = query.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("نبهني ملي يطلع إعلان جديد")
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("فتح نفس البحث فـ", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = query.isNotEmpty(),
                        onClick = {
                            val c = if (cat == "cars") "2" else "27"
                            val reg = if (cat == "cars" && (yMin != null || yMax != null)) "&regdate=${yMin ?: 1990}-${yMax ?: 2025}" else ""
                            open(ctx, "https://www.leboncoin.fr/recherche?text=${enc(query)}&category=$c$reg")
                        },
                    ) { Text("Leboncoin") }
                    OutlinedButton(
                        enabled = query.isNotEmpty(),
                        onClick = { open(ctx, "https://www.facebook.com/marketplace/search/?query=${enc(query)}") },
                    ) { Text("Marketplace") }
                }
            }
        }

        if (loading) item { CircularProgressIndicator(Modifier.size(28.dp)) }
        error?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) } }
        if (results.isNotEmpty()) {
            item { Text("${shown.size} إعلان (من ${results.size})", fontSize = 12.sp, color = Color.Gray) }
        }

        items(shown) { l ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = l.imageUrl,
                        contentDescription = l.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(l.title, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        Text("%.2f %s".format(l.price, if (l.currency == "EUR") "€" else l.currency), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (l.location.isNotBlank()) Text(l.location, fontSize = 12.sp, color = Color.Gray)
                        if (l.seller.isNotBlank()) Text("البائع: ${l.seller}", fontSize = 12.sp, color = Color.Gray)
                        TextButton(onClick = { open(ctx, l.url) }) { Text("فتح الإعلان والاتصال بالبائع") }
                    }
                }
            }
        }

        if (alerts.isNotEmpty() || alertMsg != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("التنبيهات", fontWeight = FontWeight.Bold)
                    alertMsg?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
                    alerts.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                buildString {
                                    append(a.query)
                                    if (a.zip.isNotBlank()) append(" · ").append(a.zip)
                                    a.maxPrice?.let { append(" · ≤ ").append(it.toInt()).append("€") }
                                },
                                modifier = Modifier.weight(1f), fontSize = 13.sp,
                            )
                            TextButton(onClick = { alerts = alerts - a; AlertStore.save(ctx, alerts) }) { Text("حذف") }
                        }
                    }
                    Text("كنفحص كل 30 دقيقة تقريباً، والأندرويد ممكن يأخر الفحص إلا كان التيليفون ساكن. كتخدم مع eBay غير.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showKeys = !showKeys }) { Text(if (showKeys) "خبي الإعدادات" else "الإعدادات والمفاتيح") }
                if (showKeys) {
                    OutlinedTextField(clientId, { clientId = it }, label = { Text("eBay App ID (Client ID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(secret, { secret = it }, label = { Text("eBay Cert ID (Client Secret)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(plateToken, { plateToken = it }, label = { Text("Token ديال خدمة اللوحة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(plateHost, { plateHost = it }, label = { Text("host_name ديال خدمة اللوحة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(campaign, { campaign = it }, label = { Text("eBay Partner Network campaign ID (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        prefs.edit()
                            .putString("id", clientId.trim()).putString("secret", secret.trim())
                            .putString("plateToken", plateToken.trim()).putString("plateHost", plateHost.trim())
                            .putString("campaign", campaign.trim())
                            .apply()
                        showKeys = false
                    }) { Text("حفظ") }
                    Text("المفاتيح كتبقى غير فالتيليفون ديالك.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
