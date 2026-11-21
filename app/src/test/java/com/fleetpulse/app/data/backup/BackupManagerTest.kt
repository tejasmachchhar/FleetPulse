package com.fleetpulse.app.data.backup

import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.domain.SampleData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private fun sampleInput() = BackupBuildInput(
        vehicles = SampleData.vehicles(),
        usage = SampleData.usageEntries(),
        maintenance = SampleData.maintenanceEntries(),
        prices = SampleData.priceHistory(),
        calibration = SampleData.calibration(),
        settings = SampleData.settings(),
    )

    private fun sampleNativeJson(): String = BackupManager.buildJson(sampleInput())

    private fun dataOf(json: String): JSONObject = JSONObject(json).getJSONObject("data")

    @Test
    fun `native v2 round trip preserves every collection`() {
        val original = sampleInput()
        val parsed = BackupManager.parse(sampleNativeJson()).getOrThrow()

        assertEquals(original.vehicles.size, parsed.vehicles.size)
        assertEquals(original.usage.size, parsed.usage.size)
        assertEquals(original.maintenance.size, parsed.maintenance.size)
        assertEquals(original.prices.size, parsed.prices.size)
        assertEquals(original.calibration.size, parsed.calibration.size)
        assertTrue(parsed.usage.all { it.isSample })
        assertTrue(parsed.usage.all { it.includeInMileage })
        assertTrue(parsed.vehicles.all { it.isSample })
        assertEquals("Demo EV 1", parsed.vehicles.first().name)
        assertEquals(3.12, parsed.vehicles.first().batteryCapacity ?: 0.0, 0.0001)
        assertEquals(89.0, parsed.settings.chargerEfficiency, 0.0001)
        assertEquals("2026-10", parsed.settings.chartStartMonth)
        assertEquals(original.settings.maintenanceCategories, parsed.settings.maintenanceCategories)
    }

    @Test
    fun `mileage eligibility is preserved and legacy backups default to included`() {
        val data = dataOf(sampleNativeJson())
        data.getJSONArray("usageEntries").getJSONObject(0).put("includeInMileage", false)
        val parsed = BackupManager.parse(data.toString()).getOrThrow()
        assertFalse(parsed.usage.first().includeInMileage)
        assertTrue(parsed.usage.drop(1).all { it.includeInMileage })

        val legacy = dataOf(sampleNativeJson())
        legacy.getJSONArray("usageEntries").getJSONObject(0).remove("includeInMileage")
        val legacyParsed = BackupManager.parse(legacy.toString()).getOrThrow()
        assertTrue(legacyParsed.usage.first().includeInMileage)
    }

    @Test
    fun `legacy raw v4 state is accepted`() {
        val rawState = dataOf(sampleNativeJson()).toString()
        val parsed = BackupManager.parse(rawState).getOrThrow()
        assertEquals(3, parsed.vehicles.size)
        assertTrue(parsed.vehicles.any { it.name == "Demo Petrol" })
        assertEquals(89.0, parsed.settings.chargerEfficiency, 0.0001)
    }

    @Test
    fun `malformed json is rejected`() {
        assertTrue(BackupManager.parse("{not valid json").isFailure)
        assertTrue(BackupManager.parse("[]").isFailure)
    }

    @Test
    fun `usage referencing a missing vehicle is rejected instead of silently dropped`() {
        val data = dataOf(sampleNativeJson())
        data.getJSONArray("usageEntries").getJSONObject(0).put("vehicleId", "Vh-missing")
        val result = BackupManager.parse(data.toString())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("not in the backup"))
    }

    @Test
    fun `duplicate vehicle ids are rejected`() {
        val data = dataOf(sampleNativeJson())
        val vehicles = data.getJSONArray("vehicles")
        vehicles.getJSONObject(1).put("id", vehicles.getJSONObject(0).getString("id"))
        assertTrue(BackupManager.parse(data.toString()).isFailure)
    }

    @Test
    fun `invalid dates are rejected`() {
        val data = dataOf(sampleNativeJson())
        data.getJSONArray("usageEntries").getJSONObject(0).put("date", "2026-02-30")
        val result = BackupManager.parse(data.toString())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("invalid dates"))
    }

    @Test
    fun `empty dataset with settings is valid`() {
        val parsed = BackupManager.parse(
            BackupManager.buildJson(
                BackupBuildInput(
                    vehicles = emptyList(),
                    usage = emptyList(),
                    maintenance = emptyList(),
                    prices = emptyList(),
                    calibration = emptyList(),
                    settings = SettingsEntity(),
                ),
            ),
        ).getOrThrow()
        assertTrue(parsed.vehicles.isEmpty())
        assertTrue(parsed.usage.isEmpty())
        assertEquals(SettingsEntity().chartStartMonth, parsed.settings.chartStartMonth)
    }

    @Test
    fun `blank energy source is rejected`() {
        val data = dataOf(sampleNativeJson())
        data.getJSONArray("vehicles").getJSONObject(0).put("energySource", "")
        assertFalse(BackupManager.parse(data.toString()).isSuccess)
    }

    @Test
    fun `v3 backup preserves universal vehicle and hours fields`() {
        val input = BackupBuildInput(
            vehicles = listOf(
                Vehicle(
                    id = "v1",
                    name = "Tractor",
                    type = "Agricultural Tractor",
                    energySource = SOURCE_PETROL,
                    category = "agricultural",
                    subcategory = "agricultural_tractor",
                    trackingBasis = "operating_hours",
                    initialOperatingHours = 12.0,
                    referenceRecordId = "rec1",
                    referenceDatabaseVersion = "1.0.0-batch1",
                    referenceRawJson = "{\"id\":\"rec1\"}",
                    referenceOverridesJson = "{\"powertrain.power\": 42.0}",
                    powerKw = 42.0,
                    engineDisplacementCc = 2979.0,
                    torqueNm = 207.0,
                    engineOilGrade = "SAE 15W-40",
                ),
            ),
            usage = listOf(
                UsageEntry(
                    id = "u1",
                    date = "2026-11-20",
                    vehicleId = "v1",
                    odometer = 0.0,
                    hoursUsed = 3.5,
                    fillType = FILL_FULL_TANK,
                    unitPrice = 100.0,
                    distanceKm = 0.0,
                ),
            ),
            maintenance = emptyList(),
            prices = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val parsed = BackupManager.parse(BackupManager.buildJson(input)).getOrThrow()
        val v = parsed.vehicles.single()
        assertEquals("agricultural", v.category)
        assertEquals("operating_hours", v.trackingBasis)
        assertEquals(12.0, v.initialOperatingHours)
        assertEquals("rec1", v.referenceRecordId)
        assertEquals("1.0.0-batch1", v.referenceDatabaseVersion)
        assertEquals(42.0, v.powerKw)
        val u = parsed.usage.single()
        assertEquals(3.5, u.hoursUsed)
        assertEquals(0.0, u.distanceKm)
    }

    @Test
    fun `legacy v2 backup imports with defaults`() {
        val data = dataOf(sampleNativeJson())
        data.put("version", 2)
        val parsed = BackupManager.parse(data.toString()).getOrThrow()
        assertTrue(parsed.vehicles.isNotEmpty())
        val v = parsed.vehicles.first()
        assertEquals("two_wheeler", v.category)
        assertEquals("distance_km", v.trackingBasis)
        assertTrue(parsed.usage.isNotEmpty())
        val u = parsed.usage.first()
        assertEquals(0.0, u.distanceKm)
        assertEquals(0.0, u.hoursUsed)
        assertTrue(parsed.vehicles.all { it.category.isNotBlank() })
        assertTrue(parsed.usage.all { it.hoursUsed != null && it.distanceKm != null })
    }
}
