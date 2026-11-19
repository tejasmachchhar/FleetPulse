package com.fleetpulse.app.support

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

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
    ): Boolean = emailChooserIntent(context, address, subject, body)?.let {
        runCatching {
            start(context, it)
            true
        }.getOrDefault(false)
    } ?: false

    /**
     * Builds a mailto: draft with the recipient, subject and body already
     * filled in. The chooser lets the user pick any installed email app; the
     * app never sends silently and cannot confirm delivery.
     */
    fun emailChooserIntent(
        context: Context,
        address: String,
        subject: String,
        body: String,
    ): Intent? {
        val mailto = Uri.parse("mailto:${Uri.encode(address, "@")}")
            .buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
        val intent = Intent(Intent.ACTION_SENDTO, mailto).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return chooserIntent(context, intent, "Send email")
    }

    /**
     * Resolves every installed UPI handler and builds a chooser from explicit
     * package intents. Resolution uses queryIntentActivities rather than
     * resolveActivity so a handler that is not registered as the default still
     * appears. The UPI app receives no amount; the payer enters it there.
     */
    fun upiChooserIntent(
        context: Context,
        uri: String = SupportConfig.UPI_PAYMENT_URI,
    ): Intent? {
        val payment = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        return chooserIntent(context, payment, "Pay with UPI")
    }

    fun startUpi(context: Context, uri: String = SupportConfig.UPI_PAYMENT_URI): Boolean =
        upiChooserIntent(context, uri)?.let {
            runCatching {
                start(context, it)
                true
            }.getOrDefault(false)
        } ?: false

    /**
     * Uses explicit package targets so Android 11+ package-visibility rules do
     * not silently hide an installed handler from the chooser. If only one
     * handler exists, the chooser is skipped.
     */
    private fun chooserIntent(
        context: Context,
        base: Intent,
        title: String,
    ): Intent? {
        val matches = queryActivities(context, base)
            .distinctBy { it.activityInfo.packageName }
        if (matches.isEmpty()) return null
        if (matches.size == 1) {
            return Intent(base).setPackage(matches.first().activityInfo.packageName)
        }
        val targets = matches.map { Intent(base).setPackage(it.activityInfo.packageName) }
        return Intent.createChooser(targets.first(), title).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.drop(1).toTypedArray())
        }
    }

    private fun queryActivities(context: Context, intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }

    private fun start(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
