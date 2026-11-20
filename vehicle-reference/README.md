# FleetPulse Universal Vehicle Reference Database (`vehicle-reference/`)

Source-verified, normalized vehicle reference database designed for public GitHub hosting and low-bandwidth consumption by the **FleetPulse** Android app.

## Architecture & Android Sync Flow

1. **Check Version (`manifest.json`)**: Fetch `manifest.json` (~3 KB) and compare `database_version` and per-index SHA-256 hashes against local Room/SharedPreferences cache.
2. **Fetch Category Search Index (`indexes/<category>.json`)**: When the user selects a vehicle category during onboarding, download only that lightweight category index (~1–5 KB) containing IDs, manufacturer/brand, model, variant, year, fuel type, aliases, and pre-tokenized `search_tokens`.
3. **Fetch Exact Vehicle Record (`vehicles/<category>/<manufacturer>/<id>.json`)**: After selection, fetch the single ~4–6 KB canonical JSON record to pre-fill tank/battery capacity, efficiency baselines, tyre sizes, oil grade, and service intervals (in `km` or operating `hours`).

## Current Release: `1.0.0-batch1` (Phase 1 — Batch 1)

- **Schema Version**: `1.0.0` (JSON Schema Draft 2020-12)
- **Verified Records**: `30` exact 2025/2026 India-market variants
- **Manufacturers**: `23`
- **Unique Cited Sources**: `133`
- **Schema Validation**: `0` errors (`jsonschema (Draft 2020-12)`)

## Validation

Run the repository CI check locally:

```bash
python3 scripts/validate_database.py
```
