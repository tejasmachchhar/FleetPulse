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
)

data class VehicleReferenceSource(
    val publisher: String,
    val title: String,
    val url: String,
    val sourceType: String,
    val sourceId: String?,
)

data class VehicleReferenceManifest(
    val databaseName: String,
    val databaseVersion: String,
    val schemaVersion: String,
    val generatedAt: String,
    val phase: String,
    val totalRecords: Int,
    val categorySlugs: List<String>,
)

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

    suspend fun loadManifest(): VehicleReferenceManifest {
        val text = fetchJson("${SupportConfig.VEHICLE_REFERENCE_BASE_URL}manifest.json")
        return parseManifest(text)
    }

    suspend fun loadCatalogEntries(categorySlug: String): List<VehicleReferenceCatalogEntry> {
        val assetPath = "vehicle-reference/indexes/$categorySlug.json"
        val cached = cachedValue("vehicle_ref_index_$categorySlug")
        val remote = runCatching { fetchJson("${SupportConfig.VEHICLE_REFERENCE_BASE_URL}indexes/$categorySlug.json") }.getOrNull()
        val text = remote ?: cached ?: readAsset(assetPath)
        if (remote != null) cacheValue("vehicle_ref_index_$categorySlug", remote)
        return parseIndex(text).map { it.toEntry() }
    }

    suspend fun loadVehicleRecord(recordPath: String): VehicleReferenceVehicleRecord? {
        val cached = cachedValue("vehicle_ref_record_$recordPath")
        val remote = runCatching { fetchJson(SupportConfig.VEHICLE_REFERENCE_BASE_URL + recordPath) }.getOrNull()
        val assetPath = "vehicle-reference/$recordPath"
        val text = remote ?: cached ?: runCatching { readAsset(assetPath) }.getOrNull()
        if (remote != null) cacheValue("vehicle_ref_record_$recordPath", remote)
        return text?.let { parseVehicleRecord(it) }
    }

    suspend fun loadManufacturers(): List<VehicleReferenceCatalogEntry> {
        val assetPath = "vehicle-reference/manufacturers/index.json"
        val cached = cachedValue("vehicle_ref_manufacturer_index")
        val remote = runCatching { fetchJson("${SupportConfig.VEHICLE_REFERENCE_BASE_URL}manufacturers/index.json") }.getOrNull()
        val text = remote ?: cached ?: readAsset(assetPath)
        if (remote != null) cacheValue("vehicle_ref_manufacturer_index", remote)
        return parseManufacturerIndex(text)
    }

    private fun parseIndex(text: String): List<VehicleReferenceCatalogIndex> {
        val obj = JSONObject(text)
        val records = obj.optJSONArray("records") ?: JSONArray()
        return (0 until records.length()).mapNotNull { records.optJSONObject(it)?.let { record -> record.toCatalogEntry(record) } }
    }

    private fun parseManufacturerIndex(text: String): List<VehicleReferenceCatalogEntry> {
        val obj = JSONObject(text)
        val manufacturers = obj.optJSONArray("manufacturers") ?: JSONArray()
        return (0 until manufacturers.length()).mapNotNull { i ->
            val m = manufacturers.optJSONObject(i) ?: return@mapNotNull null
            VehicleReferenceCatalogEntry(
                id = m.optString("id"),
                manufacturer = m.optString("name"),
                brand = m.optString("brand"),
                model = m.optString("name"),
                variant = m.optString("brand"),
                year = 0,
                category = "",
                subcategory = "",
                propulsion = "",
                fuelType = "",
                aliases = m.optJSONArray("categories")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it) } } ?: emptyList(),
                recordPath = "",
            )
        }
    }

    private fun parseVehicleRecord(text: String): VehicleReferenceVehicleRecord {
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
            officialProductName = model?.optString("official_product_name") ?: "",
            variant = model?.optString("variant") ?: "",
            modelYear = model?.optInt("year") ?: 0,
            market = model?.optString("market") ?: "",
            category = root.optJSONObject("classification")?.optString("category") ?: "",
            subcategory = root.optJSONObject("classification")?.optString("subcategory") ?: "",
            propulsion = powertrain?.optString("propulsion") ?: "",
            fuelType = powertrain?.optString("fuel_type") ?: "",
            powerKw = powertrain.optDoubleOrNull("power"),
            engineDisplacementCc = powertrain.optDoubleOrNull("engine_displacement_cc"),
            torqueNm = powertrain.optDoubleOrNull("torque"),
            fuelTankLiters = storage.optDoubleOrNull("fuel_tank_liters"),
            batteryCapacityKwh = storage.optDoubleOrNull("battery_capacity_kwh"),
            batteryUsableKwh = storage.optDoubleOrNull("battery_usable_kwh"),
            batteryVoltageV = storage.optDoubleOrNull("battery_voltage_v"),
            batteryCapacityAh = storage.optDoubleOrNull("battery_capacity_ah"),
            seatingCapacity = dimensions.optIntOrNull("seating_capacity"),
            cargoCapacityLiters = dimensions.optDoubleOrNull("cargo_capacity_liters"),
            wheelCount = dimensions.optIntOrNull("wheel_count"),
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
        )
    }

    private fun parseManifest(text: String): VehicleReferenceManifest {
        val root = JSONObject(text)
        val categories = root.optJSONArray("category_counts") ?: JSONArray()
        val categorySlugs = mutableListOf<String>()
        for (i in 0 until categories.length()) {
            categorySlugs.add(categories.optString(i).replace('_', '-'))
        }
        return VehicleReferenceManifest(
            databaseName = root.optString("database_name"),
            databaseVersion = root.optString("database_version"),
            schemaVersion = root.optString("schema_version"),
            generatedAt = root.optString("generated_at"),
            phase = root.optString("phase"),
            totalRecords = root.optInt("total_records"),
            categorySlugs = categorySlugs,
        )
    }

    private fun VehicleReferenceCatalogIndex.toEntry(): VehicleReferenceCatalogEntry = VehicleReferenceCatalogEntry(
        id = id,
        manufacturer = manufacturerName,
        brand = brand,
        model = model,
        variant = variant,
        year = year,
        category = category,
        subcategory = subcategory,
        propulsion = propulsion,
        fuelType = fuelType,
        aliases = aliases,
        recordPath = recordPath,
    )

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
    )

    private fun JSONObject.toCatalogEntry(record: JSONObject): VehicleReferenceCatalogIndex =
        VehicleReferenceCatalogIndex(
            id = record.optString("id"),
            manufacturerName = record.optString("manufacturer_name"),
            brand = record.optString("brand"),
            model = record.optString("model"),
            variant = record.optString("variant"),
            year = record.optInt("year"),
            category = record.optString("category"),
            subcategory = record.optString("subcategory"),
            propulsion = record.optString("propulsion"),
            fuelType = record.optString("fuel_type"),
            aliases = record.optJSONArray("aliases")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it) }
            } ?: emptyList(),
            recordPath = record.optString("record_path"),
        )

    private fun JSONObject?.optDoubleOrNull(name: String): Double? =
        takeIf { this != null }?.optDouble(name)?.takeIf { !it.isNaN() }

    private fun JSONObject?.optIntOrNull(name: String): Int? =
        takeIf { this != null }?.optInt(name).takeIf { it != 0 }

    private fun JSONObject?.optStringOrNull(name: String): String? =
        takeIf { this != null }?.optString(name)?.takeIf { it.isNotBlank() }

    private fun readAsset(path: String): String =
        context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }

    private suspend fun fetchJson(url: String): String = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 6_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.doInput = true
            connection.doOutput = false
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode} fetching $url")
            }
            connection.inputStream.readBytes().toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }
}
