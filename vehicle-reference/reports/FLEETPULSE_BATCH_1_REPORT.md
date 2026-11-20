# FleetPulse Universal Vehicle Reference Database — Phase 1 (Batch 1) Report

**Database Version:** `1.0.0-batch1` | **Schema Version:** `1.0.0` | **Verified Date:** `2026-11-20` | **Market Scope:** India (`IN`) with international-ready schema

## 1. Proposed Database Architecture & Schema Summary

The repository is normalized for low-bandwidth Android consumption over GitHub Raw / CDN so FleetPulse never downloads the full dataset to search:

- **`manifest.json`**: Top-level version, category counts, and SHA-256 checksums for conditional caching.
- **`schema/vehicle.schema.json`**: Strict JSON Schema (Draft 2020-12) enforcing deterministic IDs (`<manufacturer>_<model>_<variant>_<year>_<iso2>`), explicit `null` for unverified values, typed multi-metric efficiency arrays, hour-based tractor specs, LNT/DEF aftertreatment, and structured discrepancy records.
- **`taxonomy/`**: Normalized `categories.json` (8 categories, 56 subcategories), `propulsion-types.json` (7 types), and `fuel-types.json` (10 types).
- **`manufacturers/index.json`**: Normalized directory of all 23 manufacturer entities (18 distinct brand groups) with stable IDs and official websites.
- **`indexes/*.json`**: 8 lightweight per-category search indexes (`two-wheelers.json` through `other.json`) pre-populated with aliases and `search_tokens`.
- **`vehicles/<category>/<manufacturer>/<id>.json`**: 30 isolated, non-merged variant records.
- **`sources/source-index.json`**: Deduplicated master registry of all 97+ cited URLs with reliability tiers.
- **`scripts/validate_database.py`**: Zero-dependency/JSON-Schema CI validation script.

## 2. Batch 1 Coverage Report

| Category | Verified Records | Subcategories Covered | Fuel / Propulsion Mix |
| --- | ---: | --- | --- |
| Two-Wheelers (`two-wheelers`) | 8 | Petrol Scooter (3), Electric Scooter (2), Commuter Motorcycle (2), Touring/Cruiser (1) | Petrol ICE (5), Mild-Hybrid Petrol (1), BEV (2) |
| Three-Wheelers (`three-wheelers`) | 5 | Passenger Auto (1), Cargo Auto (1), Electric 3W (1), E-Cargo (1), E-Rickshaw (1) | CNG Bi-Fuel (1), Diesel (1), BEV (3) |
| Passenger Vehicles (`passenger-vehicles`) | 7 | Hatchback (2), Compact SUV (2), SUV (1), MPV (2) | Petrol (2), CNG Bi-Fuel (1), Mild Hybrid (1), Strong Hybrid (1), Diesel (1), BEV (1) |
| Commercial Vehicles (`commercial-vehicles`) | 5 | Mini Truck (2), Pickup Truck (3) | Diesel LNT/BS6 (4), CNG Bi-Fuel (1) |
| Agricultural Tractors (`agricultural`) | 5 | Agricultural Tractor (4), Utility Tractor (1) | Diesel (5) — all with CFMTTI Budni `g/kWh` SFC & PTO HP |
| Construction, Public Transport, Other | 0 | Empty valid index scaffolds ready for Phase 2 & Phase 3 | — |
| **Total** | **30** | **15 subcategories active (56 defined in taxonomy)** | **23 manufacturer records, 133 unique sources** |

### Complete Batch 1 Vehicle Roster

| ID | Brand & Model | Locked Variant | Fuel | Primary Metric | Verification |
| --- | --- | --- | --- | --- | --- |
| `honda_activa_110_dlx_obd2b_2025_in` | Honda Activa 110 | Activa 110 DLX (OBD2B) | `petrol` | 59.5 km_per_liter | `cross_verified` |
| `tvs_jupiter_110_disc_smartxonnect_obd2b_2025_in` | TVS Jupiter 110 | Jupiter 110 Disc SmartXonnect (OBD2B) | `petrol` | 53.84 km_per_liter | `cross_verified` |
| `suzuki_access_125_ride_connect_obd2b_2025_in` | Suzuki Access 125 | Access 125 Ride Connect Edition (OBD2B) | `petrol` | 45.0 km_per_liter | `cross_verified` |
| `hero_splendor_plus_i3s_drum_obd2b_2025_in` | Hero Splendor+ | Splendor+ i3S Drum Brake (OBD2B) | `petrol` | 70.0 km_per_liter | `cross_verified` |
| `bajaj_platina_110_drum_cbs_2026_in` | Bajaj Platina 110 | Platina 110 Drum (CBS) | `petrol` | 70.0 km_per_liter | `cross_verified` |
| `royal_enfield_classic_350_heritage_j_series_2025_in` | Royal Enfield Classic 350 | Classic 350 Heritage (Dual-Channel ABS) | `petrol` | 41.55 km_per_liter | `officially_verified` |
| `ather_450x_3_7_kwh_2025_in` | Ather 450X | Ather 450X 3.7 kWh (Pre-July 2026) | `electric` | 161.0 km | `cross_verified` |
| `tvs_iqube_3_5_kwh_2025_in` | TVS iQube | TVS iQube 3.5 kWh (Post-April 2025) | `electric` | 145.0 km | `officially_verified` |
| `bajaj_re_cng_236cc_2025_in` | Bajaj RE | RE CNG (3-Seater/CNG, 236.2 cc Bi-Fuel) | `cng_petrol_bi_fuel` | 27.0 km_per_kg | `cross_verified` |
| `piaggio_ape_xtra_ldx_2100_diesel_2025_in` | Piaggio Apé Xtra LDX | Apé Xtra LDX 2100/Diesel (599 cc BS6 Deck Body) | `diesel` | 22.0 km_per_liter | `secondary_source` |
| `mahindra_treo_sft_7_37_kwh_2025_in` | Mahindra Treo | Mahindra Treo - SFT (Soft Top, 7.37 kWh Li-ion) | `electric` | 139.0 km | `officially_verified` |
| `euler_hiload_ev_gen3_tr_dv120_2025_in` | Euler Motors HiLoad EV | HiLoad+ TR (DV 120 cu ft Delivery Van) | `electric` | 164.0 km | `officially_verified` |
| `yc_electric_yatri_super_4_seater_2025_in` | YC Electric Yatri Super | Yatri Super (4-Seater / Electric) | `electric` | 75.0-90.0 km | `cross_verified` |
| `maruti_suzuki_swift_zxi_1_2_petrol_5mt_2025_in` | Maruti Suzuki Swift | ZXi 1.2L Z12E Petrol 5MT | `petrol` | 24.8 km_per_liter | `officially_verified` |
| `maruti_suzuki_wagonr_vxi_scng_1_0_5mt_2025_in` | Maruti Suzuki WagonR | VXi S-CNG 5MT (1.0L K10C) | `cng_petrol_bi_fuel` | 33.47 km_per_kg | `officially_verified` |
| `maruti_suzuki_ertiga_zxi_1_5_hybrid_5mt_2025_in` | Maruti Suzuki Ertiga | ZXi 1.5L K15C Smart Hybrid Petrol 5MT | `petrol` | 20.51 km_per_liter | `cross_verified` |
| `tata_nexon_ev_empowered_45_2025_in` | TATA.ev Nexon.ev | Empowered 45 (45 kWh Battery Pack) | `electric` | 489.0 km | `officially_verified` |
| `hyundai_creta_sx_1_5_mpi_petrol_6mt_2025_in` | Hyundai Creta | SX 1.5L MPi Petrol 6MT | `petrol` | 17.4 km_per_liter | `officially_verified` |
| `mahindra_scorpio_n_z8_diesel_2wd_6mt_2025_in` | Mahindra Scorpio-N | Z8 2.2L mHawk Diesel 2WD 6MT (High-Power 128.6 kW Tune) | `diesel` | 15.42 km_per_liter | `officially_verified` |
| `toyota_innova_hycross_zx_2_0_hybrid_ecvt_2025_in` | Toyota Innova Hycross | ZX 2.0L Strong Hybrid e-CVT (7-Seater) | `petrol` | 22.16 km_per_liter | `officially_verified` |
| `tata_ace_gold_plus_2100_diesel_lnt_2025_in` | Tata Ace Gold | Ace Gold+ (2100/Diesel Deck, LNT) | `diesel` | 22.0 km_per_liter | `officially_verified` |
| `maruti_suzuki_super_carry_scng_std_esp_2025_in` | Maruti Suzuki Super Carry | CNG STD (2110/CNG Standard Deck, 2025 ESP) | `cng_petrol_bi_fuel` | 23.24 km_per_kg | `officially_verified` |
| `tata_intra_v30_gold_lx_2450_diesel_2025_in` | Tata Intra V30 Gold | Intra V30 Gold LX (2450 WB CLB Deck) | `diesel` | 17.0 km_per_liter | `officially_verified` |
| `mahindra_bolero_maxx_pikup_hd_1_7l_lx_2025_in` | Mahindra Bolero MaXX Pik-Up HD | HD 1.7L LX (10 ft Load Body, Power Steering) | `diesel` | 17.2 km_per_liter | `partially_verified` |
| `ashok_leyland_bada_dost_i4_lnt_fsd_ls_2025_in` | Ashok Leyland Bada Dost i4 with LNT | FSD LS (2590 WB Fixed Side Deck, LNT) | `diesel` | 13.0-15.0 km_per_liter | `officially_verified` |
| `mahindra_575_di_xp_plus_2wd_2025_in` | Mahindra Tractors 575 DI XP Plus | Mahindra 575 DI XP Plus (2WD) | `diesel` | 248.0 g_per_kwh | `cross_verified` |
| `swaraj_744_fe_2wd_2025_in` | Swaraj Tractors 744 FE | Swaraj 744 FE (2WD) | `diesel` | 257.0 g_per_kwh | `officially_verified` |
| `sonalika_di_745_iii_sikander_dlx_2wd_2025_in` | Sonalika Tractors DI 745 III | Sonalika DI 745 III Sikander DLX (2WD) | `diesel` | 250.0 g_per_kwh | `cross_verified` |
| `john_deere_5050d_2wd_8f4r_2025_in` | John Deere 5050D | John Deere 5050D (2WD, 8F+4R Collarshift) | `diesel` | 270.0 g_per_kwh | `officially_verified` |
| `massey_ferguson_241_di_dynatrack_2wd_2025_in` | Massey Ferguson 241 DI | MF 241 DI DynaTrack (2WD) | `diesel` | 265.0 g_per_kwh | `officially_verified` |

## 3. Data Quality & Verification Report

- **Schema Validation (`jsonschema (Draft 2020-12)`)**: **PASS** (0 errors across 30 records).
- **Verification Status Breakdown**: `officially_verified`: 17, `cross_verified`: 11, `partially_verified`: 1, `secondary_source`: 1.
- **Official Primary Source Anchoring**: 30 / 30 records carry at least one Tier-1 official manufacturer webpage, official PDF brochure, official owner's manual, or CFMTTI government test report.
- **Source Reliability Tiers**: Tier 1 Official (`62`), Tier 2 Reputable Auto/CV/Agri Portals (`58`), Tier 3 Service Guides (`13`).

## 4. Duplicate Detection Report

- **Status**: **PASS**
- **Duplicate Vehicle IDs**: `0`
- **Duplicate Variant Fingerprints** (`manufacturer + model + variant + year + market`): `0`
- **Overlapping Aliases Across Records**: `0`

## 5. Missing-Data Report (Zero-Fabrication Audit)

Unpublished or unverified manufacturer fields are stored explicitly as `null` rather than inferred from sibling variants:

| Field | Populated | Missing (`null`) | Completeness | Key Structural Reason |
| --- | ---: | ---: | ---: | --- |
| `model.manufacturer_model_code` | 11/30 | 19 | 36.7% | Most Indian OEM brochures omit internal chassis/model codes. |
| `powertrain.power` | 30/30 | 0 | 100.0% | Verified on 100% of Batch 1 records. |
| `powertrain.torque` | 26/30 | 4 | 86.7% | Omitted by YC Electric (e-rickshaw) and 3 tractor OEMs (Swaraj, John Deere, TAFE). |
| `powertrain.drive_type` | 25/30 | 5 | 83.3% | Omitted explicitly on 3-wheeler OEM tables. |
| `energy_storage.primary_energy` | 28/30 | 2 | 93.3% | Omitted/conflicting on Mahindra 575 DI XP Plus (fuel tank) and YC Yatri Super (kWh). |
| `dimensions.kerb_weight_kg` | 24/30 | 6 | 80.0% | Indian car OEMs (Tata EV, Hyundai, Mahindra, Toyota) and Sonalika omit kerb weight in brochures. |
| `dimensions.gross_weight_kg` | 12/30 | 18 | 40.0% | Omitted across 2-wheelers, tractors, and 4 passenger car OEM brochures. |
| `dimensions.ground_clearance_mm` | 27/30 | 3 | 90.0% | Omitted on WagonR CNG brochure, Mahindra 575, and Sonalika 745. |
| `maintenance.engine_oil_grade` | 18/30 | 12 | 60.0% | Indian commercial truck and tractor brochures omit oil viscosity online (BEVs marked N/A). |
| `maintenance.engine_oil_capacity_liters` | 17/30 | 13 | 56.7% | Requires physical owner's manual PDFs for SCVs and tractors (BEVs marked N/A). |
| `maintenance.tire_size_front` | 29/30 | 1 | 96.7% | Exact section size omitted on retrieved Suzuki Access 125 official snippet. |
| `maintenance.tire_pressure_front_psi` | 6/30 | 24 | 20.0% | Rarely published on web brochures; verified primarily from 2W manuals. |
| `maintenance.service_interval_value` | 18/30 | 12 | 60.0% | Omitted on online brochures for Ather 450X, 3W EVs, most SCVs, and Sonalika/MF tractors. |

## 6. Cross-Source Discrepancies Log (41 Total)

| Vehicle | Field | Adopted Value | Conflicting Value | Status | Sources |
| --- | --- | --- | --- | --- | --- |
| Honda Activa 110 (Activa 110 DLX (OBD2B)) | `efficiency.fuel_economy` | 59.5 km/L (claimed, BikeDekho) & 47.0 km/L (owner avg, BikeWale) | Omitted on HMSI official site | `documented_unverified_oem` | [Primary](https://www.bikedekho.com/honda/activa-110-dlx.html) / [Alt](https://www.honda2wheelersindia.com/scooter/activa110) |
| TVS Jupiter 110 (Jupiter 110 Disc SmartXonnect (OBD2B)) | `efficiency.fuel_economy` | 53.84-61.2 km/L (flagged range) | 53.84 km/L ARAI (BikeWale) vs 61.2 km/L (BikeDekho) | `unresolved` | [Primary](https://www.bikewale.com/tvs-bikes/jupiter/) / [Alt](https://www.bikedekho.com/tvs/jupiter/smartxonnect-disc) |
| TVS Jupiter 110 (Jupiter 110 Disc SmartXonnect (OBD2B)) | `dimensions.height_mm` | 1158 mm (BikeDekho India) | 1258 mm (TVS Sri Lanka export table) | `unresolved` | [Primary](https://www.bikedekho.com/tvs/jupiter/smartxonnect-disc) / [Alt](https://www.tvsmotor.com/en/lk/our-products/tvs-jupiter-110) |
| Suzuki Access 125 (Access 125 Ride Connect Edition (OBD2B)) | `dimensions.seat_height_mm` | 773 mm (BikeWale) | 856 mm (BikeDekho — actually seat length) | `resolved_primary` | [Primary](https://www.bikewale.com/suzuki-bikes/access-125/) / [Alt](https://www.bikedekho.com/suzuki/access-125) |
| Suzuki Access 125 (Access 125 Ride Connect Edition (OBD2B)) | `dimensions.kerb_weight_kg` | 106 kg (Suzuki India official RC/SE) | 105 kg (BikeWale comparison) | `resolved_primary` | [Primary](https://www.suzukimotorcycle.co.in/product-details/all-new-access-125-bluetooth-enabled) / [Alt](https://www.bikewale.com/suzuki-bikes/access-125/) |
| Suzuki Access 125 (Access 125 Ride Connect Edition (OBD2B)) | `maintenance.engine_oil_capacity_liters` | 0.65 L drain (BikeFix Pune) | 1.0 L (RideNRepair) | `unresolved` | [Primary](https://www.bikefixpune.in/blog/suzuki-access-125-service-cost-pune) / [Alt](https://www.ridenrepair.com/blog-details/suzuki-access-125-service-cost-2026-scooter-guide) |
| Hero Splendor+ (Splendor+ i3S Drum Brake (OBD2B)) | `efficiency.fuel_economy` | 62-70 km/L (secondary range) | 70 km/L (BikeDekho) vs 63 km/L (BikeWale) vs 62 km/L (ACKO Drive) | `unresolved` | [Primary](https://www.bikedekho.com/hero/splendor-plus-i3s-obd-2b.html) / [Alt](https://www.bikewale.com/hero-bikes/splendor-plus/) |
| Bajaj Platina 110 (Platina 110 Drum (CBS)) | `powertrain.engine_and_gearbox` | 115.06 cc, 6.99 kW (9.5 PS) @ 7,500 rpm, 9.9 Nm @ 5,500 rpm, 4-speed all-down-shift (2026 Bajaj India official) | 115.45 cc, 8.6 PS @ 7,000 rpm, 9.81 Nm @ 5,000 rpm, 5-speed, 119 kg (BikeWale & BikeDekho) | `resolved_primary` | [Primary](https://www.bajajauto.com/bikes/platina/platina-110) / [Alt](https://www.bikewale.com/bajaj-bikes/platina-110/) |
| Bajaj Platina 110 (Platina 110 Drum (CBS)) | `dimensions.kerb_weight_kg` | 116 kg (2026 Lowzo) | 119 kg (BikeWale / BikeDekho legacy 5-speed) | `unresolved` | [Primary](https://lowzo.in/specs/bajaj-platina-110/) / [Alt](https://www.bikewale.com/bajaj-bikes/platina-110/) |
| Ather 450X (Ather 450X 3.7 kWh (Pre-July 2026)) | `energy_storage.battery_capacity_kwh` | 3.7 kWh NMC (2025 / Pre-July 2026 SKU) | 3.5 kWh NMC (Post-July 17, 2026 production revision retaining 161 km IDC) | `split_sku_recommended` | [Primary](https://www.atherenergy.com/450) / [Alt](https://www.autocarindia.com/bikes/ather/450/x-35-kwh) |
| TVS iQube (TVS iQube 3.5 kWh (Post-April 2025)) | `powertrain_and_dimensions` | 4.4 kW / 3.0 kW, 1805x645x1140 mm, 118.6 kg (India domestic spec) | 4.6 kW / 3.34 kW, 1835x690x1195 mm, 119 kg (TVS Kenya export 3.5 kWh page) | `resolved_primary` | [Primary](https://www.tvsmotor.com/electric-scooters/tvs-iqube/tvs-iqube-3-5-kwh-variant-details) / [Alt](https://www.tvsmotor.com/electric-scooters/tvs-iqube/tvs-iqube-3-5-kwh-variant-details) |
| Bajaj RE (RE CNG (3-Seater/CNG, 236.2 cc Bi-Fuel)) | `powertrain.power_and_torque` | 7.35 kW @ 5000 rpm / 17.66 Nm @ 3500 rpm (current Bajaj Auto spec table) | 6.90 kW @ 4750 rpm / 16.18 Nm @ 3000 rpm (Bajaj Auto FAQ & Bajaj Auto Credit) | `resolved_primary` | [Primary](https://www.bajajauto.com/three-wheelers/re) / [Alt](https://www.bajajautocredit.com/three-wheeler-loan/bajaj-re-cng) |
| Bajaj RE (RE CNG (3-Seater/CNG, 236.2 cc Bi-Fuel)) | `dimensions.gross_weight_kg` | 713 kg (structured variant tables on TrucksDekho & Truck Junction) | 708 kg (editorial summaries) | `resolved_primary` | [Primary](https://trucks.cardekho.com/en/trucks/bajaj/compact-4s) / [Alt](https://trucks.tractorjunction.com/en/bajaj-truck/compact-re) |
| Piaggio Apé Xtra LDX (Apé Xtra LDX 2100/Diesel (599 cc BS6 Deck Body)) | `classification.vehicle_class` | L5N (CMVR 3-wheel goods carrier >25 km/h) | L3N (TrucksDekho summary card tag) | `resolved_regulatory` | [Primary](https://piaggio-cv.co.in/cargo/ape-xtra-600/) / [Alt](https://trucks.cardekho.com/en/trucks/piaggio/ape-xtra-ldx-bs6) |
| Piaggio Apé Xtra LDX (Apé Xtra LDX 2100/Diesel (599 cc BS6 Deck Body)) | `efficiency_and_payload` | 22 km/L & 496 kg payload (TrucksDekho BS6) | Up to 32 km/L & 510-520 kg payload (older dealer listings) | `unresolved` | [Primary](https://trucks.cardekho.com/en/trucks/piaggio/ape-xtra-ldx-bs6) / [Alt](https://trucks.tractorjunction.com/en/piaggio-truck/ape-xtra-ldx) |
| Mahindra Treo (Mahindra Treo - SFT (Soft Top, 7.37 kWh Li-ion)) | `efficiency_and_warranty` | 139 km ARAI / 110 km real-world; 5 yr / 1,20,000 km warranty (2025/2026 MLMM website) | 141 km certified / 130 km typical; 3 yr / 80,000 km warranty (older downloadable brochure PDF) | `resolved_primary` | [Primary](https://mahindralastmilemobility.com/treo) / [Alt](https://mahindralastmilemobility.com/assets/pdf/treo.pdf) |
| Mahindra Treo (Mahindra Treo - SFT (Soft Top, 7.37 kWh Li-ion)) | `dimensions.gross_weight_kg` | null (omitted from official PDF) | 698 kg (Truck Junction) vs 720 kg (BusesDekho) | `unresolved` | [Primary](https://mahindralastmilemobility.com/assets/pdf/treo.pdf) / [Alt](https://buses.cardekho.com/buses/mahindra/treo/3-seater-sft) |
| Euler Motors HiLoad EV (HiLoad+ TR (DV 120 cu ft Delivery Van)) | `generation_specs` | Gen 3 HiLoad+ TR: 11.5 kWh, 11.7 kW PMSM, 1495 kg GVW, 768 kg payload, 164 km ARAI | Legacy Gen 2 listings on 91Trucks: 13 kWh, 10.96 kW induction, 1413 kg GVW, 688 kg payload | `resolved_primary` | [Primary](http://5.imimg.com/data5/SELLER/Doc/2025/12/570206670/KJ/MA/JB/237264453/euler-hiload-ev.pdf) / [Alt](https://www.91trucks.com/auto-rickshaws/euler-motors/hi-load/dv-120-cft) |
| YC Electric Yatri Super (Yatri Super (4-Seater / Electric)) | `energy_storage.battery_chemistry` | null in energy_storage (dealer-configurable) | 48 V / 41.7 Ah Li-ion (TrucksDekho) vs 4x12V Lead-Acid packs (regional IndiaMART dealers) | `unresolved` | [Primary](https://www.ycevehicles.com/super) / [Alt](https://trucks.cardekho.com/en/trucks/yc-electric/yatri-super/specifications) |
| Maruti Suzuki Swift (ZXi 1.2L Z12E Petrol 5MT) | `powertrain.torque` | 111.7 Nm @ 4300 rpm (Official Maruti Z12E brochure) | 112.4 Nm @ 4300 rpm (CarDekho & CarWale) | `resolved_primary` | [Primary](https://www.marutisuzuki.com/content/dam/msil/arena/in/en/assets/cars/swift/document/Swift_SCNG_VerticalBrochure.pdf) / [Alt](https://www.cardekho.com/overview/Maruti_Swift/Maruti_Swift_ZXi.htm) |
| Maruti Suzuki WagonR (VXi S-CNG 5MT (1.0L K10C)) | `efficiency.fuel_economy` | 33.47 km/kg (April 2025 6-airbag OEM brochure) | 34.05 km/kg (older 2-airbag BS6.1 CarDekho listings) | `resolved_primary` | [Primary](https://www.marutisuzuki.com/content/dam/msil/arena/in/en/assets/cars/wagonr/documents/Arena-Wagon-r-Brochure.pdf) / [Alt](https://www.cardekho.com/overview/Maruti_Wagon_R/Maruti_Wagon_R_VXI_CNG_BSVI.htm) |
| Maruti Suzuki Ertiga (ZXi 1.5L K15C Smart Hybrid Petrol 5MT) | `dimensions.length_mm_and_torque` | 4435 mm length & 139.0 Nm @ 4300 rpm (2025/2026 OEM Tour M & CarDekho 2026) | 4395 mm length & 136.8 Nm @ 4400 rpm (pre-2025 CarWale page) | `resolved_primary` | [Primary](https://www.marutisuzukicommercial.com/ertiga-tour-m) / [Alt](https://www.carwale.com/maruti-suzuki-cars/ertiga/zxi/) |
| TATA.ev Nexon.ev (Empowered 45 (45 kWh Battery Pack)) | `powertrain.power_and_battery` | 110 kW motor & 46.08 kWh gross (marketed as 45 kWh) per official brochure | 106 kW (142-143 bhp) & 45 kWh on CarDekho/CarWale (legacy 40.5 kWh LR carryover) | `resolved_primary` | [Primary](https://stc.aeplcdn.com/f/brochure/16/2503/a16fk5i.pdf) / [Alt](https://www.cardekho.com/tata/nexon-ev) |
| TATA.ev Nexon.ev (Empowered 45 (45 kWh Battery Pack)) | `dimensions_and_gc` | 3994x1811x1616 mm & 190 mm unladen GC (official brochure) | 3995x1802x1625 mm & 205 mm GC (CarWale — 30 kWh MR figure) | `resolved_primary` | [Primary](https://stc.aeplcdn.com/f/brochure/16/2503/a16fk5i.pdf) / [Alt](https://www.cardekho.com/tata/nexon-ev) |
| Hyundai Creta (SX 1.5L MPi Petrol 6MT) | `dimensions.kerb_weight_kg` | null (omitted by Hyundai India) | Third-party portals diverge from 1175 kg to 1490 kg (Autocar cites 1282 kg for CVT) | `unresolved` | [Primary](https://www.hyundai.com/content/dam/hyundai/in/en/data/brochure/creta.pdf) / [Alt](https://www.autocarindia.com/cars/hyundai/creta/specifications) |
| Mahindra Scorpio-N (Z8 2.2L mHawk Diesel 2WD 6MT (High-Power 128.6 kW Tune)) | `powertrain.engine_displacement_cc` | 2184 cc (CarDekho Z8 sheet; Mahindra brochure states '2.2 L') | 2198 cc (CarDekho Z4 / overview page) | `unresolved` | [Primary](https://www.cardekho.com/overview/Mahindra_Scorpio_N_2022-2026/Mahindra_Scorpio_N_2022-2026_Z8_Diesel.htm) / [Alt](https://www.cardekho.com/mahindra/scorpio-n) |
| Toyota Innova Hycross (ZX 2.0L Strong Hybrid e-CVT (7-Seater)) | `efficiency_and_ice_torque` | 22.16 km/L & 191 Nm ICE torque (2025 Official Toyota Brochure) | 23.24 km/L & 188 Nm ICE torque (2023 launch manual, Autocar India & CarDekho) | `resolved_primary` | [Primary](https://www.toyotabharat.com/documents/brochures/e-brochure-hycross.pdf) / [Alt](https://www.autocarindia.com/cars/toyota/innova-hycross/specifications) |
| Tata Ace Gold (Ace Gold+ (2100/Diesel Deck, LNT)) | `powertrain.power` | 16.2 kW (22 PS) Power Mode (Tata Motors official) | 20 HP (91Trucks) | `resolved_primary` | [Primary](https://smalltrucks.tatamotors.com/ace-gold-plus) / [Alt](https://www.91trucks.com/trucks/tata/ace-gold-diesel-plus) |
| Tata Ace Gold (Ace Gold+ (2100/Diesel Deck, LNT)) | `classification.vehicle_class` | N1 (CMVR 4-wheel goods carrier <= 3.5T) | L5N (TrucksDekho tag) | `resolved_regulatory` | [Primary](https://smalltrucks.tatamotors.com/ace-gold-plus) / [Alt](https://trucks.cardekho.com/en/trucks/tata/ace-gold-plus/specifications) |
| Maruti Suzuki Super Carry (CNG STD (2110/CNG Standard Deck, 2025 ESP)) | `efficiency.unit` | 23.24 km/kg (CNG mode per CMVR brochure) | 23.24 kmpl (TrucksDekho unit mislabeling) | `resolved_primary` | [Primary](https://www.scribd.com/document/650992523/CNG-Brochure) / [Alt](https://trucks.cardekho.com/en/trucks/maruti-suzuki/super-carry/cng-standard) |
| Tata Intra V30 Gold (Intra V30 Gold LX (2450 WB CLB Deck)) | `powertrain.displacement_and_torque_rpm` | 1,497 cc & 160 Nm @ 1,750-2,750 rpm (Jan 2025 Official Brochure PDF) | 1,496 cc & 1,800-3,000 rpm (Tata Motors webpage table) | `resolved_primary` | [Primary](https://smalltrucks.tatamotors.com/assets/smalltrucks/files/2025-01/Intra%20V30%20Gold%20-%20Brochure%20-2.pdf?VersionId=fBCb_25AGB8M9amZyw5olX.iDFaCWjqi) / [Alt](https://smalltrucks.tatamotors.com/tata-intra-v30-gold) |
| Mahindra Bolero MaXX Pik-Up HD (HD 1.7L LX (10 ft Load Body, Power Steering)) | `dimensions.kerb_weight_kg_and_gvw` | GVW 3,480 kg (10 ft HD 1.7L); Kerb weight = null | 3,460 kg GVW / 1,760 kg kerb (TrucksFloor, 9 ft HD 1.7 conflation) vs 1,895 kg kerb (HT Auto) | `unresolved` | [Primary](https://trucks.tractorjunction.com/en/mahindra-truck/bolero-maxx-pik-up-hd-17l-lx/price/nadia) / [Alt](https://trucksfloor.com/en/mahindra-truck/bolero-maxx-pik-up-hd/17l-lx) |
| Ashok Leyland Bada Dost i4 with LNT (FSD LS (2590 WB Fixed Side Deck, LNT)) | `dimensions.payload_kg_and_seating` | 1,850 kg payload & D+2 (3 seats) per Ashok Leyland official spec table | 1,400 kg in OEM meta description; '1 seat' in TrucksDekho header | `resolved_primary` | [Primary](https://www.ashokleyland.com/in/lightvehicles/smallcommercialvechicles/bada-dost-i4-with-lnt/specification) / [Alt](https://trucks.cardekho.com/en/trucks/ashok-leyland/bada-dost-i4-with-lnt) |
| Mahindra Tractors 575 DI XP Plus (Mahindra 575 DI XP Plus (2WD)) | `energy_storage.fuel_tank_liters` | null (omitted by Mahindra official page) | 65 L (KhetiGaadi) vs 48-50 L (other portals) | `unresolved` | [Primary](https://www.mahindratractor.com/tractors/mahindra-575-di-xp-plus) / [Alt](https://www.khetigaadi.com/new-tractor-model/mahindra-575-di-xp-plus/en) |
| Mahindra Tractors 575 DI XP Plus (Mahindra 575 DI XP Plus (2WD)) | `powertrain.torque` | 207 Nm (2026 Mahindra official spec table) | 178.6-179 Nm (TractorKarvan) / 192 Nm (TractorJunction prose) | `resolved_primary` | [Primary](https://www.mahindratractor.com/tractors/mahindra-575-di-xp-plus) / [Alt](https://tractorkarvan.com/tractor/mahindra-575-di-xp-plus) |
| Swaraj Tractors 744 FE (Swaraj 744 FE (2WD)) | `energy_storage.fuel_tank_liters` | 62 L (2026 Swaraj official page) | 56 L (TractorJunction, TractorKarvan, KhetiGaadi legacy figure) | `resolved_primary` | [Primary](https://www.swarajtractors.com/swaraj-744-fe-tractor) / [Alt](https://www.tractorjunction.com/swaraj-tractor/744-fe/) |
| Swaraj Tractors 744 FE (Swaraj 744 FE (2WD)) | `efficiency.specific_fuel_consumption` | 257 g/kWh (2024 CFMTTI Budni test report) | 238 g/kWh (TractorGyan) / 251 g/kWh (2012 Xtra) | `resolved_primary` | [Primary](https://www.scribd.com/document/823835671/3-SWARAJ-744-FE-4x4-N1-1) / [Alt](https://tractorkarvan.com/tractor/swaraj-744-fe) |
| Sonalika Tractors DI 745 III (Sonalika DI 745 III Sikander DLX (2WD)) | `powertrain.torque_and_lift` | 210 Nm torque & 2,000 kg lift (2026 Sonalika DLX official page) | 205 Nm (HDM brochure PDF); 1,800 kg lift (TractorJunction/TractorGuru) | `resolved_primary` | [Primary](https://www.sonalika.com/tractor/di-745-iii.html) / [Alt](https://www.sonalika.com/media/product-brochure/di-745-iii-1710852063.pdf) |
| Sonalika Tractors DI 745 III (Sonalika DI 745 III Sikander DLX (2WD)) | `powertrain.pto.power_hp` | 40.8 HP (range 40.0-43.0 HP across portals; omitted on OEM page) | 40.0 HP (TractorKarvan) vs 40.8 HP (TractorGuru) vs 43.0 HP (TractorJunction) | `unresolved` | [Primary](https://tractorguru.in/tractor/sonalika-di-745-iii-sikander) / [Alt](https://www.tractorjunction.com/sonalika-tractor/745-di-iii-sikander/) |
| John Deere 5050D (John Deere 5050D (2WD, 8F+4R Collarshift)) | `dimensions.hydraulic_lift_and_weight` | 1,800 kg lift & 1,880 kg weight (2026 John Deere India page) | 1,600 kgf lift & 1,870 kg weight (older 2018 PDF leaflet); 1,975 kg (4WD weight on KhetiGaadi) | `resolved_primary` | [Primary](https://www.deere.co.in/en-in/products-and-solutions/tractors/mid-size-tractors/5050d-tractor-nta1meq) / [Alt](https://www.deere.co.in/assets/pdfs/region-1/products/tractors/d-series/New-5050D-50HP-Single-Page-Leaflet_English-V.pdf) |
| Massey Ferguson 241 DI (MF 241 DI DynaTrack (2WD)) | `maintenance.tire_size_front_and_battery` | 6.00x16 front tyres & 12V 80Ah battery (Official TAFE/MF page) | 6.50x16 front tyres & 12V 75Ah battery (TractorJunction) | `resolved_primary` | [Primary](https://masseyfergusonindia.com/massey-ferguson/MF-241-DYNATRACK) / [Alt](https://www.tractorjunction.com/massey-ferguson-tractor/241-di-dynatrack/) |

## 7. Recommendations for Future Batches & Android Maintenance

1. **Split Mid-Cycle 2026 SKUs in Batch 2**: Add dedicated records for the **Ather 450X 3.5 kWh (Post-July 2026)** and **Bajaj Platina 110 5-Speed (Pre-2026)** so FleetPulse users on either hardware revision get exact battery/gearbox specs.
2. **Phase 1 Batch 2 Priorities**: Expand Indian two-wheelers (Ola S1 Pro Gen 2/3, Bajaj Chetak 3501, Hero Vida V2, Yamaha FZ-S, Ampere Magnus/Reo), passenger cars (Tata Punch, Maruti Dzire/Baleno, Kia Sonet, Mahindra Bolero Neo, MG Windsor EV), and 3-wheelers (TVS King EV Max, Atul Gem, Kinetic Green Safar).
3. **Phase 2 Expansion**: Populate `commercial-vehicles` MCV/HCV (Tata Prima/Signa, Ashok Leyland AVTR, Eicher Pro, BharatBenz), `public-transport` (Tata Starbus EV, Olectra, Volvo 9600), `construction` (JCB 3DX Backhoe Loader, Tata Hitachi EX200, Komatsu PC210), and additional agricultural machinery (Kubota MU4501, Escorts Farmtrac 45, VST Shakti Power Tiller, Kartar Combine Harvester).
4. **Owner's Manual Ingestion Pipeline**: For missing oil capacities, cold tyre pressures, and service intervals on SCVs and tractors, extract tables directly from official OEM owner's manual PDFs rather than relying on secondary service blogs.
5. **GitHub Actions CI**: Run `python3 scripts/validate_database.py` on every Pull Request to block duplicate IDs, broken taxonomy references, or schema regressions before updating `manifest.json`.
