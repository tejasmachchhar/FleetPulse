package com.fleetpulse.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY id")
    suspend fun getAll(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: String): Vehicle?

    @Upsert
    suspend fun upsert(vehicle: Vehicle)

    @Upsert
    suspend fun upsertAll(vehicles: List<Vehicle>)

    @Query("UPDATE vehicles SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()

    @Query("DELETE FROM vehicles WHERE isSample = 1 AND id NOT IN (SELECT DISTINCT vehicleId FROM usage_entries WHERE isSample = 0 UNION SELECT DISTINCT vehicleId FROM maintenance_entries WHERE isSample = 0)")
    suspend fun deleteSampleVehiclesWithoutUserData()

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<UsageEntry>>

    @Query("SELECT * FROM usage_entries ORDER BY date ASC, createdAt ASC")
    suspend fun getAll(): List<UsageEntry>

    @Query("SELECT * FROM usage_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, createdAt DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<UsageEntry>>

    @Upsert
    suspend fun upsert(entry: UsageEntry)

    @Upsert
    suspend fun upsertAll(entries: List<UsageEntry>)

    @Query("DELETE FROM usage_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM usage_entries WHERE isSample = 1")
    suspend fun deleteSamples()

    @Query("SELECT COUNT(*) FROM usage_entries WHERE isSample = 1")
    suspend fun sampleCount(): Int

    @Query("DELETE FROM usage_entries")
    suspend fun deleteAll()
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<MaintenanceEntry>>

    @Query("SELECT * FROM maintenance_entries ORDER BY date ASC, createdAt ASC")
    suspend fun getAll(): List<MaintenanceEntry>

    @Upsert
    suspend fun upsert(entry: MaintenanceEntry)

    @Upsert
    suspend fun upsertAll(entries: List<MaintenanceEntry>)

    @Query("DELETE FROM maintenance_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM maintenance_entries WHERE isSample = 1")
    suspend fun deleteSamples()

    @Query("SELECT COUNT(*) FROM maintenance_entries WHERE isSample = 1")
    suspend fun sampleCount(): Int

    @Query("DELETE FROM maintenance_entries")
    suspend fun deleteAll()
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM price_history ORDER BY effectiveFrom ASC")
    fun observeAll(): Flow<List<PriceEntry>>

    @Query("SELECT * FROM price_history ORDER BY effectiveFrom ASC")
    suspend fun getAll(): List<PriceEntry>

    @Upsert
    suspend fun upsert(entry: PriceEntry)

    @Upsert
    suspend fun upsertAll(entries: List<PriceEntry>)

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM price_history WHERE isSample = 1")
    suspend fun deleteSamples()

    @Query("DELETE FROM price_history")
    suspend fun deleteAll()
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration_points ORDER BY vehicleId, voltage ASC")
    fun observeAll(): Flow<List<CalibrationPoint>>

    @Query("SELECT * FROM calibration_points ORDER BY vehicleId, voltage ASC")
    suspend fun getAll(): List<CalibrationPoint>

    @Upsert
    suspend fun upsert(point: CalibrationPoint)

    @Upsert
    suspend fun upsertAll(points: List<CalibrationPoint>)

    @Query("DELETE FROM calibration_points WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM calibration_points WHERE isSample = 1")
    suspend fun deleteSamples()

    @Query("DELETE FROM calibration_points")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM app_meta WHERE `key` = :key")
    suspend fun get(key: String): MetaEntity?

    @Upsert
    suspend fun upsert(meta: MetaEntity)

    @Query("DELETE FROM app_meta")
    suspend fun clear()
}

@Database(
    entities = [
        Vehicle::class,
        UsageEntry::class,
        MaintenanceEntry::class,
        PriceEntry::class,
        CalibrationPoint::class,
        SettingsEntity::class,
        MetaEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FleetDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun usageDao(): UsageDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun priceDao(): PriceDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun settingsDao(): SettingsDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "fleetpulse.db"

        /**
         * Universal vehicle support. Existing EV/bike data is preserved; new
         * columns default to the legacy two-wheeler/petrol assumptions.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN category TEXT NOT NULL DEFAULT 'two_wheeler'")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN trackingBasis TEXT NOT NULL DEFAULT 'distance_km'")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN referenceRecordId TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN referenceDatabaseVersion TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN referenceRawJson TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN referenceOverridesJson TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN tankCapacityLiters REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN cargoCapacityLiters REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN seatingCapacity INTEGER")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN wheelCount INTEGER")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN axleConfiguration TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN powerKw REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN initialOperatingHours REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN engineDisplacementCc REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN torqueNm REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN engineOilGrade TEXT")
                db.execSQL("ALTER TABLE usage_entries ADD COLUMN distanceKm REAL")
                db.execSQL("ALTER TABLE usage_entries ADD COLUMN hoursUsed REAL")
            }
        }

        /**
         * Adds the per-entry mileage-eligibility flag. Existing records are
         * preserved and default to included, exactly matching pre-upgrade
         * calculation behaviour.
         */
        /**
         * Persists the wizard-collected starting operating hours for agricultural
         * and construction vehicles. Existing users default to null and continue
         * to work without changing behaviour.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN initialOperatingHours REAL")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE usage_entries ADD COLUMN includeInMileage INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}
