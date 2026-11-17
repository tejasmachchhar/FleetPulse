# Calculation Logic

This document describes the behaviour implemented by `domain/CalcEngine.kt`, `domain/PetrolPricing.kt`, `domain/NumberText.kt`, and `domain/VerificationSuite.kt`. It is intentionally more precise than the summary in the README.

The engine is pure Kotlin. It has no Android dependencies and is exercised by JVM tests and by the in-app Calculation Verification screen.

## Inputs and ordering

The engine receives an `EngineState` containing vehicles, usage entries, maintenance entries, price history, calibration points, and settings. It also receives `EvalOptions` with a reference date and a period filter.

Usage entries are grouped by vehicle and sorted by:

1. `date` ascending (ISO `yyyy-MM-dd`)
2. original insertion order for equal dates

Maintenance entries are evaluated in their supplied order, but the latest record per vehicle/category is selected by date and then insertion order. The latest record wins.

Dates are local calendar dates stored as ISO strings. They are never converted through UTC, so a late-evening entry cannot move to the previous or next day.

## Odometer and distance

For each vehicle, the previous odometer starts at `vehicle.initialOdometer`.

- **First entry** (`i == 0`):
  - If `odometer < initialOdometer`, the entry is ERROR.
  - If `odometer - initialOdometer > maxPlausibleDistance`, the entry is CHECK, its distance is forced to `0`, and it is treated as the starting reading. The message asks the user to set the initial odometer correctly.
  - Otherwise, distance is `odometer - initialOdometer`.
- **Later entries**:
  - If `odometer < previousValidOdometer`, the entry is ERROR (rollback).
  - Otherwise, distance is `odometer - previousValidOdometer`.
  - A zero difference adds a CHECK note ("Duplicate odometer reading").
  - A gap larger than `maxPlausibleDistance` adds a CHECK note.

`previousValidOdometer` and the vehicle's current odometer are updated only for entries with no ERROR. A bad rollback therefore does not corrupt the next comparison. The current odometer is the maximum valid reading seen for that vehicle.

The first reading being far above the initial odometer is deliberately treated as a zero-distance baseline rather than a huge fake trip. This is common when a user starts tracking long after buying the vehicle.

## Energy or fuel per entry

### Electric vehicle, ride only

`fillType == "Ride only"` stores distance and cost basis but no energy. Energy is `0`, and the basis is `null`.

### Electric vehicle, metered

If `energyAdded > 0`:

```text
energyUsed = energyAdded
basis      = Measured
```

If the metered value exceeds the approximate wall capacity (`batteryCapacity / chargerEfficiency * 1.15`), the entry gets a CHECK note. The factor 1.15 allows for charging losses and rounding; it is not a hard validation.

### Electric vehicle, voltage-derived

If `energyAdded` is absent and `voltsBefore` is present:

1. Interpolate `voltsBefore` to a battery percentage.
2. If the curve is missing, the entry is ERROR.
3. For a full charge, `percentAfter = 100`.
4. For a partial charge, `voltsAfter` is required; otherwise ERROR.
5. If `percentAfter < percentBefore`, the entry is ERROR.
6. Otherwise:

```text
wall energy = (percentAfter - percentBefore) / 100
              * batteryCapacity / (chargerEfficiency / 100)
basis       = Estimated
```

The charger efficiency is clamped to the fallback `89%` only when the configured value is not greater than zero.

Out-of-range voltages are clamped by the interpolation step and add a CHECK note.

### Electric vehicle, missing input

If there is no metered energy and no `voltsBefore`, the entry is ERROR: enter either metered kWh or Volts Before.

### Petrol vehicle

- Ride only: energy `0`, basis `null`.
- `energyAdded > 0`: energy is litres, basis `Measured`. If quantity exceeds `tankCapacity * 1.05`, add a CHECK note.
- Otherwise ERROR: enter petrol quantity in litres.

### Petrol quantity/price/amount helper

`PetrolPricing` implements:

```text
amount     = quantity * unitPrice
unitPrice  = amount / quantity
quantity   = amount / unitPrice
```

All helpers reject null, non-finite, zero, or negative inputs. The saved usage entry stores quantity and the effective unit price; the amount is always their consistent product.

## Price and entry cost

For each entry, the applicable price is selected as follows:

1. If the entry has a positive `unitPrice`, use it.
2. Otherwise, use the latest tariff for the vehicle's energy source whose `effectiveFrom <= entry.date`.
3. If no price exists, `applicablePrice` is `null`, a CHECK note is added, and cost is zero.

```text
raw cost   = energyUsed * applicablePrice
entry cost = floor(raw cost * 100 + 0.5) / 100
```

The rounding matches JavaScript `Math.round(x * 100) / 100` for non-negative costs. Costs are rounded per entry before period sums, so the displayed totals match the stored cost of each record.

A tariff is historical: editing or adding a future tariff does not rewrite an older entry's cost. An entry with an explicit unit price remains pinned to that price.

## Full-to-full interval state machine

A verified stretch requires:

- an eligible full charge/full tank to open the interval;
- a later eligible full charge/full tank to close it;
- no ERROR in the interval;
- positive accumulated distance;
- positive accumulated energy/fuel.

For each vehicle, the engine maintains:

```text
hasPrevFull          // an eligible full fill opened the interval
accDistSinceFull     // distance accumulated while the interval is open
accEnergySinceFull   // eligible energy/fuel accumulated
accCostSinceFull     // entry costs accumulated
stretchHasEstimated  // any eligible energy was estimated
stretchValidPossible // no ERROR occurred in the interval
```

### Opening

If no interval is open, only a full charge/full tank with `includeInMileage = true` opens one. The opening entry is a baseline: its own distance is not a completed stretch.

### Accumulating

For each later entry while an interval is open:

```text
accDistSinceFull += distance
if (includeInMileage) {
    accEnergySinceFull += energyUsed
    if (basis == Estimated) stretchHasEstimated = true
}
accCostSinceFull += entryCost
```

The odometer distance is real travel regardless of the eligibility checkbox. The checkbox excludes the entry's **energy/fuel** from the mileage denominator and removes its ability to be a boundary. An excluded entry still keeps its distance, cost, and history.

### Closing

If the entry is a full fill with `includeInMileage = true` and the interval is valid:

```text
stretch distance = accDistSinceFull
stretch energy   = accEnergySinceFull
stretch cost     = accCostSinceFull
stretch basis    = Estimated if any eligible energy was estimated, else Measured
stretch efficiency = stretch distance / stretch energy
```

For an EV:

```text
km per charge = stretch distance
                * (batteryCapacity / chargerEfficiencyFraction)
                / stretch energy
```

For petrol, `km per charge` is the same as `stretch efficiency` in km/L.

After closing, all accumulators reset. A closing full fill with `includeInMileage = false` does not close the interval; it is treated like any other entry.

### ERROR handling

If any entry in the interval is ERROR, `stretchValidPossible` becomes false. A later full fill resets the accumulators but does not produce a verified stretch. The next eligible full fill can open a fresh baseline. This prevents a rollback or missing calibration from contaminating a later result.

### Plausibility

If the configured plausibility band exists for the energy source, a stretch outside the band is marked implausible and gets a CHECK note. If `excludeImplausible` is on, implausible stretches are excluded from averages; otherwise they are included and flagged.

## Verified, provisional, and unavailable

For a period:

- **Verified** uses only completed, valid, eligible full-to-full stretches whose **closing entry date** falls inside the period.
- A verified stretch may have started before the period; its full stretch distance/energy/cost are included when it closes inside the period.
- **Provisional** uses all non-error distance in the period divided by all eligible energy in the period.
- If no verified denominator exists, the provisional value is used and labelled `Provisional`.
- If neither exists, the metric is `null` and shown as unavailable, never as zero.

This means a period can show a verified stretch that began before the selected date range. That is intentional: the closing full fill is the observation that makes the stretch measurable.

## All-time statistics

For each vehicle:

```text
valid stretches       = verified stretches that pass plausibility policy
latest valid          = last valid stretch by date
last 5 valid          = last five valid stretches
avg last 5 km/charge  = average of their kmPerCharge values
all-time avg km/charge = average of all valid stretches' kmPerCharge values
```

The all-time average is an average of stretch values, not a distance-weighted total. For an EV, `kmPerCharge` is the per-stretch km/charge. For petrol, it is km/L. Battery wear is only calculated for EVs.

## Battery wear

Battery wear is an allowance per kilometre, not a cash transaction. For an EV with all required fields:

```text
battery price       = batteryCapacity * batteryRatePerKWh
cycles capacity     = chargeCycles * kmPerChargeUsed
age capacity        = batteryLifeYears * 365 * dailyKmPlan
distance considered = min(cycles capacity, age capacity)
wear cost per km    = battery price / distance considered
```

`kmPerChargeUsed` is:

1. the all-time average of valid EV stretches when one exists and is positive;
2. otherwise the vehicle's rated km/charge.

The source is recorded as `Actual (N valid charges)` or `Rated (no valid charge yet)` so the UI can explain the figure.

If battery capacity, replacement rate, charge cycles, expected life, or daily km plan is missing or non-positive, battery price and wear are zero and the source is `N/A`. If the rated km/charge is zero and no verified average exists, the cycle capacity, distance considered, and wear are zero, while the source remains `Rated (no valid charge yet)`. The engine does not invent a battery price from a partial configuration.

`maxRunningCapacityKm` is the cycles capacity. `distanceOverBatteryLifeKm` is the age capacity. `distanceConsideredKm` is their minimum. The UI can therefore explain which limit drives the wear rate.

## Period metrics

The period bounds are inclusive on both ends. Supported keys:

| Key | Bounds |
|---|---|
| `all` | `1900-01-01` to `9999-12-31` |
| `current_month` | First to last day of the reference month |
| `last_30` | `referenceDate - 30 days` to `referenceDate` |
| `last_90` | `referenceDate - 90 days` to `referenceDate` |
| `current_fy` | 1 April to 31 March containing the reference date |
| `prev_fy` | Previous 1 April to 31 March |
| `custom` | Supplied dates; blank values fall back to `2026-01-01` / `2027-12-31` |

The `last_30` label is the UI label; the implementation includes both the start date (`referenceDate - 30`) and the end date, which is 31 calendar dates. The same inclusive behaviour applies to `last_90`. This matches the reference implementation and its tests.

For each vehicle, with `pLogs` = non-error entries whose date is in the period:

```text
distance        = sum of entry distances
energyAdded     = sum of all entry energy/fuel
eligibleEnergy  = sum of energy/fuel where includeInMileage = true
measuredEnergy  = sum where basis = Measured
estimatedEnergy = sum where basis = Estimated
energySpend     = sum of rounded entry costs
```

Verified aggregates use only valid stretches whose closing entry is in `pLogs`:

```text
verifiedDistance   = sum of their stretchDistance
verifiedEnergy     = sum of their stretchEnergy
verifiedEnergyCost = sum of their stretchCost
verifiedEfficiency = verifiedDistance / verifiedEnergy, or null
```

Then:

```text
provisionalEfficiency = distance / eligibleEnergy, or null
efficiencyUsed        = verifiedEfficiency ?: provisionalEfficiency
efficiencyBasis       = Verified / Provisional / Not enough data

simpleEnergyCostPerKm   = energySpend / distance, or null
verifiedEnergyCostPerKm = verifiedEnergyCost / verifiedDistance, or null
energyCostPerKmUsed     = verifiedEnergyCostPerKm ?: simpleEnergyCostPerKm
energyCostBasis         = Verified / Provisional / Not enough data

batteryWearInPeriod = distance * batteryWearCostPerKm
runningCostPerKm    = energyCostPerKmUsed + batteryWearCostPerKm
totalRunningCost    = energySpend + batteryWearInPeriod
```

`totalRunningCost` and `runningCostPerKm` answer different questions. The per-km rate uses verified energy cost, while the total includes every saved entry's energy/fuel cost, including a baseline or partial fill whose cost is not part of a verified stretch. This is why the reference total is ₹477.75 while the rate is ₹1.79/km.

Maintenance:

```text
maintenanceSpend      = sum of maintenance amounts in the period
maintenanceCostPerKm  = maintenanceSpend / distance, or null
allInCostPerKm        = runningCostPerKm + maintenanceCostPerKm, or null
```

Maintenance is not folded into `runningCostPerKm`; it is reported separately unless the all-in view is explicitly requested.

Other period fields:

- `avgPricePaid = energySpend / energyAdded` (per vehicle, so units are consistent).
- `avgConsumptionPer100Km = 100 / verifiedEfficiency`, or null.
- `shareMeasuredVerifiedKm = measured verified km / verified km * 100`.
- `measuredOnlyEfficiency` and `estimatedOnlyEfficiency` split verified stretches by basis.
- `bestEfficiency`, `worstEfficiency`, and `latestStretchEff` use verified stretch efficiency values.

## Fleet totals and savings

`CalcEngine.fleetTotals(stats)` aggregates the selected period stats:

```text
fleet distance       = sum of vehicle distance
electricity spend    = sum of EV energy spend
petrol spend         = sum of petrol energy spend
battery wear         = sum of vehicle wear in period
running cost         = electricity + petrol + battery wear
maintenance spend    = sum of maintenance spend
fleet running ₹/km   = distance-weighted average of each vehicle's runningCostPerKm
```

Only vehicles with a non-null `runningCostPerKm` and positive distance participate in the weighted average. The most economical vehicle is the one with the lowest `runningCostPerKm` among those with a basis.

EV-vs-petrol savings:

```text
evCostPerKm     = distance-weighted EV running cost per km
petrolCostPerKm = distance-weighted petrol running cost per km
savings         = (petrolCostPerKm - evCostPerKm) * evDistance
```

`evCostPerKm` and `petrolCostPerKm` are `null` when no qualifying distance exists. Savings are `null` if either side lacks a basis or EV distance is zero; the UI does not invent a comparison. A negative result is possible and is shown honestly.

The comparison uses **running cost** (energy/fuel plus battery wear), not fuel price alone. Battery wear is relevant to the EV total and is not hidden to make the comparison look better.

## Maintenance reminders

For each maintenance record:

```text
hasReminder = nextDueDate != null || nextDueKm != null
daysLeft    = days between referenceDate and nextDueDate, or null
kmLeft      = nextDueKm - currentOdometer, or null
```

The latest record per `(vehicleId, category)` is selected by date then insertion order. A record with a reminder that is not the latest in its category is marked **Completed**. This automatically retires older reminders when a newer service record is logged.

For the latest pending record:

- **Overdue** if `daysLeft < 0` or `kmLeft < 0`.
- **Due Soon** if not overdue and `daysLeft <= warnDays` or `kmLeft <= warnKm`.
- **OK** otherwise.
- **Logged** if it has no reminder. Logged records are not pending.

The nearest reminder per vehicle sorts pending records by status rank (Overdue, Due Soon, OK) and then by `daysLeft`, with date-less reminders after date-based ones. The worker only notifies for Overdue and Due Soon, and skips archived vehicles.

Km-based reminders use the latest odometer in a non-error usage entry. The app does not estimate today's odometer from the calendar or a daily plan.

## Calibration curve

Calibration points are filtered by vehicle and sorted by voltage. For a voltage `V`:

- No points: percentage `null`, `missingCurve = true`, `outOfRange = true`.
- `V <= first.voltage`: return first percentage; `outOfRange = V < first.voltage`.
- `V >= last.voltage`: return last percentage; `outOfRange = V > last.voltage`.
- Between two points: linear interpolation.
- Duplicate voltage pair: interpolation returns the lower point's percentage; the calibration check flags `ERROR: Duplicate voltage for vehicle`.
- A flat or falling percentage curve: flagged `CHECK: Battery % does not rise with voltage`.
- Out-of-range voltage on a usage entry: CHECK; the clamped value is still used.

A missing curve for a voltage-derived entry is ERROR. A missing curve for a metered entry does not matter because no interpolation is needed.

## Data quality model

| Status | Meaning | Calculation participation |
|---|---|---|
| `OK` | Complete and within plausibility bands | Included |
| `CHECK` | Calculated but unusual; message explains why | Included and flagged |
| `ERROR` | Missing mandatory data, impossible value, rollback, or missing calibration | Excluded from calculations; retained in history |

The UI blocks saving a new entry whose preview would be ERROR. Existing ERROR records can be edited or deleted. The Usage Log lists ERROR records so the user can fix them.

## Number handling and formatting

- Dates are ISO `yyyy-MM-dd`.
- Numeric text parsing rejects NaN, infinities, and locale-dependent grouping. The editable number field accepts a decimal dot and treats blank as null.
- Calculations use full `Double` precision.
- Per-entry costs are rounded to two decimals with the JS-compatible `jsRound2`.
- Display precision varies by field: quantities to three decimals, prices and money to two, distances and efficiencies as appropriate. Display rounding never feeds back into stored data.

## Verification reference values

The synthetic demo dataset produces the following values in the `all` period. These are asserted by `CalcEngineTest` and `VerificationSuiteTest`.

| Metric | Value |
|---|---:|
| Demo EV 1 verified distance | 254.0 km |
| Demo EV 1 verified wall energy | 5.43 kWh |
| Demo EV 1 verified efficiency | 46.7 km/kWh |
| Demo EV 1 all-time average km/charge | 163.5 km |
| Demo EV 1 max running capacity | 408,742 km |
| Demo EV 1 age capacity | 38,325 km |
| Demo EV 1 distance considered | 38,325 km |
| Demo EV 1 battery price | ₹62,400 |
| Demo EV 1 battery wear | ₹1.63/km |
| Demo EV 1 verified energy cost | ₹0.16/km |
| Demo EV 1 running cost | ₹1.79/km |
| Demo EV 1 total running cost | ₹477.75 |
| Demo EV 2 fallback km/charge | 70 km |
| Demo EV 2 battery wear | ₹0.70/km |
| Petrol partial-refill stretch | 62.0 km/L |
| Voltage interpolation at 60.0 V | 55.0% |

### Why ₹477.75 is not ₹1.79 × 254

₹477.75 is the **period total**:

```text
all energy spend in the period (₹64.19) + battery wear for period distance (₹413.56)
```

₹1.79/km is the **unit rate**:

```text
verified energy cost per km (₹0.16) + battery wear per km (₹1.63)
```

The period total includes the baseline full-charge cost, which is not part of a verified stretch. The unit rate uses only verified stretch costs. Both figures are correct for their definitions, and the UI labels them separately.

## Preserved edge cases

The engine intentionally preserves these reference behaviours:

- A partial charge/refill never produces its own mileage result.
- A full fill with `includeInMileage = false` is not a boundary and contributes no energy to the denominator.
- A rollback is ERROR and breaks the current interval; the next full fill starts fresh.
- A first reading far above the initial odometer is a zero-distance baseline with a CHECK note.
- A missing price is a CHECK with zero cost, not an ERROR.
- An implausible stretch is flagged and excluded from averages only when the setting says so.
- The latest service record in a category retires older reminders as Completed.
- Reminders use only the latest logged odometer.
- Historical tariffs are date-effective and do not rewrite past costs.
- The `last_30` and `last_90` windows include both endpoints.
