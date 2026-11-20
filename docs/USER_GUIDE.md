# User Guide

FleetPulse is an offline Android app for tracking daily vehicle use, full-to-full mileage, fuel and charging costs, maintenance, and running costs. It is designed for a small personal fleet of electric and petrol two-wheelers, though the fields are general enough for other vehicles.

All data stays on the phone unless you export a backup or CSV file. The app has no account, server, ads, analytics, or internet permission.

## Install and first launch

### Install

1. Download the APK from the repository's Releases page.
2. Open the APK on the phone.
3. Allow installation from the browser or file manager if Android asks.
4. Release assets are labelled **release** or **debug**. A debug/development APK is for testing and is not a production-signed build.

FleetPulse requires Android 8.0 (API 26) or newer.

### First-run choice

The first launch offers two choices:

- **Start clean** — creates empty settings with no vehicles or records.
- **Load sample data** — adds three synthetic demo vehicles (`Demo EV 1`, `Demo EV 2`, `Demo Petrol`) and the reference records. Everything is marked `[SAMPLE]`.

Choose clean for real use. The sample mode is useful for exploring the screens and running the calculation verification suite. Sample data can be removed later without deleting genuine records.

### Notification permission

On Android 13 and newer, the app asks for notification permission after onboarding. If you deny it, the notification preference is switched off. You can enable notifications later in Settings and grant the permission in Android's app settings.

## Navigation

The bottom bar contains:

- **Dashboard** — fleet overview and filters.
- **Vehicles** — add, edit, archive, and delete vehicles.
- **Usage Log** — all usage records with status filters.
- **Analysis** — mileage, cost, energy, and savings details.
- **Maintenance** — service records and reminders.
- **More** — Feedback & Support, About FleetPulse, Calibration, Settings, Data Management, User Guide, and Calculation Verification.

The floating **+** button opens the quick-entry sheet: EV charge, petrol refill, ride/odometer only, or maintenance.

Detail screens hide the bottom bar. Use the back arrow to return.

## Vehicles

Open **Vehicles → Add vehicle**.

### Common fields

- **Name** — a short display name.
- **Vehicle ID** — a stable ID. A suggested value is filled in; changing it later can break references, so keep it simple.
- **Type** — free text, such as `Electric Bike`.
- **Energy source** — `Electricity` or `Petrol`. This changes the entry form and the available calculations.
- **Registration** — optional. Do not put personal information in a public screenshot or issue.
- **Purchase date** and **initial odometer** — the reading when tracking began.
- **Colour** and **notes** — display and reference only.
- **Archived** — hides the vehicle from active workflows while preserving history.

### Electric vehicle fields

- **Battery capacity (kWh)** — usable capacity.
- **Battery rate (₹/kWh)** — replacement cost per kWh.
- **Battery life (years)** — expected service life.
- **Charge cycles** — expected full-charge cycles.
- **Rated km/charge** — fallback when no verified stretch exists.
- **Daily km plan** — used for the age-capacity limit.

Battery wear is an allowance based on these values; it is not a real replacement invoice.

### Petrol vehicle fields

- **Tank capacity (L)** — used for a plausibility check when a refill exceeds the tank.

### Vehicle actions

- **Edit** — change fields. A new usage entry is not required.
- **Archive** — preserve all history but hide the vehicle from active selection. Restore it later.
- **Delete** — permanently removes the vehicle and its usage, maintenance, and calibration records. The app refuses to delete the last vehicle. Confirm carefully.

An archived vehicle still appears in historical analysis if selected, but reminder notifications skip archived vehicles.

## Quick entry

The form changes based on the vehicle's energy source and the selected shortcut.

### EV charge

Choose an EV and a fill type:

- **Full charge** — the battery is charged to 100%.
- **Partial charge** — the battery is charged to a level below 100%.
- **Ride only** — records distance with no charging energy.

Enter either:

- **Metered wall energy (kWh)** — the most accurate option; the app labels it `Measured`; or
- **Volts before** — for a full charge; the app interpolates the battery percentage from the calibration curve and uses 100% as the after value; or
- **Volts before and after** — for a partial charge.

If you use voltage mode, the EV needs calibration points. Without a curve, the entry is an error and cannot be saved as a new entry.

### Petrol refill

Enter the quantity in litres and optionally the unit price or total amount. The app can derive the third value:

- quantity + unit price → amount
- amount + quantity → unit price
- amount + unit price → quantity

The derived field is labelled. Leaving the unit price blank lets the historical tariff apply automatically.

### Ride/odometer only

Records the odometer and distance without energy or fuel. Use it for a trip reading, a baseline, or a non-refuel event.

### Maintenance shortcut

Opens the maintenance form directly. You can also add a maintenance record from the Maintenance screen.

### Include this entry for mileage estimation

This checkbox is on by default. Turn it off when the energy/fuel quantity should not affect the full-to-full mileage denominator, for example:

- a partial charge whose kWh you do not trust;
- a free or undocumented top-up;
- an entry you want in history and expenses but not in the efficiency calculation.

Effects:

- The entry's energy/fuel is not added to the mileage denominator.
- The entry cannot open or close a full-to-full stretch.
- Its odometer distance is still real travel between readings and remains part of the stretch distance.
- Its cost remains in expenses and history.

### Validation

The form previews the entry before saving:

- **ERROR** — missing mandatory data, rollback, missing calibration, or impossible values. A new entry with an ERROR preview is blocked.
- **CHECK** — unusual but usable, such as a duplicate reading, a large gap, an out-of-range voltage, or implausible efficiency. The entry can be saved and remains flagged.
- **OK** — complete and within the configured bands.

Duplicate readings and duplicate same-date/type/odometer entries are rejected to prevent accidental double entry.

## Dashboard

The Dashboard shows fleet-level and per-vehicle information.

### Filters

- **Vehicle** — all vehicles or one vehicle.
- **Period** — all data, current month, last 30/90 days, current/previous financial year, or a custom range.

The period includes both endpoints. The `last_30`/`last_90` labels match the reference implementation even though both endpoints are inclusive.

### Cards and charts

- Total distance, energy/fuel spend, battery-wear allowance, running cost per km, maintenance, and EV-vs-petrol savings.
- Fuel/energy chart and efficiency trend.
- Per-vehicle spotlight with current odometer and reminder status.
- Data-quality count for entries needing attention.
- **Sample Mode** banner when demo records are present.

Unavailable values are shown as unavailable, not as zero.

### Interpreting efficiency

The app distinguishes:

- **Verified** — completed full-to-full stretches.
- **Provisional** — non-error distance divided by eligible energy when no verified stretch exists.
- **Not enough data** — no usable denominator.

The label is always shown. A provisional result is not equivalent to a verified one.

## Usage Log

The Usage Log lists records by date.

- Filter by vehicle and status: all, OK, CHECK, ERROR.
- Each card shows the date, vehicle, fill type, odometer, distance, energy/fuel, cost, basis, status, and eligibility.
- **Edit** opens the same quick-entry form in edit mode.
- **Delete** asks for confirmation and removes the record.
- ERROR entries explain what must be fixed. They remain in history but are excluded from calculations.

## Analysis

Analysis has three tabs: mileage, cost, and energy.

- Verified vs provisional detail and the basis used.
- EV km/kWh and petrol km/L trends are separate; the app does not mix units.
- Battery-wear breakdown and the cycle/age capacity limit.
- Running-cost split and maintenance cost.
- EV-vs-petrol savings when both sides have a valid cost basis; otherwise unavailable.
- Historical tariff list and the tariff in effect for each entry.

Savings use running cost (energy/fuel plus battery wear), not fuel price alone. A negative saving is possible and is shown honestly.

## Maintenance

Open **Maintenance** or use the quick-entry shortcut.

Fields:

- date
- vehicle
- odometer
- category
- description
- amount
- next due date (optional)
- next due km (optional)
- remarks

### Reminder states

- **Overdue** — the due date or due km has passed.
- **Due Soon** — within the configured warning days or km.
- **OK** — has a pending reminder but is not near it.
- **Completed** — an older record in the same vehicle/category has been superseded by a newer record.
- **Logged** — no reminder is attached.

The latest record per vehicle/category owns the pending reminder. Logging a newer service record automatically retires the older reminder. This prevents duplicate notifications for repeated work.

Km-based reminders compare the next-due km with the latest odometer you actually logged. The app does not estimate the current odometer from the calendar.

### Notifications

The app schedules a daily local check through WorkManager. It notifies for Overdue and Due Soon reminders on active (non-archived) vehicles. Notifications open the Maintenance screen.

Notifications are local only. No data leaves the device. Android 13+ requires notification permission.

## Calibration

Calibration is only needed for EVs where you record voltage instead of metered kWh.

1. Open **More → Calibration**.
2. Choose an EV.
3. Add voltage/percentage points from the manufacturer's data or your own measurements.
4. Use the live voltage checker to see the interpolated percentage.

Rules:

- Points are sorted by voltage.
- Values between points are interpolated linearly.
- A voltage below the lowest point is clamped to the lowest percentage and flagged.
- A voltage above the highest point is clamped to the highest percentage and flagged.
- Duplicate voltages are errors.
- A flat or falling percentage curve is flagged.
- A missing curve makes a voltage-derived entry an error.

The demo curve is a typical example, not a measurement of your pack. Replace it with your own data.

## Settings

Settings contains:

- **Behaviour** — charger efficiency, max plausible km between readings, exclude implausible efficiency.
- **Plausible efficiency band** — EV min/max in km/kWh; petrol min/max in km/L.
- **Reminders** — warning days and warning km.
- **Chart start month** — the first month shown in charts.
- **Maintenance categories** — one category per line.
- **Appearance and notifications** — theme and local reminders.
- **Tariffs** — historical ₹/kWh and ₹/L prices with effective dates.

Changing a tariff does not rewrite old entry costs. An explicit unit price on an entry always wins over the tariff.

## Data management

Open **More → Data Management**.

### Export JSON backup

Choose a destination through Android's file picker. The backup contains vehicles, usage, maintenance, prices, calibration, and settings in format v2. `includeInMileage` is included.

Back up regularly. A local database can be lost if the phone is lost, reset, or the app is uninstalled.

### Import JSON backup

1. Choose a backup file.
2. Review the preview: vehicles, usage, maintenance, prices, calibration, and settings.
3. Confirm the restore.

Restore replaces the entire local dataset in one transaction. If the write fails, the existing data remains. Import accepts format v2 and the legacy FleetPulse v4 raw state JSON. Old files without `includeInMileage` import as included.

Importing a backup replaces current data. Export a fresh backup first if you are unsure.

### Export CSV

- **Usage CSV** — evaluated usage rows, including status, basis, eligibility, and computed metrics.
- **Maintenance CSV** — maintenance records and computed due states.

CSV files are for spreadsheets and review. They are not a restore source.

### Sample data

- **Load samples** adds the synthetic demo dataset without overwriting records with the same IDs.
- **Remove samples** deletes only rows flagged `isSample`. Genuine usage, maintenance, and calibration remain. Demo vehicles are removed only when no genuine record references them. Sample tariffs remain while genuine usage still depends on them, so historical costs do not change.

## Feedback and Support

Open **More → Feedback & Support**. The centre has five actions. Project links and build details are in **More → About FleetPulse**.

- **Report a Bug** — choose a category such as crash, incorrect calculation, missing data, UI, navigation, or performance; optionally add one sentence.
- **Suggest an Improvement** — choose a category and write a short suggestion. Problem and expected-behaviour fields are optional.
- **Send General Feedback** — a short note is enough.
- **Email Feedback & Support** — send a bug, improvement, or general note to the support address without a GitHub account.
- **Support FleetPulse** — optional UPI, Buy Me a Coffee, and GitHub Sponsors when configured.

### What the app fills in

The report can include:

- the screen name, such as `Dashboard` or `Usage entry · EV charge`;
- FleetPulse version and version code;
- Android version, device manufacturer and model;
- screen size, density, language, and timestamp;
- redacted recent diagnostics, exception type and stack frames for bug reports.

It never includes vehicle names or registrations, odometer readings, fuel/charging records, expenses, notes, database contents, location, files, or credentials. The screen context is only a label; the actual values on the screen are not read into the report.

### Preview and consent

1. Tap **Review report**.
2. Read the exact text in the preview.
3. If needed, cancel and turn off app version, device/environment, or recent diagnostics on the form.
4. Tick the consent checkbox.
5. Choose one destination:
   - **Open GitHub issue** — opens a prefilled issue page. GitHub may ask you to sign in. FleetPulse cannot press the submit button or confirm that an issue was posted.
   - **Share** — opens the Android share sheet with the report text.
   - **Copy** — copies the report to the clipboard.
   - **Email report** — opens your email app with the report as a draft. This is available on the GitHub preview and from the dedicated email flow.

Opening GitHub is not a submission. The app never says an issue was created just because a browser opened. Opening an email app does not send the email; you must press Send there.

Long reports are shortened locally so the prefilled GitHub link stays reliable. The preview shows the exact shortened text and a note that it was shortened; Share and Copy use the same text. Shorten the description or turn off recent diagnostics to keep more of the technical detail.

### Email feedback

Open **More → Feedback & Support → Email Feedback & Support**:

1. Choose **Report a Bug**, **Suggest an Improvement**, or **General Feedback**.
2. Fill only the fields that help explain the issue. Bug steps and expected/actual behaviour are optional.
3. Leave app version, Android/device details, and recent diagnostics on or turn them off.
4. Tap **Review email draft** and read the exact subject and body.
5. Tick the consent box, then tap **Open email app**. The recipient `tejasmachchhar2019@gmail.com`, subject, and body are prefilled; you can edit them before sending.
6. If no email app is installed, use **Copy email**, **Share draft**, or **Copy support address** instead.

FleetPulse cannot send email itself and cannot confirm delivery. The support address is shown on the screen and can be copied separately.

### Contextual report buttons

A small bug icon on Dashboard, Usage Log, Analysis, Maintenance, and Quick Entry opens the bug form with that screen already filled in. A failed backup import shows a **Report problem** action. These shortcuts do not add personal records to the report.

### Local diagnostics

FleetPulse keeps a small local diagnostics log for troubleshooting. It is stored in the app's private storage, capped at two rotated files of 64 KiB, and redacted before writing. It is read into a report only when **Recent diagnostics** is enabled. It is never uploaded automatically and there is no crash-reporting service.

## About and project links

Open **More → About FleetPulse** for the verified project destinations:

- GitHub repository;
- documentation and user guide;
- changelog and release notes;
- latest release and APK downloads;
- privacy information;
- support and privacy-safe reporting guide.

The screen also shows the current version, version code, build type, and package name. Feedback & Support stays focused on reporting and assistance instead of repeating the same link list.

## Calculation Verification

Open **More → Calculation Verification** and tap **Run verification**. The screen runs the same reference suite as the JVM tests:

- workbook parity values;
- full-to-full boundaries;
- partial refill/charge behaviour;
- eligibility;
- battery wear;
- historical tariffs;
- calibration interpolation and range checks;
- maintenance retirement;
- edge cases.

A failure means the engine does not match the expected reference values. Report it with a synthetic reproduction, not a real backup.

## Data safety and privacy

- No internet permission, analytics, ads, or cloud account.
- Android auto-backup is disabled.
- The database and preferences are in the app's private storage.
- Backups and CSV files go only where you choose through the system file picker.
- Exported files are outside the app's protection once written. Store them securely.
- The feedback preview shows the exact report before any external action; optional diagnostics can be disabled and consent is required.
- The local diagnostics log is capped at two 64 KiB files and is read into a report only when you leave **Recent diagnostics** on.
- UPI and Buy Me a Coffee are optional. FleetPulse does not verify payments or store payment credentials.
- Email feedback opens your email app with a reviewed draft. FleetPulse does not send email silently or confirm delivery.
- The app does not encrypt the database with a user password; it relies on the Android app sandbox and device encryption.
- Deleting the app or clearing its storage deletes the local database. Export a backup first.

See [PRIVACY.md](../PRIVACY.md) for the full statement.

## Troubleshooting

### A new entry is blocked as ERROR

Check the message on the form. Common causes:

- missing odometer;
- an odometer lower than the previous reading;
- an EV voltage entry with no calibration curve;
- a partial voltage charge without Volts After;
- a full/partial charge with neither metered kWh nor voltage;
- a petrol refill with no quantity.

Fix the entry or edit the calibration.

### My efficiency is provisional

A verified result needs a completed full-to-full stretch with positive distance and eligible energy. Partial fills alone cannot produce one. Check that the opening and closing entries are full fills and that the eligibility checkbox is on.

### The total cost and the per-km cost do not match

They answer different questions. The per-km rate uses verified stretch costs; the period total includes every saved entry's energy/fuel cost, including a baseline or partial fill. See the “Why ₹477.75 is not ₹1.79 × 254” section in [CALCULATION_LOGIC.md](CALCULATION_LOGIC.md).

### A reminder is marked Completed

A newer record in the same vehicle/category supersedes the older reminder. This is intentional. Edit the newer record if its next-due values are wrong.

### I denied notification permission

Enable notifications in Settings, then grant the permission in Android's app settings. The daily worker skips notifications when the preference is off or permission is missing.

### The app has no data after reinstalling

Uninstalling removes the local database. Android auto-backup is disabled, so restore from your own JSON backup.

### I need to move to a new phone

1. Export a JSON backup on the old phone.
2. Install FleetPulse on the new phone.
3. Complete, skip, or replay the onboarding from Settings → Help & Onboarding.
4. Import the backup and confirm.

### A screenshot contains my registration number

Do not publish it. Use the synthetic demo dataset or redact the image before sharing. The same applies to issue reports and logs.

## FAQ

**Does the app need internet?** No. It has no `INTERNET` permission.

**Does it sync between phones?** No. Export/import is the transfer mechanism.

**Can I track a car?** The fields are general, but the interface and defaults are tuned for two-wheelers. You can enter a car as a vehicle and use the same calculations.

**Does it read the vehicle's computer?** No OBD-II or GPS integration.

**Is battery wear money I spent?** No. It is an allowance per km based on your replacement cost and expected life. It is reported separately from energy spend.

**Are the demo values real?** No. Names, registration, and records are synthetic demo/reference values. Replace them with your own data.

**Can I use miles or gallons?** The current UI uses kilometres, litres, kWh, and ₹. Unit conversion is not implemented.

**Can I delete all sample data?** Yes, from Data Management. Genuine records are preserved.

## Universal vehicle categories

FleetPulse supports two-wheelers, three-wheelers, passenger vehicles, commercial vehicles, agricultural machinery, construction equipment, and other vehicle types. New categories are added through the Vehicle Reference Database; the app does not hardcode them.

Agricultural and construction vehicles are tracked in operating hours, while road vehicles use distance in km. Both are displayed on Dashboard, Usage Log, and the Quick Entry form as appropriate.
