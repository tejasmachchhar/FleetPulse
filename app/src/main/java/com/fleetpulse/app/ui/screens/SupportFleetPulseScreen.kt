package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.QrCode
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetViolet

@Composable
fun SupportFleetPulseScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showThanks by rememberSaveable { mutableStateOf(false) }
    var showNoUpi by rememberSaveable { mutableStateOf(false) }

    val upiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // The activity result is not a payment confirmation. It only tells us
        // that control returned; the thank-you dialog says so explicitly.
        showThanks = true
    }

    fun copyUpi() {
        if (SupportActions.copyText(context, "FleetPulse UPI ID", SupportConfig.UPI_ID)) {
            Toast.makeText(context, "UPI ID copied.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not copy the UPI ID.", Toast.LENGTH_SHORT).show()
        }
    }

    fun open(url: String) {
        if (!SupportActions.openUrl(context, url)) {
            Toast.makeText(context, "No app is available to open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Support FleetPulse",
                subtitle = "Optional support for independent open-source development",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Why support?") {
                Text(
                    "FleetPulse is developed and maintained independently. Voluntary contributions help with " +
                        "new features, bug fixes, testing across devices, compatibility updates, and continued " +
                        "open-source maintenance.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Donations are entirely optional. Every tracking feature, backup option and calculation " +
                        "stays available without paying.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FleetGreen,
                )
            }
        }

        item {
            SectionCard(
                title = "UPI · primary option",
                subtitle = "Scan the QR code or open any installed UPI app",
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    QrCode(
                        content = SupportConfig.UPI_PAYMENT_URI,
                        contentDescription = "UPI payment QR code for ${SupportConfig.UPI_ID}",
                    )
                }
                Text(
                    "Scan with any UPI app. The QR code contains the same UPI ID and payee name shown below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        null,
                        tint = FleetBlue,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("UPI ID", style = MaterialTheme.typography.labelSmall)
                        Text(
                            SupportConfig.UPI_ID,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = { copyUpi() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copy")
                    }
                }
                Button(
                    onClick = {
                        val intent = SupportActions.upiChooserIntent(context)
                        if (intent == null) {
                            showNoUpi = true
                        } else {
                            upiLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Pay using UPI")
                }
                Text(
                    "No fixed amount. Enter any amount in your UPI app. FleetPulse does not see, store or verify " +
                        "your payment credentials or transaction.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Other verified options",
                subtitle = "Only configured, publicly available links are shown",
            ) {
                SupportConfig.BUY_ME_A_COFFEE_URL?.let { url ->
                    SupportLinkRow(
                        icon = Icons.Default.LocalCafe,
                        title = "Buy Me a Coffee",
                        subtitle = "Open the maintainer's verified support page",
                        tint = FleetAmber,
                        onClick = { open(url) },
                    )
                }
                SupportConfig.GITHUB_SPONSORS_URL?.let { url ->
                    SupportLinkRow(
                        icon = Icons.Default.FavoriteBorder,
                        title = "GitHub Sponsors",
                        subtitle = "Sponsor through the verified GitHub profile",
                        tint = FleetViolet,
                        onClick = { open(url) },
                    )
                }
                if (SupportConfig.GITHUB_SPONSORS_URL == null) {
                    Text(
                        "GitHub Sponsors is not enabled for this maintainer yet. It will appear here only after a " +
                            "public GitHub Sponsors profile is configured; FleetPulse does not show an unverified link.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "No other payment provider is configured. Razorpay or another provider will only be added after " +
                        "the account, URL, fees and payout requirements are verified; no payment SDK or custom " +
                        "backend is included today.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "Thank you for supporting independent open-source development.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }

    if (showNoUpi) {
        AlertDialog(
            onDismissRequest = { showNoUpi = false },
            title = { Text("No UPI app found") },
            text = {
                Text(
                    "Install or enable a UPI application, or ask someone to scan the QR code. " +
                        "You can also copy the UPI ID below.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    copyUpi()
                    showNoUpi = false
                }) { Text("Copy UPI ID") }
            },
            dismissButton = { TextButton(onClick = { showNoUpi = false }) { Text("Close") } },
        )
    }

    if (showThanks) {
        AlertDialog(
            onDismissRequest = { showThanks = false },
            title = { Text("Thank you for supporting FleetPulse!") },
            text = {
                Text(
                    "FleetPulse cannot verify whether a payment was completed. Please check your UPI app for " +
                        "the transaction status. No payment credentials or transaction details are stored by FleetPulse.",
                )
            },
            confirmButton = { TextButton(onClick = { showThanks = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun SupportLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.OpenInNew,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
