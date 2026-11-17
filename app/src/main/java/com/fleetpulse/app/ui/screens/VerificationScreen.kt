package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.domain.VerificationResult
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed

@Composable
fun VerificationScreen(
    results: List<VerificationResult>,
    onRun: () -> Unit,
    onBack: () -> Unit,
) {
    var showFailuresOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (results.isEmpty()) onRun()
    }

    val passed = results.count { it.pass }
    val failed = results.size - passed
    val shown = if (showFailuresOnly) results.filter { !it.pass } else results

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Calculation verification",
                subtitle = "Reference workbook parity and edge cases",
                onBack = onBack,
            )
        }

        item {
            SectionCard(
                title = if (results.isEmpty()) "Running…" else if (failed == 0) "All checks passed" else "$failed checks failed",
                subtitle = if (results.isEmpty()) null else "$passed of ${results.size} passed",
            ) {
                if (results.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Evaluating the reference dataset…", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                (if (failed == 0) FleetGreen else FleetRed).copy(alpha = 0.10f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (failed == 0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (failed == 0) FleetGreen else FleetRed,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (failed == 0) {
                                "The native engine reproduces every reference value. Unit tests cover the same cases."
                            } else {
                                "One or more values differ from the reference. Do not rely on the affected figures; " +
                                    "report the mismatch with the expected and actual values shown below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = showFailuresOnly,
                        onClick = { showFailuresOnly = !showFailuresOnly },
                        label = { Text("Failures only") },
                    )
                    TextButton(onClick = onRun) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run again")
                    }
                }
            }
        }

        items(shown, key = { it.name }) { result ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (result.pass) MaterialTheme.colorScheme.outlineVariant else FleetRed.copy(alpha = 0.6f),
                ),
            ) {
                Row(Modifier.padding(12.dp)) {
                    Icon(
                        if (result.pass) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (result.pass) "Pass" else "Fail",
                        tint = if (result.pass) FleetGreen else FleetRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(result.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Expected: ${result.expected}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "Actual:   ${result.actual}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (result.pass) MaterialTheme.colorScheme.onSurfaceVariant else FleetRed,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "What is checked") {
                Text(
                    "• Demo EV 1 full-to-full distance, wall energy, km/kWh, km per charge and battery capacity limits\n" +
                        "• 20-to-80% partial charge energy using the calibration curve\n" +
                        "• Voltage interpolation, outside-range clamping and duplicate/inverted points\n" +
                        "• Battery wear inputs, petrol full-tank mileage and cost per km\n" +
                        "• Historical tariff selection by date and fleet totals\n" +
                        "• Maintenance reminder retirement and overdue states\n" +
                        "• Data quality ERROR/CHECK/OK rules and duplicate odometer handling\n\n" +
                        "The identical suite runs in app/src/test as JVM tests, so a green build and a green screen mean the same thing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onRun,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Re-run verification") }
            }
        }
    }
}
