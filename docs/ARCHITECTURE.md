# Architecture

FleetPulse is a single-activity Android app written in Kotlin with Jetpack Compose. It is intentionally local-only: Room is the source of truth, DataStore holds small preferences, and WorkManager performs a daily local reminder check. There is no HTTP client, no backend, and no `INTERNET` permission.

## Goals

- Keep the calculation engine pure Kotlin so it can be tested without Android.
- Make every stored value traceable to a user-entered record.
- Separate sample records from genuine records with `isSample`.
- Never silently hide a missing value behind a zero or a fabricated result.
- Keep data on the device unless the user explicitly exports it.
- Preserve the reference behaviour of the original FleetPulse v4 prototype while using native Android storage and UI.

## Layers

```text
┌──────────────────────────────────────────────────────────────┐
│ UI: Compose screens, navigation, dialogs, charts             │
│     MainActivity, screens/*, components/*, theme/*           │
└──────────────────────────────┬───────────────────────────────┘
                               │ UiSnapshot / actions
┌──────────────────────────────▼───────────────────────────────┐
│ Presentation: MainViewModel                                   │
│     StateFlow<UiSnapshot>, filters, imports, verification    │
└──────────────────────────────┬───────────────────────────────┘
                               │ EngineState
┌──────────────────────────────▼───────────────────────────────┐
│ Domain: pure Kotlin                                          │
│     CalcEngine, PetrolPricing, NumberText,                   │
│     VerificationSuite, SampleData                            │
└──────────────────────────────┬───────────────────────────────┘
                               │ entities and flows
┌──────────────────────────────▼───────────────────────────────┐
│ Data: Room, DataStore, backup/CSV, repository                │
│     FleetRepository, FleetDatabase, BackupManager,           │
│     AppPreferences                                           │
└──────────────────────────────┬───────────────────────────────┘
                               │ local storage
┌──────────────────────────────▼───────────────────────────────┐
│ Platform: Application, WorkManager, notifications, SAF        │
└──────────────────────────────────────────────────────────────┘
```

### UI layer

- `MainActivity` is the only activity. It enables edge-to-edge drawing, collects preferences and the UI snapshot, and shows either onboarding, a loading state, or the main scaffold.
- `MainScaffold` owns the `NavHost`, bottom navigation, FAB, and quick-entry bottom sheet.
- Top-level destinations are Dashboard, Vehicles, Usage Log, Analysis, Maintenance, and More. Detail destinations are Calibration, Settings, Data Management, User Guide, Calculation Verification, and the entry form.
- `Scaffold` uses `WindowInsets.safeDrawing`; full-screen editors use the same inset policy. Editable fields request bring-into-view when the IME inset changes.
- Screens receive immutable data and callbacks. They do not open the database directly.
- `ui/components/` contains shared cards, fields, empty states, and Canvas charts. No chart library is used.
- `ui/theme/` defines the Material 3 palette, typography, dark/light handling, and vehicle colours.

### Presentation layer

`MainViewModel` is an `AndroidViewModel` that combines the repository's Room/DataStore flows into one `UiSnapshot`:

```kotlin
data class UiSnapshot(
    val state: EngineState,
    val evaluation: EvalResult,
    val filter: DashboardFilter,
)
```

- `repo.state` is a combined `Flow` of vehicles, usage, maintenance, prices, calibration, and settings.
- `filter` holds the selected vehicle, period key, and custom range.
- `CalcEngine.evaluate(state, options)` runs whenever either source changes.
- `snapshot` is shared with `SharingStarted.WhileSubscribed(5_000)` to avoid recomputation when the UI is not collecting.
- `rawState` exposes the un-evaluated state for edit forms and import building.
- One-shot messages use a `MutableSharedFlow` to avoid duplicate toasts after configuration changes.
- Backup import is previewed in `lastImportPreview`; the write happens only after explicit confirmation.

The view model also owns validation that needs the full dataset, such as duplicate odometer detection, duplicate entry detection, and the "at least one vehicle must remain" rule.

### Domain layer

The domain layer has no Android dependencies.

- `CalcEngine` evaluates entries in date order, computes distances, energy, costs, full-to-full stretches, all-time statistics, battery wear, maintenance states, period metrics, and calibration checks.
- `PetrolPricing` implements the two-of-three quantity/unit-price/amount relationships.
- `NumberText` parses and formats editable decimal text without locale-dependent commas or silent coercion.
- `VerificationSuite` runs the same reference cases shown in the in-app verification screen.
- `SampleData` builds the synthetic demo dataset. Its numeric records are reference values; names and registration numbers are demo labels.

The engine returns immutable result types (`EvaluatedUsage`, `VehicleAllTimeStats`, `PeriodStats`, `EvaluatedMaintenance`, `CalibrationCheck`). The UI never recalculates a formula; it renders the engine's result and basis labels.

### Data layer

- `FleetDatabase` is a Room database with seven entities: vehicles, usage entries, maintenance entries, price history, calibration points, app settings, and app metadata.
- DAOs expose `Flow` queries for observation and `suspend` queries for writes/reads. Writes use `@Upsert` where appropriate.
- Foreign keys cascade from vehicles to usage, maintenance, and calibration. A vehicle deletion therefore removes its dependent records in one database operation.
- `FleetRepository` combines flows, provides demo load/remove, and performs restore in a `withTransaction`.
- `BackupManager` builds and parses the JSON v2 backup, normalises legacy v4 state, and generates evaluated CSV text.
- `AppPreferences` stores theme mode, onboarding completion, and notification enablement in DataStore Preferences.

### Platform layer

- `FleetPulseApp` creates the Room database, repository, and preferences, then creates the notification channel and schedules the unique periodic reminder work.
- `ReminderWorker` reads the database directly, evaluates maintenance with `CalcEngine`, and posts local notifications for overdue/due-soon reminders. It returns success even when notifications are disabled or permission is missing; the worker does not retry noisily.
- Backup export/import and CSV export use Android's Storage Access Framework through `ActivityResultContracts.CreateDocument` and `OpenDocument`.
- `FileProvider` is declared with a private cache path for any file sharing that needs a content URI. No files are shared automatically.

## Startup and onboarding

1. `Application.onCreate()` builds the database, repository, and preferences; creates the notification channel; schedules the daily worker.
2. `MainActivity` collects `AppPreferencesState`. Until DataStore has loaded, a loading screen is shown.
3. If onboarding is incomplete, the onboarding screen offers:
   - **Start clean** — creates default settings with the current chart month.
   - **Load the sample dataset** — creates demo settings and inserts the marked reference records.
4. After onboarding, the main scaffold observes the snapshot. Until the first Room emission arrives, a loading screen remains visible.
5. On Android 13+, notification permission is requested once after onboarding. Denial switches the notification preference off.

## Navigation and entry flow

- Bottom destinations use a single back-stack pattern (`popUpTo` start destination, `launchSingleTop`, `restoreState`) so quick actions cannot stack duplicate screens.
- The FAB opens a modal bottom sheet with four shortcuts: EV charge, petrol refill, ride/odometer only, and maintenance.
- Entry navigation uses a route with encoded `mode`, `vehicleId`, and `entryId` arguments. `QuickEntryScreen` adapts its fields to the selected vehicle's energy source and to edit/new mode.
- Vehicle and maintenance editors are full-screen dialogs with scrollable bodies. Settings, calibration, tariffs, and other forms use the shared keyboard-aware components.
- Notifications can deep-link to the maintenance screen through the `navigate` intent extra.

## Data flow for a saved entry

```text
User edits QuickEntryScreen
        │
        ▼
MainViewModel validation (required fields, duplicates, full-dataset checks)
        │
        ▼
FleetRepository.saveUsage()
        │
        ▼
Room upsert → Flow emission
        │
        ▼
combine(repo.state, filter)
        │
        ▼
CalcEngine.evaluate()
        │
        ▼
UiSnapshot → screens re-render
```

A save is asynchronous. The screen calls a view-model method, which launches in `viewModelScope`, writes to Room, and emits a one-shot message. The UI re-renders only from the resulting database emission; it does not mutate a local copy of the dataset.

## Backup and restore

- Export builds a `BackupBuildInput` from `rawState` and serialises JSON v2 with a format marker, version, timestamp, and data sections.
- Import parses and validates the file first. The view model stores the parsed `BackupPayload` as a preview; no database write occurs until the user confirms.
- Restore replaces all seven tables inside one Room transaction. If an unexpected exception occurs, the transaction rolls back and the existing dataset remains.
- Legacy v4 raw state JSON is accepted and normalised into the same `BackupPayload`.
- Missing `includeInMileage` values import as `true`, preserving pre-upgrade behaviour.

## Reminder flow

```text
WorkManager (daily, unique work)
        │
        ▼
ReminderWorker.doWork()
        │
        ├── notifications disabled? → success, no notification
        ├── load vehicles, entries, prices, calibration, settings
        ├── CalcEngine.evaluate(referenceDate = today)
        ├── keep pending Overdue / Due Soon reminders
        ├── skip archived vehicles
        └── post local notification with maintenance deep link
```

Km-based reminders compare `nextDueKm` with the latest odometer in a non-error usage entry. The app never estimates the current odometer from the calendar.

## Error handling and data quality

- The engine distinguishes ERROR (excluded from calculations), CHECK (included but flagged), and OK.
- An odometer rollback breaks the current stretch and is ERROR; the next valid full fill starts a fresh baseline.
- Missing energy price is a CHECK: the entry is retained, but its cost is zero and the user is warned.
- Missing voltage calibration is ERROR for a voltage-derived entry.
- Error and check messages are stored on the evaluated result so the Usage Log can explain exactly why an entry is flagged.
- New entries whose preview has ERROR status are blocked by the entry form.
- The app avoids `Double.NaN` and division-by-zero results; unavailable metrics are `null` and displayed as unavailable rather than zero.

## Threading

- Room and DataStore operations are `suspend`/`Flow` based and run off the main thread.
- The calculation engine is CPU-light and runs on the view-model's flow collector; it does not perform I/O.
- WorkManager runs the reminder worker on its own background executor.
- Compose collects state with `collectAsStateWithLifecycle` where appropriate and preserves local form state with `rememberSaveable`.

## Testing boundaries

- Pure engine tests instantiate `EngineState` directly with no Android runtime.
- Backup tests use `org.json` on the JVM.
- Petrol and number parsing tests are pure JVM.
- `VerificationSuiteTest` executes the in-app suite so a UI parity failure also fails CI.
- No instrumented tests are required for the CI build. Device/UI tests would need an emulator or physical device and are listed as a future improvement.

## Deliberate non-goals

- No cloud sync, account, or remote API.
- No automatic GPS trip detection.
- No OBD-II integration.
- No Play Store billing or subscription.
- No claim that estimated battery wear is an actual replacement invoice.
