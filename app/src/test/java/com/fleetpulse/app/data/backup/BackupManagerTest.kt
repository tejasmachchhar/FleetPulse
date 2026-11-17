package com.fleetpulse.app.data.backup

import com.fleetpulse.app.data.db.SettingsEntity
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
    fun `unknown energy source is rejected`() {
        val data = dataOf(sampleNativeJson())
        data.getJSONArray("vehicles").getJSONObject(0).put("energySource", "Hydrogen")
        assertFalse(BackupManager.parse(data.toString()).isSuccess)
    }
}
