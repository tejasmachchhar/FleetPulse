# Database Schema

FleetPulse stores its authoritative data in a local Room database named `fleetpulse.db`. Small UI/onboarding preferences are stored separately in DataStore Preferences. There is no server database and no automatic cloud backup.

## Room database

- Database class: `com.fleetpulse.app.data.db.FleetDatabase`
- Database name: `fleetpulse.db`
- Current version: **2**
- Schema export: enabled (`exportSchema = true`)
- Exported schemas: `app/schemas/com.fleetpulse.app.data.db.FleetDatabase/1.json` and `2.json`
- KSP schema location: `app/schemas`
- Foreign keys: enabled by Room for declared relations
- Destructive migration on **downgrade** only: `fallbackToDestructiveMigrationOnDowngrade()`
- Upgrade migration: `MIGRATION_1_2`

`app/schemas/` is versioned source. It must remain tracked so schema changes can be reviewed. The build writes new schema JSON files through the KSP argument:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

## Entity relationship overview

```text
vehicles (1) ──< usage_entries
    │
    ├──< maintenance_entries
    │
    └──< calibration_points

price_history       (independent, matched by energySource + date)
app_settings        (single row, id = 1)
app_meta            (independent key/value metadata)
```

`usage_entries`, `maintenance_entries`, and `calibration_points` reference `vehicles.id` with `ON DELETE CASCADE`. Deleting a vehicle therefore removes its dependent records. The UI protects this operation by requiring confirmation and by keeping at least one vehicle in the fleet.

## Tables

### `vehicles`

| Column | SQLite type | Null | Notes |
|---|---|---:|---|
| `id` | `TEXT` | no | Primary key; user-visible stable ID such as `Vh1`; new IDs are suggested in sequence |
| `name` | `TEXT` | no | Display name |
| `type` | `TEXT` | no | Free-text type, such as `Electric Bike` or `Petrol Bike` |
| `energySource` | `TEXT` | no | `Electricity` or `Petrol` |
| `regNo` | `TEXT` | no | Registration or blank; demo uses `DEMO-0001` |
| `purchaseDate` | `TEXT` | no | ISO `yyyy-MM-dd`, or blank |
| `initialOdometer` | `REAL` | no | Odometer when tracking began |
| `batteryCapacity` | `REAL` | yes | Usable kWh for an EV |
| `batteryRate` | `REAL` | yes | Replacement rate per kWh |
| `batteryLifeYears` | `REAL` | yes | Expected battery life in years |
| `chargeCycles` | `INTEGER` | yes | Expected full-charge cycles |
| `ratedKmPerCharge` | `REAL` | yes | Manufacturer/rated km per charge |
| `dailyKmPlan` | `REAL` | yes | Planned daily km for age-capacity modelling |
| `tankCapacity` | `REAL` | yes | Petrol tank capacity in litres |
| `color` | `TEXT` | no | Hex colour used in the UI |
| `notes` | `TEXT` | no | Free-text notes |
| `archived` | `INTEGER` | no | Boolean stored as 0/1 |
| `isSample` | `INTEGER` | no | Boolean stored as 0/1; sample separation |

No secondary indices are declared. Queries order by `id`.

`Vehicle.isElectric` is a computed Kotlin property comparing `energySource` with `Electricity`; it is not a database column.

### `usage_entries`

| Column | SQLite type | Null | Default | Notes |
|---|---|---:|---|---|
| `id` | `TEXT` | no | — | Primary key; new entries use a UUID |
| `date` | `TEXT` | no | — | Local ISO `yyyy-MM-dd`; never UTC-converted |
| `vehicleId` | `TEXT` | no | — | FK to `vehicles.id`, cascade delete |
| `odometer` | `REAL` | yes | — | Reading in km; missing is ERROR |
| `fillType` | `TEXT` | no | — | `Full charge`, `Partial charge`, `Full tank`, `Partial refill`, `Ride only` |
| `energyAdded` | `REAL` | yes | — | Metered wall kWh or petrol litres |
| `voltsBefore` | `REAL` | yes | — | EV voltage before charging |
| `voltsAfter` | `REAL` | yes | — | EV voltage after a partial charge |
| `unitPrice` | `REAL` | yes | — | Explicit ₹/kWh or ₹/L; overrides tariff |
| `remarks` | `TEXT` | no | — | Free text; demo rows carry `[SAMPLE]` |
| `isSample` | `INTEGER` | no | — | Boolean 0/1 |
| `createdAt` | `INTEGER` | no | — | Epoch millis; tie-breaker for same-date ordering |
| `includeInMileage` | `INTEGER` | no | `1` | Boolean 0/1; eligibility for full-to-full mileage |

Indices:

- `index_usage_entries_vehicleId` on `vehicleId`
- `index_usage_entries_date` on `date`

Foreign key: `vehicleId` → `vehicles.id` on delete cascade.

The `includeInMileage` column is the v1→v2 addition. It controls energy/fuel participation and boundary eligibility in the full-to-full calculation, not deletion or expense inclusion. The odometer distance between readings remains real travel and contributes to a stretch.

### `maintenance_entries`

| Column | SQLite type | Null | Notes |
|---|---|---:|---|
| `id` | `TEXT` | no | Primary key; new entries use a UUID |
| `date` | `TEXT` | no | ISO `yyyy-MM-dd` |
| `vehicleId` | `TEXT` | no | FK to `vehicles.id`, cascade delete |
| `odometer` | `REAL` | no | Odometer at service |
| `category` | `TEXT` | no | Category used for reminder retirement |
| `description` | `TEXT` | no | Work performed |
| `amount` | `REAL` | no | Cost in ₹ |
| `nextDueDate` | `TEXT` | yes | Next due date |
| `nextDueKm` | `REAL` | yes | Next due odometer |
| `remarks` | `TEXT` | no | Free text |
| `isSample` | `INTEGER` | no | Boolean 0/1 |
| `createdAt` | `INTEGER` | no | Epoch millis; tie-breaker |

Indices:

- `index_maintenance_entries_vehicleId`
- `index_maintenance_entries_date`

Foreign key: `vehicleId` → `vehicles.id` on delete cascade.

A record has a reminder if either `nextDueDate` or `nextDueKm` is non-null. The latest record per `(vehicleId, category)` is the only one whose reminder remains pending; older reminders are reported as `Completed`.

### `price_history`

| Column | SQLite type | Null | Notes |
|---|---|---:|---|
| `id` | `TEXT` | no | Primary key |
| `energySource` | `TEXT` | no | `Electricity` or `Petrol` |
| `effectiveFrom` | `TEXT` | no | ISO date from which the price applies |
| `price` | `REAL` | no | ₹/kWh or ₹/L |
| `note` | `TEXT` | no | Free text |
| `isSample` | `INTEGER` | no | Boolean 0/1 |

Indices:

- `index_price_history_energySource`
- `index_price_history_effectiveFrom`

No foreign key. Prices are historical: the effective price for an entry is the latest row for the same source whose `effectiveFrom <= entry.date`. Adding a newer tariff does not rewrite older entries. Sample tariffs are retained while genuine usage still depends on them, so removal of sample data does not change historical costs.

### `calibration_points`

| Column | SQLite type | Null | Notes |
|---|---|---:|---|
| `id` | `TEXT` | no | Primary key |
| `vehicleId` | `TEXT` | no | FK to `vehicles.id`, cascade delete |
| `voltage` | `REAL` | no | Pack voltage |
| `percent` | `REAL` | no | Battery percentage at that voltage |
| `note` | `TEXT` | no | Free text |
| `isSample` | `INTEGER` | no | Boolean 0/1 |

Index: `index_calibration_points_vehicleId`.

The engine sorts points by voltage and interpolates linearly. Duplicate voltages and flat/falling curves are flagged in the evaluation result. Calibration is only used for voltage-derived EV energy.

### `app_settings`

Single-row table with `id = 1`.

| Column | SQLite type | Null | Kotlin default | Notes |
|---|---|---:|---|---|
| `id` | `INTEGER` | no | `1` | Primary key; the app writes `id = 1` |
| `chargerEfficiency` | `REAL` | no | `89.0` | Percent; converted to a fraction in the engine |
| `maxPlausibleDistance` | `REAL` | no | `250.0` | km gap warning/baseline threshold |
| `excludeImplausible` | `INTEGER` | no | `true` | Boolean 0/1 |
| `warnDays` | `INTEGER` | no | `14` | Reminder due-soon days |
| `warnKm` | `INTEGER` | no | `200` | Reminder due-soon km |
| `chartStartMonth` | `TEXT` | no | `2026-10` | `yyyy-MM` chart window start |
| `evPlausibleMin` | `REAL` | no | `10.0` | Lower EV efficiency bound, km/kWh |
| `evPlausibleMax` | `REAL` | no | `80.0` | Upper EV efficiency bound, km/kWh |
| `petrolPlausibleMin` | `REAL` | no | `20.0` | Lower petrol efficiency bound, km/L |
| `petrolPlausibleMax` | `REAL` | no | `90.0` | Upper petrol efficiency bound, km/L |
| `maintenanceCategories` | `TEXT` | no | newline list | Custom + default categories |

The plausibility bounds compare the engine's full-to-full efficiency value: km/kWh for EV, km/L for petrol. The labels in the settings UI describe them as efficiency bands. They are not consumption-per-100-km values.

Default maintenance categories:

```text
Routine service
Repair
Tyres
Brakes
Battery / Charger
Chain / Drive
Electricals
Accessories
Other
```

`SettingsEntity.categoryList()` trims and drops blank lines. The stored text remains newline-separated.

### `app_meta`

| Column | SQLite type | Null | Notes |
|---|---|---:|---|
| `key` | `TEXT` | no | Primary key |
| `value` | `TEXT` | no | Metadata value |

Currently used for:

- `restoredAt` — epoch millis set after a successful full restore.

The table is not a general user-facing setting store. It is intentionally separate from `app_settings`.

## Migration history

### Version 1 → 2

Adds per-entry mileage eligibility:

```sql
ALTER TABLE usage_entries
ADD COLUMN includeInMileage INTEGER NOT NULL DEFAULT 1;
```

Existing rows default to included, preserving the pre-upgrade calculation behaviour. The migration is additive; no user data is dropped.

`FleetDatabase` registers `MIGRATION_1_2` in `FleetPulseApp`:

```kotlin
Room.databaseBuilder(this, FleetDatabase::class.java, FleetDatabase.NAME)
    .addMigrations(FleetDatabase.MIGRATION_1_2)
    .fallbackToDestructiveMigrationOnDowngrade()
    .build()
```

There is currently no automated Room migration test. Adding one is on the roadmap. A future schema change must:

1. Increment the Room version.
2. Provide a non-destructive `Migration`.
3. Export the new schema JSON.
4. Add a migration test.
5. Update the backup compatibility notes if fields change.

## DataStore preferences

File: `fleetpulse_prefs` (Preferences DataStore, stored under the app's private `datastore` directory).

| Key | Type | Default | Meaning |
|---|---|---|---|
| `theme_mode` | String | `system` | `system`, `light`, or `dark` |
| `onboarding_complete` | Boolean | `false` | Whether the first-run choice has been made |
| `notifications_enabled` | Boolean | `true` | User preference for local service reminders |

`AppPreferencesState.loaded` is not persisted. It is false until DataStore emits its first value and is used to avoid flashing the onboarding screen during startup.

## Backup mapping

JSON v2 (`format: "fleetpulse-backup"`, `version: 2`) maps to the tables as follows:

| JSON section | Table |
|---|---|
| `data.vehicles` | `vehicles` |
| `data.usageEntries` | `usage_entries` |
| `data.maintenanceEntries` | `maintenance_entries` |
| `data.priceHistory` | `price_history` |
| `data.calibration` | `calibration_points` |
| `data.settings` | `app_settings` |

`app_meta` is not exported. After a successful restore, `restoredAt` is written. `includeInMileage` is exported when present and defaults to `true` on import when missing, including legacy v4 files.

The backup writer identifies itself with:

```text
"app": "FleetPulse Android 4.2.0"
```

That label is a format-level compatibility string. If the app version changes, update the string and its tests together so backups remain traceable.

CSV exports are evaluated views, not backups. They contain computed fields and should not be imported as a restore source.

## Storage and security notes

- The database is stored in the app's private internal storage and is protected by the Android app sandbox and device storage encryption.
- FleetPulse does **not** use SQLCipher or application-level database encryption. A rooted device or a backup of the app's private data can expose the database, as with most local Android databases.
- Android auto-backup is disabled (`android:allowBackup="false"`), so the database is not copied to Google's auto-backup service.
- Exported JSON and CSV files are outside the app's sandbox once the user chooses a destination. Their protection is the responsibility of the destination and the user.
- The app declares no `INTERNET` permission.
- Never commit a real `fleetpulse.db`, exported backup, CSV, or screenshot containing personal data.
