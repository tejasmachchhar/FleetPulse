package com.fleetpulse.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fleetpulse.app.data.prefs.fleetDataStore
import com.fleetpulse.app.support.SupportConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.io.readBytes
import kotlin.io.use

data class VehicleReferenceCatalogEntry(
    val id: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val variant: String,
    val year: Int,
    val category: String,
    val subcategory: String,
    val propulsion: String,
    val fuelType: String,
    val aliases: List<String>,
    val recordPath: String,
    val sourceUrls: List<String> = emptyList(),
)

data class VehicleReferenceVehicleRecord(
    val rawJson: String? = null,
    val id: String,
    val manufacturerId: String,
    val manufacturerName: String,
    val manufacturerBrand: String,
    val modelName: String,
    val officialProductName: String,
    val variant: String,
    val modelYear: Int,
    val market: String,
    val category: String,
    val subcategory: String,
    val propulsion: String,
    val fuelType: String,
    val powerKw: Double?,
    val engineDisplacementCc: Double?,
    val torqueNm: Double?,
    val fuelTankLiters: Double?,
    val batteryCapacityKwh: Double?,
    val batteryUsableKwh: Double?,
    val batteryVoltageV: Double?,
    val batteryCapacityAh: Double?,
    val seatingCapacity: Int?,
    val cargoCapacityLiters: Double?,
    val wheelCount: Int?,
    val axleConfiguration: String?,
    val engineOilGrade: String?,
    val sources: List<VehicleReferenceSource>,
    val verificationStatus: String?,
    val sourceUrls: List<String>,
)

data class VehicleReferenceSource(
    val publisher: String,
    val title: String,
    val url: String,
    val sourceType: String,
    val sourceId: String?,
)

data class VehicleReferenceManufacturer(
    val id: String,
    val name: String,
    val brand: String,
    val countryOfOrigin: String?,
    val indiaMarketActive: Boolean,
    val officialWebsite: String?,
    val categories: List<String>,
)

data class VehicleReferenceManifest(
    val databaseName: String,
    val databaseVersion: String,
    val schemaVersion: String,
    val generatedAt: String,
    val phase: String,
    val totalRecords: Int,
    val categoryKeys: List<String>,
) {
    val categorySlugs: List<String> get() = categoryKeys
}

class VehicleReferenceRepository(private val context: Context) {

    private val dataStore = context.fleetDataStore
    private val preferenceKeys = mutableMapOf<String, Preferences.Key<String>>()

    private fun keyFor(name: String): Preferences.Key<String> =
        preferenceKeys.getOrPut(name) { stringPreferencesKey(name) }

    private suspend fun cachedValue(name: String): String? =
        dataStore.data.first()[keyFor(name)]

    private suspend fun cacheValue(name: String, value: String) {
        dataStore.edit { it[keyFor(name)] = value }
    }

    fun normalizeSlug(slug: String): String =
        slug.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-").takeIf { it.isNotBlank() } ?: "other"

    fun validateRecordPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.contains("..")) return false
        if (!path.startsWith("vehicles/") && !path.startsWith("manufacturers/")) return false
        if (!path.endsWith(".json")) return false
        return path.split('/').all { it.isNotBlank() && !it.contains('\\') }
    }

    /** Total access: never throws, never blocks composition for a network stall. */
    suspend fun loadManifest(): VehicleReferenceManifest? = try {
        cachedValue("vehicle_ref_manifest")?.let { VehicleReferenceParser.parseManifest(it) }
            ?: readAsset("vehicle-reference/manifest.json")?.let { VehicleReferenceParser.parseManifest(it) }
    } catch (_: Exception) { null }

    suspend fun loadCatalogEntries(categorySlug: String): List<VehicleReferenceCatalogEntry> = try {
        val slug = normalizeSlug(categorySlug)
        val cached = cachedValue("vehicle_ref_index_$slug")
        val text = cached
            ?: readAsset("vehicle-reference/indexes/$slug.json")
            ?: return emptyList()
        VehicleReferenceParser.parseIndex(text)
    } catch (_: Exception) { emptyList() }

    suspend fun loadManufacturers(): List<VehicleReferenceManufacturer> = try {
        cachedValue("vehicle_ref_manufacturer_index")
            ?.let { VehicleReferenceParser.parseManufacturers(it) }
            ?: readAsset("vehicle-reference/manufacturers/index.json")
                ?.let { VehicleReferenceParser.parseManufacturers(it) }
                ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun loadVehicleRecord(recordPath: String): VehicleReferenceVehicleRecord? {
        if (!validateRecordPath(recordPath)) return null
        return try {
            cachedValue("vehicle_ref_record_$recordPath")
                ?.let { VehicleReferenceParser.parseVehicleRecord(it) }
                ?: readAsset("vehicle-reference/$recordPath")
                    ?.let { VehicleReferenceParser.parseVehicleRecord(it) }
        } catch (_: Exception) { null }
    }

    /** Start a cached refresh in the background. Failures are ignored. */
    suspend fun refreshRemoteIfStale(force: Boolean = false) = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val manifest = loadManifest()
        val version = manifest?.databaseVersion ?: return@withContext
        val cached = cachedValue("vehicle_ref_manifest")
        if (!force && cached != null) return@withContext
        val text = fetchJson(SupportConfig.VEHICLE_REFERENCE_BASE_URL + "manifest.json")
            ?: return@withContext
        val parsed = try { VehicleReferenceParser.parseManifest(text) } catch (_: Exception) { return@withContext }
        if (parsed.schemaVersion != version) return@withContext
        cacheValue("vehicle_ref_manifest", text)
        cacheIndex(parsed.databaseVersion, text)
    }

    private suspend fun cacheIndex(databaseVersion: String, manifestText: String) {
        // Refresh indexes that are not cached. Do not overwrite user edits (there are none
        // for reference data; this only caches the browsing catalogue).
        val root = try { JSONObject(manifestText) } catch (_: Exception) { return }
        val categoryObject = root.optJSONObject("category_counts")
        val categoryArray = root.optJSONArray("category_counts") ?: JSONArray()
        val slugs = mutableSetOf<String>()
        if (categoryObject != null) {
            val names = categoryObject.names() ?: categoryArray ?: categoryArray
            for (i in 0 until names.length()) {
                slugs.add(normalizeSlug(names.getString(i)))
            }
        } else {
            for (i in 0 until categoryArray.length()) {
                slugs.add(normalizeSlug(categoryArray.optString(i)))
            }
        }
        slugs.forEach { slug ->
            val key = "vehicle_ref_index_${databaseVersion}_$slug"
            if (cachedValue(key) == null) {
                fetchJson(SupportConfig.VEHICLE_REFERENCE_BASE_URL + "indexes/$slug.json")
                    ?.let { text -> cacheValue(key, text) }
            }
        }
    }

    private suspend fun fetchJson(url: String): String? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 4_000
                connection.readTimeout = 5_000
                connection.instanceFollowRedirects = true
                connection.doInput = true
                connection.doOutput = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "FleetPulse-ReferenceDB/1")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                connection.inputStream.readBytes().toString(Charsets.UTF_8)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) { null }
    }

    private fun readAsset(path: String): String? = try {
        context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: Exception) { null }

    private fun JSONObject?.optDoubleOrNull(name: String): Double? =
        takeIf { this != null }?.optDouble(name)?.takeIf { !it.isNaN() }

    private fun JSONObject?.optIntOrNull(name: String): Int? =
        takeIf { this != null }?.optInt(name).takeIf { it != 0 }

    private fun JSONObject?.optStringOrNull(name: String): String? =
        takeIf { this != null }?.optString(name)?.takeIf { it.isNotBlank() }
}

object VehicleReferenceParser {

    private const val SUPPORTED_SCHEMA_VERSION = "1.0.0"

    fun parseManifest(text: String): VehicleReferenceManifest {
        val root = JSONObject(text)
        val categoryKeys = mutableListOf<String>()
        val categoryObject = root.optJSONObject("category_counts")
        val categoryArray = root.optJSONArray("category_counts") ?: JSONArray()
        if (categoryObject != null) {
            val names = categoryObject.names() ?: categoryArray ?: categoryArray
            for (i in 0 until names.length()) {
                val name = names.getString(i)
                if (categoryObject.has(name)) categoryKeys.add(normalizeCategoryKey(name))
            }
        } else {
            for (i in 0 until categoryArray.length()) {
                categoryKeys.add(normalizeCategoryKey(categoryArray.optString(i)))
            }
        }
        return VehicleReferenceManifest(
            databaseName = root.optString("database_name").ifBlank { "Vehicle Reference Database" },
            databaseVersion = root.optString("database_version"),
            schemaVersion = root.optString("schema_version"),
            generatedAt = root.optString("generated_at"),
            phase = root.optString("phase"),
            totalRecords = root.optInt("total_records"),
            categoryKeys = categoryKeys.ifEmpty { listOf("other") },
        )
    }

    fun parseIndex(text: String): List<VehicleReferenceCatalogEntry> {
        val root = JSONObject(text)
        val records = root.optJSONArray("records") ?: JSONArray()
        return (0 until records.length()).mapNotNull { i ->
            records.optJSONObject(i)?.let { record -> VehicleReferenceParser.toCatalogEntry(record) }
        }
    }

    fun parseVehicleRecord(text: String): VehicleReferenceVehicleRecord {
        val root = JSONObject(text)
        val manufacturer = root.optJSONObject("manufacturer")
        val model = root.optJSONObject("model")
        val powertrain = root.optJSONObject("powertrain")
        val storage = root.optJSONObject("energy_storage")
        val dimensions = root.optJSONObject("dimensions")
        val maintenance = root.optJSONObject("maintenance")
        val sources = root.optJSONArray("sources") ?: JSONArray()
        return VehicleReferenceVehicleRecord(
            rawJson = text,
            id = root.optString("id"),
            manufacturerId = manufacturer?.optString("id") ?: "",
            manufacturerName = manufacturer?.optString("name") ?: "",
            manufacturerBrand = manufacturer?.optString("brand") ?: "",
            modelName = model?.optString("name") ?: "",
            officialProductName = model?.optString("official_product_name") ?: model?.optString("name") ?: "",
            variant = model?.optString("variant") ?: "",
            modelYear = model?.optInt("year") ?: 0,
            market = model?.optString("market") ?: "",
            category = root.optJSONObject("classification")?.optString("category") ?: "",
            subcategory = root.optJSONObject("classification")?.optString("subcategory") ?: "",
            propulsion = powertrain?.optString("propulsion") ?: "",
            fuelType = powertrain?.optString("fuel_type") ?: "",
            powerKw = powertrain?.optDoubleOrNull("power"),
            engineDisplacementCc = powertrain?.optDoubleOrNull("engine_displacement_cc"),
            torqueNm = powertrain?.optDoubleOrNull("torque"),
            fuelTankLiters = storage?.optDoubleOrNull("fuel_tank_liters"),
            batteryCapacityKwh = storage?.optDoubleOrNull("battery_capacity_kwh"),
            batteryUsableKwh = storage?.optDoubleOrNull("battery_usable_kwh"),
            batteryVoltageV = storage?.optDoubleOrNull("battery_voltage_v"),
            batteryCapacityAh = storage?.optDoubleOrNull("battery_capacity_ah"),
            seatingCapacity = dimensions?.optIntOrNull("seating_capacity"),
            cargoCapacityLiters = dimensions?.optDoubleOrNull("cargo_capacity_liters"),
            wheelCount = dimensions?.optIntOrNull("wheel_count"),
            axleConfiguration = dimensions?.optStringOrNull("axle_configuration"),
            engineOilGrade = maintenance?.optStringOrNull("engine_oil_grade"),
            sources = (0 until sources.length()).mapNotNull { i ->
                val source = sources.optJSONObject(i) ?: return@mapNotNull null
                VehicleReferenceSource(
                    publisher = source.optString("publisher"),
                    title = source.optString("title"),
                    url = source.optString("url"),
                    sourceType = source.optString("source_type"),
                    sourceId = source.optString("source_id").takeIf { it.isNotBlank() },
                )
            },
            verificationStatus = root.optJSONObject("verification")?.optString("status"),
            sourceUrls = sources.toStrings("url"),
        )
    }

    fun parseManufacturers(text: String): List<VehicleReferenceManufacturer> {
        val root = JSONObject(text)
        val manufacturers = root.optJSONArray("manufacturers") ?: JSONArray()
        return (0 until manufacturers.length()).mapNotNull { i ->
            manufacturers.optJSONObject(i)?.let { m ->
                VehicleReferenceManufacturer(
                    id = m.optString("id"),
                    name = m.optString("name"),
                    brand = m.optString("brand"),
                    countryOfOrigin = m.optString("country_of_origin").takeIf { it.isNotBlank() },
                    indiaMarketActive = m.optBoolean("india_market_active"),
                    officialWebsite = m.optString("official_website").takeIf { it.isNotBlank() },
                    categories = m.optJSONArray("categories")?.toStrings("") ?: emptyList(),
                )
            }
        }
    }

    private fun normalizeCategoryKey(value: String): String =
        value.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")

    private data class VehicleReferenceCatalogIndex(
        val id: String,
        val manufacturerName: String,
        val brand: String,
        val model: String,
        val variant: String,
        val year: Int,
        val category: String,
        val subcategory: String,
        val propulsion: String,
        val fuelType: String,
        val aliases: List<String>,
        val recordPath: String,
        val sourceUrls: List<String>,
    )

    private fun toCatalogEntry(record: JSONObject): VehicleReferenceCatalogEntry {
        val sources = record.optJSONArray("sources") ?: JSONArray()
        return VehicleReferenceCatalogEntry(
            id = record.optString("id"),
            manufacturer = record.optString("manufacturer_name"),
            brand = record.optString("brand"),
            model = record.optString("model"),
            variant = record.optString("variant"),
            year = record.optInt("year"),
            category = record.optString("category"),
            subcategory = record.optString("subcategory"),
            propulsion = record.optString("propulsion"),
            fuelType = record.optString("fuel_type"),
            aliases = record.optJSONArray("aliases")?.toStrings("") ?: emptyList(),
            recordPath = record.optString("record_path"),
            sourceUrls = sources.toStrings("url"),
        )
    }

    private fun JSONArray?.toStrings(field: String): List<String> =
        this?.let { arr -> (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { s -> s.isNotBlank() } } } ?: emptyList()

    private fun JSONObject?.optDoubleOrNull(name: String): Double? =
        takeIf { this != null }?.optDouble(name)?.takeIf { !it.isNaN() }

    private fun JSONObject?.optIntOrNull(name: String): Int? =
        takeIf { this != null }?.optInt(name).takeIf { it != 0 }

    private fun JSONObject?.optStringOrNull(name: String): String? =
        takeIf { this != null }?.optString(name)?.takeIf { it.isNotBlank() }

    fun supportedSchemaVersion(): String = SUPPORTED_SCHEMA_VERSION
}

fun encodeUrlComponent(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
