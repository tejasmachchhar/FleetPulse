package com.fleetpulse.app.support

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Small, permission-free hand-offs to other Android apps.
 *
 * Opening a browser, share sheet, email app, or UPI app does not give
 * FleetPulse network access and does not transmit anything by itself. The user
 * chooses the destination and completes the action there.
 */
object SupportActions {

    fun openUrl(context: Context, url: String): Boolean = runCatching {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    }.getOrDefault(false)

    fun shareText(context: Context, text: String, chooserTitle: String = "Share report"): Boolean =
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            start(context, Intent.createChooser(send, chooserTitle))
            true
        }.getOrDefault(false)

    fun copyText(context: Context, label: String, text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    }.getOrDefault(false)

    fun email(
        context: Context,
        address: String,
        subject: String,
        body: String,
    ): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        start(context, intent)
        true
    }.getOrDefault(false)

    fun upiChooserIntent(
        context: Context,
        uri: String = SupportConfig.UPI_PAYMENT_URI,
    ): Intent? {
        val payment = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (payment.resolveActivity(context.packageManager) == null) return null
        return Intent.createChooser(payment, "Pay with UPI")
    }

    fun startUpi(context: Context, uri: String = SupportConfig.UPI_PAYMENT_URI): Boolean =
        upiChooserIntent(context, uri)?.let {
            runCatching {
                start(context, it)
                true
            }.getOrDefault(false)
        } ?: false

    private fun start(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
