package com.fleetpulse.app.data.backup

import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.DEFAULT_CATEGORY_TEXT
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.EvalResult
import com.fleetpulse.app.support.BuildInfo
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

data class BackupPayload(
    val vehicles: List<Vehicle>,
    val usage: List<UsageEntry>,
    val maintenance: List<MaintenanceEntry>,
    val prices: List<PriceEntry>,
    val calibration: List<CalibrationPoint>,
    val settings: SettingsEntity,
)

data class BackupBuildInput(
    val vehicles: List<Vehicle>,
    val usage: List<UsageEntry>,
    val maintenance: List<MaintenanceEntry>,
    val prices: List<PriceEntry>,
    val calibration: List<CalibrationPoint>,
    val settings: SettingsEntity,
)

/**
 * Versioned, documented backup format.
 *
 * v2 (native):
 * {
 *   "format": "fleetpulse-backup", "version": 2, "exportedAt": ISO-8601,
 *   "data": { "vehicles": [...], "usageEntries": [...], "maintenanceEntries": [...],
 *             "priceHistory": [...], "calibration": [...], "settings": {...} }
 * }
 *
 * The original web app exported its raw state object
 * (top-level vehicles / usageEntries / maintenanceEntries / priceHistory /
 * calibration / settings). That layout is accepted on restore as well, so
 * existing FleetPulse v4 backups continue to work.
 */
object BackupManager {

    const val FORMAT = "fleetpulse-backup"
    const val VERSION = 3

    fun buildJson(input: BackupBuildInput): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", Instant.now().toString())
        root.put("app", "FleetPulse Android ${BuildInfo.VERSION_NAME}")

        val data = JSONObject()
        data.put("vehicles", JSONArray().apply { input.vehicles.forEach { put(vehicleJson(it)) } })
        data.put("usageEntries", JSONArray().apply { input.usage.forEach { put(usageJson(it)) } })
        data.put("maintenanceEntries", JSONArray().apply { input.maintenance.forEach { put(maintJson(it)) } })
        data.put("priceHistory", JSONArray().apply { input.prices.forEach { put(priceJson(it)) } })
        data.put("calibration", JSONArray().apply { input.calibration.forEach { put(calibrationJson(it)) } })
        data.put("settings", settingsJson(input.settings))
        root.put("data", data)
        return root.toString(2)
    }

    fun parse(json: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(json)
        val isNative = root.optString("format") == FORMAT
        val data = if (isNative) {
            val version = root.optInt("version", 0)
            require(version in 1..VERSION) { "Unsupported backup version: $version" }
            root.getJSONObject("data")
        } else {
            // Legacy web state export
            root
        }

        val vehiclesJson = data.optJSONArray("vehicles")
            ?: throw IllegalArgumentException("Backup is missing the vehicles list")
        val usageJson = data.optJSONArray("usageEntries")
            ?: throw IllegalArgumentException("Backup is missing the usage entries list")

        val vehicles = (0 until vehiclesJson.length()).map { i ->
            val o = vehiclesJson.getJSONObject(i)
            val id = o.optString("id").ifBlank { throw IllegalArgumentException("Vehicle #${i + 1} has no ID") }
            require(o.optString("energySource").isNotBlank()) {
                "Vehicle $id has an unknown energy source"
            }
            Vehicle(
                id = id,
                name = o.optString("name").ifBlank { id },
                type = o.optString("type", "Vehicle"),
                energySource = o.optString("energySource"),
                regNo = o.optString("regNo", ""),
                purchaseDate = o.optString("purchaseDate", ""),
                initialOdometer = o.optDouble("initialOdometer", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                initialOperatingHours = o.nullableDouble("initialOperatingHours"),
                batteryCapacity = o.nullableDouble("batteryCapacity"),
                batteryRate = o.nullableDouble("batteryRate"),
                batteryLifeYears = o.nullableDouble("batteryLifeYears"),
                chargeCycles = o.nullableInt("chargeCycles"),
                ratedKmPerCharge = o.nullableDouble("ratedKmPerCharge"),
                dailyKmPlan = o.nullableDouble("dailyKmPlan"),
                tankCapacity = o.nullableDouble("tankCapacity"),
                tankCapacityLiters = o.nullableDouble("tankCapacityLiters"),
                cargoCapacityLiters = o.nullableDouble("cargoCapacityLiters"),
                seatingCapacity = o.nullableInt("seatingCapacity"),
                wheelCount = o.nullableInt("wheelCount"),
                axleConfiguration = o.optString("axleConfiguration").takeIf { it.isNotBlank() },
                powerKw = o.nullableDouble("powerKw"),
                engineDisplacementCc = o.nullableDouble("engineDisplacementCc"),
                torqueNm = o.nullableDouble("torqueNm"),
                engineOilGrade = o.optString("engineOilGrade").takeIf { it.isNotBlank() },
                category = o.optString("category", "two_wheeler"),
                subcategory = o.optString("subcategory", ""),
                trackingBasis = o.optString("trackingBasis", "distance_km"),
                referenceRecordId = o.optString("referenceRecordId").takeIf { it.isNotBlank() },
                referenceDatabaseVersion = o.optString("referenceDatabaseVersion").takeIf { it.isNotBlank() },
                referenceRawJson = o.optString("referenceRawJson").takeIf { it.isNotBlank() },
                referenceOverridesJson = o.optString("referenceOverridesJson").takeIf { it.isNotBlank() },
                color = o.optString("color", "#2563EB"),
                notes = o.optString("notes", ""),
                archived = o.optBoolean("archived", false),
                isSample = o.optBoolean("isSample", false),
            )
        }
        require(vehicles.map { it.id }.toSet().size == vehicles.size) { "Duplicate vehicle IDs in backup" }
        val vehicleIds = vehicles.map { it.id }.toSet()

        val usage = (0 until usageJson.length()).mapNotNull { i ->
            val o = usageJson.optJSONObject(i)
                ?: throw IllegalArgumentException("Usage entry #${i + 1} is not an object")
            val id = o.optString("id").ifBlank { "u_${System.nanoTime()}_$i" }
            val vehicleId = o.optString("vehicleId")
            require(vehicleId in vehicleIds) {
                "Usage entry ${o.optString("date")} references a vehicle that is not in the backup: $vehicleId"
            }
            UsageEntry(
                id = id,
                date = o.optString("date"),
                vehicleId = vehicleId,
                odometer = o.nullableDouble("odometer"),
                distanceKm = o.nullableDouble("distanceKm") ?: 0.0,
                hoursUsed = o.nullableDouble("hoursUsed") ?: 0.0,
                fillType = o.optString("fillType", "Ride only"),
                energyAdded = o.nullableDouble("energyAdded"),
                voltsBefore = o.nullableDouble("voltsBefore"),
                voltsAfter = o.nullableDouble("voltsAfter"),
                unitPrice = o.nullableDouble("unitPrice"),
                remarks = o.optString("remarks", ""),
                isSample = o.optBoolean("isSample", false),
                createdAt = o.optLong("createdAt", i.toLong()),
                // Older backups predate this field; include by default.
                includeInMileage = o.optBoolean("includeInMileage", true),
            )
        }
        require(usage.map { it.id }.toSet().size == usage.size) { "Duplicate usage entry IDs in backup" }

        val maintJson = data.optJSONArray("maintenanceEntries") ?: JSONArray()
        val maintenance = (0 until maintJson.length()).mapNotNull { i ->
            val o = maintJson.optJSONObject(i)
                ?: throw IllegalArgumentException("Maintenance entry #${i + 1} is not an object")
            val vehicleId = o.optString("vehicleId")
            require(vehicleId in vehicleIds) {
                "Maintenance entry #${i + 1} references a vehicle that is not in the backup: $vehicleId"
            }
            MaintenanceEntry(
                id = o.optString("id").ifBlank { "m_${System.nanoTime()}_$i" },
                date = o.optString("date"),
                vehicleId = vehicleId,
                odometer = o.optDouble("odometer", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                category = o.optString("category", "Other"),
                description = o.optString("description", ""),
                amount = o.optDouble("amount", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                nextDueDate = o.optString("nextDueDate").takeIf { it.isNotBlank() && it != "null" },
                nextDueKm = o.nullableDouble("nextDueKm"),
                remarks = o.optString("remarks", ""),
                isSample = o.optBoolean("isSample", false),
                createdAt = o.optLong("createdAt", i.toLong()),
            )
        }
        require(maintenance.map { it.id }.toSet().size == maintenance.size) {
            "Duplicate maintenance entry IDs in backup"
        }

        val priceJson = data.optJSONArray("priceHistory") ?: JSONArray()
        val prices = (0 until priceJson.length()).mapNotNull { i ->
            val o = priceJson.optJSONObject(i)
                ?: throw IllegalArgumentException("Tariff #${i + 1} is not an object")
            val source = o.optString("energySource")
            require(source.isNotBlank()) { "Tariff #${i + 1} has an unknown energy source" }
            PriceEntry(
                id = o.optString("id").ifBlank { "p_${System.nanoTime()}_$i" },
                energySource = source,
                effectiveFrom = o.optString("effectiveFrom"),
                price = o.optDouble("price", 0.0),
                note = o.optString("note", ""),
                isSample = o.optBoolean("isSample", false),
            )
        }
        require(prices.map { it.id }.toSet().size == prices.size) { "Duplicate tariff IDs in backup" }

        val calJson = data.optJSONArray("calibration") ?: JSONArray()
        val calibration = (0 until calJson.length()).mapNotNull { i ->
            val o = calJson.optJSONObject(i)
                ?: throw IllegalArgumentException("Calibration point #${i + 1} is not an object")
            val vehicleId = o.optString("vehicleId")
            require(vehicleId in vehicleIds) {
                "Calibration point #${i + 1} references a vehicle that is not in the backup: $vehicleId"
            }
            CalibrationPoint(
                id = o.optString("id").ifBlank { "cal_${System.nanoTime()}_$i" },
                vehicleId = vehicleId,
                voltage = o.optDouble("voltage", 0.0),
                percent = o.optDouble("percent", 0.0),
                note = o.optString("note", ""),
                isSample = o.optBoolean("isSample", false),
            )
        }
        require(calibration.map { it.id }.toSet().size == calibration.size) {
            "Duplicate calibration point IDs in backup"
        }

        val s = data.optJSONObject("settings")
        val bounds = s?.optJSONObject("plausibleBounds")
        val evBounds = bounds?.optJSONObject("Electricity")
        val petBounds = bounds?.optJSONObject("Petrol")
        val categories = s?.optJSONArray("maintenanceCategories")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { c -> c.isNotBlank() } }
        }?.takeIf { it.isNotEmpty() } ?: DEFAULT_CATEGORY_TEXT.split("\n")

        val settings = SettingsEntity(
            id = 1,
            chargerEfficiency = s?.optDouble("chargerEfficiency", 89.0)?.takeIf { it != 0.0 } ?: 89.0,
            maxPlausibleDistance = s?.optDouble("maxPlausibleDistance", 250.0)?.takeIf { it > 0 } ?: 250.0,
            excludeImplausible = s?.optBoolean("excludeImplausible", true) ?: true,
            warnDays = s?.optInt("warnDays", 14) ?: 14,
            warnKm = s?.optInt("warnKm", 200) ?: 200,
            chartStartMonth = s?.optString("chartStartMonth")?.takeIf { it.length == 7 } ?: "2026-10",
            evPlausibleMin = evBounds?.optDouble("min", 10.0) ?: 10.0,
            evPlausibleMax = evBounds?.optDouble("max", 80.0) ?: 80.0,
            petrolPlausibleMin = petBounds?.optDouble("min", 20.0) ?: 20.0,
            petrolPlausibleMax = petBounds?.optDouble("max", 90.0) ?: 90.0,
            maintenanceCategories = categories.joinToString("\n"),
        )

        fun validDate(value: String): Boolean = runCatching { LocalDate.parse(value) }.isSuccess

        require(usage.all { validDate(it.date) }) { "Some usage entries have invalid dates" }
        require(maintenance.all { validDate(it.date) }) { "Some service entries have invalid dates" }
        require(maintenance.all { it.nextDueDate == null || validDate(it.nextDueDate!!) }) {
            "Some service reminders have invalid next-due dates"
        }
        require(prices.all { validDate(it.effectiveFrom) }) { "Some tariffs have invalid effective dates" }
        require(vehicles.all { it.purchaseDate.isBlank() || validDate(it.purchaseDate) }) {
            "Some vehicles have invalid purchase dates"
        }

        BackupPayload(vehicles, usage, maintenance, prices, calibration, settings)
    }

    // CSV exports ------------------------------------------------------
    fun usageCsv(result: EvalResult): String {
        val headers = listOf(
            "Date", "VehicleID", "VehicleName", "Odometer", "FillType", "VoltsBefore", "VoltsAfter",
            "PctBefore", "PctAfter", "Distance_km", "EnergyUsed", "Basis", "TotalCost_INR",
            "VerifiedEfficiency", "KmPerCharge", "IncludeInMileage", "Status", "CheckMessage", "Remarks",
        )
        val rows = result.evaluatedUsage.map { e ->
            listOf(
                e.entry.date,
                e.entry.vehicleId,
                e.vehicleName.csv(),
                e.entry.odometer?.toString() ?: "",
                e.entry.fillType.csv(),
                e.entry.voltsBefore?.toString() ?: "",
                e.entry.voltsAfter?.toString() ?: "",
                e.pctBefore?.let { Math.round(it).toString() + "%" } ?: "",
                e.pctAfter?.let { Math.round(it).toString() + "%" } ?: "",
                "%.1f".format(java.util.Locale.US, e.distance),
                "%.2f".format(java.util.Locale.US, e.energyUsed),
                e.basis ?: "",
                "%.2f".format(java.util.Locale.US, e.totalCost),
                e.stretchEfficiency?.let { "%.1f".format(java.util.Locale.US, it) } ?: "",
                e.kmPerCharge?.let { "%.1f".format(java.util.Locale.US, it) } ?: "",
                if (e.entry.includeInMileage) "Yes" else "No",
                e.status,
                e.checkMessage.csv(),
                e.entry.remarks.csv(),
            )
        }
        return buildString {
            appendLine(headers.joinToString(","))
            rows.forEach { appendLine(it.joinToString(",")) }
        }
    }

    fun maintenanceCsv(result: EvalResult): String {
        val headers = listOf(
            "Date", "VehicleID", "VehicleName", "Odometer", "Category", "Description",
            "Amount_INR", "NextDueDate", "NextDueKm", "DaysLeft", "KmLeft", "Status", "Remarks",
        )
        val rows = result.evaluatedMaintenance.map { m ->
            listOf(
                m.entry.date,
                m.entry.vehicleId,
                m.vehicleName.csv(),
                m.entry.odometer.toString(),
                m.entry.category.csv(),
                m.entry.description.csv(),
                m.entry.amount.toString(),
                m.entry.nextDueDate ?: "",
                m.entry.nextDueKm?.toString() ?: "",
                m.daysLeft?.toString() ?: "",
                m.kmLeft?.let { "%.1f".format(java.util.Locale.US, it) } ?: "",
                m.status,
                m.entry.remarks.csv(),
            )
        }
        return buildString {
            appendLine(headers.joinToString(","))
            rows.forEach { appendLine(it.joinToString(",")) }
        }
    }

    private fun String.csv(): String = "\"" + replace("\"", "\"\"") + "\""

    // JSON helpers -----------------------------------------------------
    private fun vehicleJson(v: Vehicle) = JSONObject().apply {
        put("id", v.id); put("name", v.name); put("type", v.type)
        put("energySource", v.energySource); put("regNo", v.regNo)
        put("purchaseDate", v.purchaseDate); put("initialOdometer", v.initialOdometer)
        put("initialOperatingHours", v.initialOperatingHours ?: JSONObject.NULL)
        put("batteryCapacity", v.batteryCapacity ?: JSONObject.NULL)
        put("batteryRate", v.batteryRate ?: JSONObject.NULL)
        put("batteryLifeYears", v.batteryLifeYears ?: JSONObject.NULL)
        put("chargeCycles", v.chargeCycles ?: JSONObject.NULL)
        put("ratedKmPerCharge", v.ratedKmPerCharge ?: JSONObject.NULL)
        put("dailyKmPlan", v.dailyKmPlan ?: JSONObject.NULL)
        put("tankCapacity", v.tankCapacity ?: JSONObject.NULL)
        put("tankCapacityLiters", v.tankCapacityLiters ?: JSONObject.NULL)
        put("cargoCapacityLiters", v.cargoCapacityLiters ?: JSONObject.NULL)
        put("seatingCapacity", v.seatingCapacity ?: JSONObject.NULL)
        put("wheelCount", v.wheelCount ?: JSONObject.NULL)
        put("axleConfiguration", v.axleConfiguration ?: JSONObject.NULL)
        put("powerKw", v.powerKw ?: JSONObject.NULL)
        put("engineDisplacementCc", v.engineDisplacementCc ?: JSONObject.NULL)
        put("torqueNm", v.torqueNm ?: JSONObject.NULL)
        put("engineOilGrade", v.engineOilGrade ?: JSONObject.NULL)
        put("category", v.category); put("subcategory", v.subcategory); put("trackingBasis", v.trackingBasis)
        put("referenceRecordId", v.referenceRecordId ?: JSONObject.NULL)
        put("referenceDatabaseVersion", v.referenceDatabaseVersion ?: JSONObject.NULL)
        put("referenceRawJson", v.referenceRawJson ?: JSONObject.NULL)
        put("referenceOverridesJson", v.referenceOverridesJson ?: JSONObject.NULL)
        put("color", v.color); put("notes", v.notes)
        put("archived", v.archived); put("isSample", v.isSample)
    }

    private fun usageJson(u: UsageEntry) = JSONObject().apply {
        put("id", u.id); put("date", u.date); put("vehicleId", u.vehicleId)
        put("odometer", u.odometer ?: JSONObject.NULL)
        put("distanceKm", u.distanceKm ?: JSONObject.NULL)
        put("hoursUsed", u.hoursUsed ?: JSONObject.NULL)
        put("fillType", u.fillType)
        put("energyAdded", u.energyAdded ?: JSONObject.NULL)
        put("voltsBefore", u.voltsBefore ?: JSONObject.NULL)
        put("voltsAfter", u.voltsAfter ?: JSONObject.NULL)
        put("unitPrice", u.unitPrice ?: JSONObject.NULL)
        put("includeInMileage", u.includeInMileage)
        put("remarks", u.remarks); put("isSample", u.isSample); put("createdAt", u.createdAt)
    }

    private fun maintJson(m: MaintenanceEntry) = JSONObject().apply {
        put("id", m.id); put("date", m.date); put("vehicleId", m.vehicleId)
        put("odometer", m.odometer); put("category", m.category)
        put("description", m.description); put("amount", m.amount)
        put("nextDueDate", m.nextDueDate ?: JSONObject.NULL)
        put("nextDueKm", m.nextDueKm ?: JSONObject.NULL)
        put("remarks", m.remarks); put("isSample", m.isSample); put("createdAt", m.createdAt)
    }

    private fun priceJson(p: PriceEntry) = JSONObject().apply {
        put("id", p.id); put("energySource", p.energySource); put("effectiveFrom", p.effectiveFrom)
        put("price", p.price); put("note", p.note); put("isSample", p.isSample)
    }

    private fun calibrationJson(c: CalibrationPoint) = JSONObject().apply {
        put("id", c.id); put("vehicleId", c.vehicleId); put("voltage", c.voltage)
        put("percent", c.percent); put("note", c.note); put("isSample", c.isSample)
    }

    private fun settingsJson(s: SettingsEntity) = JSONObject().apply {
        put("chargerEfficiency", s.chargerEfficiency)
        put("maxPlausibleDistance", s.maxPlausibleDistance)
        put("excludeImplausible", s.excludeImplausible)
        put("warnDays", s.warnDays); put("warnKm", s.warnKm)
        put("chartStartMonth", s.chartStartMonth)
        put("plausibleBounds", JSONObject().apply {
            put("Electricity", JSONObject().apply {
                put("unit", "kWh"); put("min", s.evPlausibleMin); put("max", s.evPlausibleMax)
            })
            put("Petrol", JSONObject().apply {
                put("unit", "L"); put("min", s.petrolPlausibleMin); put("max", s.petrolPlausibleMax)
            })
        })
        put("maintenanceCategories", JSONArray(s.maintenanceCategories.split("\n")))
    }

    private fun JSONObject.nullableDouble(key: String): Double? {
        if (isNull(key)) return null
        if (!has(key)) return null
        val d = optDouble(key, Double.NaN)
        return if (d.isNaN()) null else d
    }

    private fun JSONObject.nullableInt(key: String): Int? {
        if (isNull(key)) return null
        if (!has(key)) return null
        val d = optDouble(key, Double.NaN)
        return if (d.isNaN()) null else d.toInt()
    }
}
