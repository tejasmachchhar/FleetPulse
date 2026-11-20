package com.fleetpulse.app.ui.screens

import android.content.Context
import org.json.JSONObject
import org.json.JSONTokener

data class VehicleCategoryOption(
    val id: String,
    val name: String,
    val slug: String,
    val trackingBasis: String,
    val subcategories: List<String>,
)

data class VehicleSubcategoryOption(
    val id: String,
    val name: String,
)

fun Context.loadVehicleTaxonomy(): List<VehicleCategoryOption> {
    return assets.open("vehicle-reference/taxonomy/categories.json").use { stream ->
        val root = JSONObject(JSONTokener(String(stream.readBytes(), Charsets.UTF_8)))
        val categories = root.optJSONArray("categories") ?: org.json.JSONArray()
        (0 until categories.length()).mapNotNull { i ->
            val category = categories.optJSONObject(i) ?: return@mapNotNull null
            val subcategories = category.optJSONArray("subcategories") ?: org.json.JSONArray()
            VehicleCategoryOption(
                id = category.optString("id"),
                name = category.optString("name"),
                slug = category.optString("slug"),
                trackingBasis = category.optString("tracking_basis"),
                subcategories = (0 until subcategories.length()).map { subcategories.optString(it) },
            )
        }
    }
}

fun VehicleCategoryOption.toSubcategories(indexEntries: List<com.fleetpulse.app.data.repo.VehicleReferenceCatalogEntry>): List<VehicleSubcategoryOption> {
    val seen = LinkedHashMap<String, String>()
    indexEntries.forEach { seen[it.subcategory] = it.subcategory.replace('_', ' ').replaceFirstChar { c -> c.uppercase() } }
    return seen.map { VehicleSubcategoryOption(id = it.key, name = it.value) }
}
