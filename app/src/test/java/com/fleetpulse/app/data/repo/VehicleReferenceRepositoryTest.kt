package com.fleetpulse.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleReferenceRepositoryTest {

    @Test
    fun `manifest parses object category counts`() {
        val json = """
            {
              "database_name": "FleetPulse Universal Vehicle Reference Database",
              "database_version": "1.0.0-batch1",
              "schema_version": "1.0.0",
              "generated_at": "2026-11-20",
              "phase": "Phase 1 — Batch 1",
              "total_records": 30,
              "category_counts": {
                "two-wheelers": 8,
                "agricultural": 5
              }
            }
        """.trimIndent()
        val manifest = VehicleReferenceParser.parseManifest(json)
                assertEquals("1.0.0-batch1", manifest.databaseVersion)
        assertEquals(30, manifest.totalRecords)
        assertEquals(setOf("two-wheelers", "agricultural"), manifest.categoryKeys.toSet())
        assertEquals(setOf("two-wheelers", "agricultural"), manifest.categorySlugs.toSet())
    }

    @Test
    fun `index parse returns usable entries`() {
        val json = """
            {
              "records": [
                {
                  "id": "honda_activa",
                  "manufacturer_name": "Honda",
                  "brand": "Honda",
                  "model": "Activa",
                  "variant": "Activa 110 DLX",
                  "year": 2025,
                  "category": "two_wheeler",
                  "subcategory": "petrol_scooter",
                  "propulsion": "ice",
                  "fuel_type": "petrol",
                  "aliases": ["Activa 6G"],
                  "record_path": "vehicles/two-wheelers/honda_hmsi/honda_activa.json"
                }
              ]
            }
        """.trimIndent()
        val entries = VehicleReferenceParser.parseIndex(json)
        assertEquals(1, entries.size)
        assertEquals("honda_activa", entries.single().id)
        assertEquals("Activa 110 DLX", entries.single().variant)
    }

    @Test
    fun `record parse maps spec fields`() {
        val json = """
            {
              "id": "mahindra_tractor",
              "manufacturer": { "id": "mahindra_tractors", "name": "Mahindra Tractors", "brand": "Mahindra Tractor" },
              "model": { "name": "575 DI XP Plus", "official_product_name": "Mahindra 575 DI XP Plus", "variant": "2WD", "year": 2025, "market": "IN" },
              "classification": { "category": "agricultural", "subcategory": "agricultural_tractor" },
              "powertrain": { "propulsion": "ice", "fuel_type": "diesel", "power": 35.0, "torque": 207.0 },
              "energy_storage": { "fuel_tank_liters": null, "battery_capacity_kwh": null },
              "dimensions": { "seating_capacity": 1, "wheel_count": 4, "axle_configuration": "4x2" },
              "maintenance": { "engine_oil_grade": "SAE 15W-40" },
              "sources": [ { "publisher": "Mahindra", "title": "Spec", "url": "https://mahindra.example/spec", "source_type": "official_manufacturer" } ],
              "verification": { "status": "cross_verified" }
            }
        """.trimIndent()
        val record = VehicleReferenceParser.parseVehicleRecord(json)
        assertEquals("diesel", record.fuelType)
        assertEquals(35.0, record.powerKw!!, 0.0)
        assertEquals(207.0, record.torqueNm!!, 0.0)
        assertEquals("SAE 15W-40", record.engineOilGrade)
        assertEquals("https://mahindra.example/spec", record.sources.single().url)
    }

    @Test
    fun `manufacturer parse handles missing categories`() {
        val json = """
            {
              "manufacturers": [
                { "id": "example", "name": "Example", "brand": "Example", "categories": ["agricultural"] }
              ]
            }
        """.trimIndent()
        val manufacturers = VehicleReferenceParser.parseManufacturers(json)
        assertEquals(1, manufacturers.size)
        assertEquals(listOf("agricultural"), manufacturers.single().categories)
    }


}
