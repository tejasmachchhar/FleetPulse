package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard

@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "User guide",
                subtitle = "What every number means and how it is calculated",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Quick start") {
                GuideText(
                    "1. On first launch choose clean data, or load the marked sample fleet to explore.\n" +
                        "2. Vehicles → Add: enter battery capacity, replacement rate, life and cycles (EV) or tank capacity (petrol).\n" +
                        "3. Use the + button → EV charge / Petrol refill / Ride only / Maintenance, or open Usage Log.\n" +
                        "4. Enter the odometer and either the battery voltage pair or the metered kWh (EV) / litres (petrol).\n" +
                        "5. Dashboard, Analysis and Maintenance update instantly from the same calculation engine.",
                )
            }
        }

        item {
            SectionCard(title = "Full-to-full methodology") {
                GuideText(
                    "Mileage is only trusted between two full fills — a full charge for EVs or a full tank for petrol.\n\n" +
                        "• A verified stretch starts at a full fill and ends at the next full fill.\n" +
                        "• Distance = ending odometer − starting odometer.\n" +
                        "• Energy = every unit added during the stretch, including partial fills in between.\n" +
                        "• Efficiency = total verified distance ÷ total verified energy. It is a weighted result, never an average of averages.\n" +
                        "• EV km/charge is calculated for each verified stretch as stretch distance × usable capacity ÷ (stretch wall energy × charger-efficiency fraction), then averaged over verified stretches.\n" +
                        "• Partial fills and single rides never produce a mileage figure of their own.\n" +
                        "• Incomplete stretches at the edges of the log are shown as Provisional when used at all.",
                )
            }
        }

        item {
            SectionCard(title = "EV energy: voltage vs meter") {
                GuideText(
                    "Voltage mode: the app converts pack voltage to battery % using your calibration curve, then estimates energy.\n\n" +
                        "energy (kWh) = (% after − % before) ÷ 100 × usable capacity ÷ charger efficiency\n\n" +
                        "• A full charge starts at 0% before and ends at 100% after, so only the before-voltage is needed.\n" +
                        "• Meter mode uses the wall meter reading directly and is always preferred when available.\n" +
                        "• Measured energy is labelled Measured; voltage-derived energy is labelled Estimated.\n" +
                        "• Charger efficiency defaults to 89% and is adjustable in Settings.",
                )
            }
        }

        item {
            SectionCard(title = "Battery wear estimator") {
                GuideText(
                    "Battery wear is an allowance for replacement cost, not money spent this month.\n\n" +
                        "battery price = usable capacity × replacement rate per kWh\n" +
                        "cycles capacity = cycles × km per charge\n" +
                        "age capacity = life years × 365 × planned km per day\n" +
                        "distance considered = smaller of the two capacities above\n" +
                        "wear cost per km = battery price ÷ distance considered\n\n" +
                        "Km per charge uses the all-time average of verified full-to-full stretches for the battery model; " +
                        "if none exists it uses the rated km per charge, and the dashboard says which source was used. " +
                        "Swap bikes/batteries → edit the vehicle so the model stays honest.",
                )
            }
        }

        item {
            SectionCard(title = "Running cost, tariffs and savings") {
                GuideText(
                    "• Energy spend uses the unit price stored on the entry; if blank, the historical tariff effective on that date is used.\n" +
                        "• Tariffs are never applied retroactively: adding a new price does not change past entries.\n" +
                        "• Running cost = energy spend + battery wear allowance. Maintenance is reported separately and is never mixed into ₹/km running cost.\n" +
                        "• EV vs petrol savings = (petrol cost per km − electric cost per km) × electric distance. Both costs are distance-weighted for the selected period.\n" +
                        "• If either side has no cost basis, savings show as unavailable rather than inventing a number.",
                )
            }
        }

        item {
            SectionCard(title = "Data quality: ERROR, CHECK, OK") {
                GuideText(
                    "ERROR — the entry cannot be used in any calculation: missing mandatory data, missing calibration curve, " +
                        "or a value that makes the maths impossible. ERROR rows are kept and listed so nothing is lost.\n\n" +
                        "CHECK — the maths ran but something looks unusual: an implausible efficiency, a voltage outside the calibrated range, " +
                        "or a first reading far above the initial odometer. CHECK rows are included but flagged.\n\n" +
                        "OK — all required data present and within configured plausibility bounds.\n\n" +
                        "New entries with an ERROR preview are blocked in Quick Entry. Existing ERROR records can always be edited or deleted manually.",
                )
            }
        }

        item {
            SectionCard(title = "Maintenance and reminders") {
                GuideText(
                    "• A record with a next-due date or next-due odometer is a reminder.\n" +
                        "• Logging a newer record in the same category for the same vehicle automatically retires the older reminder (Completed).\n" +
                        "• Overdue / Due Soon use the warn-days and warn-km thresholds in Settings; km reminders use your latest logged odometer only.\n" +
                        "• Notifications are scheduled locally once a day; no server is involved.\n" +
                        "• Maintenance spend never changes running cost per km — it is shown as a separate all-in figure.",
                )
            }
        }

        item {
            SectionCard(title = "Backups, imports and sample data") {
                GuideText(
                    "• Export JSON creates a complete backup: vehicles, usage, maintenance, tariffs, calibration and settings.\n" +
                        "• Import previews the file and asks for confirmation before replacing anything. Restore is all-or-nothing.\n" +
                        "• Legacy raw state JSON from FleetPulse v4 web is accepted.\n" +
                        "• CSV exports contain evaluated rows, including basis, status and check messages.\n" +
                        "• Sample rows are flagged in the database, shown with [SAMPLE], and removable as a group. Removing them never touches your own records.",
                )
            }
        }

        item {
            SectionCard(title = "Frequently asked") {
                GuideText(
                    "Why is my first EV entry 0 km?\n" +
                        "There is no previous full charge to measure from. The first reading sets the starting point. If it is far above the " +
                        "vehicle's initial odometer it is flagged CHECK rather than silently producing a huge fake distance.\n\n" +
                        "Why does a full charge show litres/kWh but no km/L?\n" +
                        "A full fill closes a stretch; the mileage appears on that entry only after the next full fill.\n\n" +
                        "Why is battery wear included in running cost?\n" +
                        "It reflects real consumption of battery life. It is displayed separately so you can see energy cost alone.\n\n" +
                        "What if I delete the tariff a past entry used?\n" +
                        "Entries with their own stored unit price keep it. Entries relying on the tariff fall back to the nearest older tariff; " +
                        "if none exists the entry is flagged CHECK and its energy cost is treated as zero, never silently invented.\n\n" +
                        "Does the app need internet?\n" +
                        "No. It has no INTERNET permission. Backups use Android's system file picker.",
                )
            }
        }

        item {
            SectionCard(title = "Verification suite") {
                GuideText(
                    "More → Calculation Verification runs the reference workbook cases against the same engine used by the UI. " +
                        "It checks full-to-full periods, voltage interpolation, battery wear, tariffs, fleet totals and retirement logic. " +
                        "The same suite also runs as JVM unit tests in the project source.",
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "FleetPulse native edition · formulas preserved from the v4 reference implementation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GuideText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal,
    )
}
