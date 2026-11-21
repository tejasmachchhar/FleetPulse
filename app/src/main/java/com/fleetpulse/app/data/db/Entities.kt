package com.fleetpulse.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Domain / Room models for FleetPulse.
 *
 * Field names and semantics deliberately mirror the original FleetPulse v4
 * JavaScript state (`vehicles`, `usageEntries`, `maintenanceEntries`,
 * `priceHistory`, `calibration`, `settings`) so that a JSON backup produced by
 * the web application can be restored without loss.
 */

const val SOURCE_ELECTRICITY = "Electricity"
const val SOURCE_PETROL = "Petrol"
const val SOURCE_DIESEL = "Diesel"
const val SOURCE_CNG = "CNG"
const val SOURCE_LPG = "LPG"
const val SOURCE_HYBRID = "Hybrid"

const val FILL_FULL_CHARGE = "Full charge"
const val FILL_PARTIAL_CHARGE = "Partial charge"
const val FILL_FULL_TANK = "Full tank"
const val FILL_PARTIAL_REFILL = "Partial refill"
const val FILL_RIDE_ONLY = "Ride only"

const val BASIS_MEASURED = "Measured"
const val BASIS_ESTIMATED = "Estimated"

const val STATUS_OK = "OK"
const val STATUS_CHECK = "CHECK"
const val STATUS_ERROR = "ERROR"

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val energySource: String,
    /** Universal vehicle taxonomy. Kept in the app database; future categories are additive. */
    val category: String = "two_wheeler",
    val subcategory: String = "",
    val trackingBasis: String = "distance_km",
    /** Reference-database identity for this vehicle, when populated. */
    val referenceRecordId: String? = null,
    val referenceDatabaseVersion: String? = null,
    val referenceRawJson: String? = null,
    /** User-authored specification overrides keyed by reference field path. */
    val referenceOverridesJson: String? = null,
    val regNo: String = "",
    val purchaseDate: String = "",
    val initialOdometer: Double = 0.0,
    val initialOperatingHours: Double? = null,
    val batteryCapacity: Double? = null,
    val batteryRate: Double? = null,
    val batteryLifeYears: Double? = null,
    val chargeCycles: Int? = null,
    val ratedKmPerCharge: Double? = null,
    val dailyKmPlan: Double? = null,
    val tankCapacity: Double? = null,
    val tankCapacityLiters: Double? = null,
    val cargoCapacityLiters: Double? = null,
    val seatingCapacity: Int? = null,
    val wheelCount: Int? = null,
    val axleConfiguration: String? = null,
    val powerKw: Double? = null,
    val engineDisplacementCc: Double? = null,
    val torqueNm: Double? = null,
    val engineOilGrade: String? = null,
    val color: String = "#2563EB",
    val notes: String = "",
    val archived: Boolean = false,
    val isSample: Boolean = false,
) {
    val isElectric: Boolean get() = energySource == SOURCE_ELECTRICITY
    val isOperatingHoursBasis: Boolean get() = trackingBasis == TRACKING_BASIS_OPERATING_HOURS
    val isDistanceBasis: Boolean get() = trackingBasis == TRACKING_BASIS_DISTANCE_KM || trackingBasis == TRACKING_BASIS_BOTH
}

const val TRACKING_BASIS_DISTANCE_KM = "distance_km"
const val TRACKING_BASIS_OPERATING_HOURS = "operating_hours"
const val TRACKING_BASIS_BOTH = "both"

@Entity(
    tableName = "usage_entries",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId"), Index("date")],
)
data class UsageEntry(
    @PrimaryKey val id: String,
    /** Local calendar date, ISO `yyyy-MM-dd` (never UTC-converted). */
    val date: String,
    val vehicleId: String,
    val odometer: Double?,
    val fillType: String,
    val energyAdded: Double? = null,
    val voltsBefore: Double? = null,
    val voltsAfter: Double? = null,
    val unitPrice: Double? = null,
    val remarks: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = 0L,
    /**
     * User-controlled participation in full-to-full mileage estimation.
     *
     * The entry (and its cost) is still stored and included in expenses. An
     * excluded entry contributes no energy/fuel to an interval denominator and
     * cannot start or end a full-to-full interval.
     */
    @ColumnInfo(defaultValue = "1")
    val includeInMileage: Boolean = true,
    /**
     * Distance used for distance-tracking vehicles. Kept separate from [odometer]
     * so agricultural machinery can report operation in hours while road vehicles
     * use km distances.
     */
    val distanceKm: Double? = null,
    /** Operating hours reported for hour-tracking vehicles (tractors, machinery). */
    val hoursUsed: Double? = null,
)

@Entity(
    tableName = "maintenance_entries",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId"), Index("date")],
)
data class MaintenanceEntry(
    @PrimaryKey val id: String,
    val date: String,
    val vehicleId: String,
    val odometer: Double,
    val category: String,
    val description: String,
    val amount: Double,
    val nextDueDate: String? = null,
    val nextDueKm: Double? = null,
    val remarks: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = 0L,
)

@Entity(tableName = "price_history", indices = [Index("energySource"), Index("effectiveFrom")])
data class PriceEntry(
    @PrimaryKey val id: String,
    val energySource: String,
    val effectiveFrom: String,
    val price: Double,
    val note: String = "",
    val isSample: Boolean = false,
)

@Entity(
    tableName = "calibration_points",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId")],
)
data class CalibrationPoint(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val voltage: Double,
    val percent: Double,
    val note: String = "",
    val isSample: Boolean = false,
)

/**
 * Single-row table (id = 1) holding the calculation / validation settings
 * that the web app kept inside `appState.settings`.
 */
@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val chargerEfficiency: Double = 89.0,
    val maxPlausibleDistance: Double = 250.0,
    val excludeImplausible: Boolean = true,
    val warnDays: Int = 14,
    val warnKm: Int = 200,
    val chartStartMonth: String = "2026-10",
    val evPlausibleMin: Double = 10.0,
    val evPlausibleMax: Double = 80.0,
    val petrolPlausibleMin: Double = 20.0,
    val petrolPlausibleMax: Double = 90.0,
    /** Newline separated custom + default maintenance categories. */
    val maintenanceCategories: String = DEFAULT_CATEGORY_TEXT,
)

val DEFAULT_CATEGORY_TEXT = listOf(
    "Routine service",
    "Repair",
    "Tyres",
    "Brakes",
    "Battery / Charger",
    "Chain / Drive",
    "Electricals",
    "Accessories",
    "Other",
).joinToString("\n")

fun SettingsEntity.categoryList(): List<String> =
    maintenanceCategories.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Small key/value table used for app-managed flags that must survive a
 * backup/restore (currently the schema/backup format marker).
 */
@Entity(tableName = "app_meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
