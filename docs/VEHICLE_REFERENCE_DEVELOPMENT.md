# Vehicle Reference Database Development

This document describes how FleetPulse integrates the Vehicle Reference Database and how future batches are added without hardcoded app changes.

## Location

- Database: `vehicle-reference/`
- Manifest: `vehicle-reference/manifest.json`
- Taxonomy: `vehicle-reference/taxonomy/categories.json`
- Category indexes: `vehicle-reference/indexes/<category>.json`
- Manufacturer index: `vehicle-reference/manufacturers/index.json`
- Vehicle records: `vehicle-reference/vehicles/<category>/<manufacturer>/<id>.json`
- Validation script: `vehicle-reference/scripts/validate_database.py`

## App integration

- `VehicleReferenceRepository` is the only app client for the reference database.
- Android flow: fetch `manifest.json`, fetch the selected category index, then fetch one vehicle record.
- The repository caches manifest/index/record JSON in DataStore; a local asset copy in `app/src/main/assets/vehicle-reference/` is the offline fallback.
- No network credentials or secrets are used.

## Adding a future batch

1. Add new vehicle records under `vehicle-reference/vehicles/...`.
2. Update category/manufacturer/taxonomy indexes.
3. Update `vehicle-reference/manifest.json` with new `database_version`, `schema_version`, `total_records`, and `total_manufacturers`.
4. Run `python3 vehicle-reference/scripts/validate_database.py`.
5. If the schema changes, update `VehicleReferenceRepository` and `Vehicle`/`UsageEntry` mappings additively; do not remove existing fields.
6. If new categories/subcategories are added, the app’s category selector uses `taxonomy/categories.json`, so no app-code change is required.

## Rules

- User edits always win: the app stores `Vehicle.referenceOverridesJson`.
- Do not overwrite user data during database updates.
- Do not commit generated logs or sensitive personal information.
- Reference records are seed data; users can edit or delete them like normal vehicles.
