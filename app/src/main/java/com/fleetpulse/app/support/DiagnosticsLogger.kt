package com.fleetpulse.app.support

import android.content.Context
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Lightweight local diagnostics with a hard size cap and no automatic upload.
 *
 * The log contains component names, timestamps, and redacted exception text.
 * It never contains repository/database contents, vehicle names, registration
 * numbers, odometers, expenses, notes, location, or file contents.
 */
object DiagnosticsLogger {

    private const val LOG_DIR = "diagnostics"
    private const val LOG_NAME = "fleetpulse-diagnostics.log"
    private const val ROTATED_NAME = "fleetpulse-diagnostics.1.log"
    private const val MAX_BYTES = 64 * 1024L
    private const val RECENT_LIMIT = 6_000

    private val lock = Any()
    private val timestampFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @Volatile
    private var installed = false

    @Volatile
    private var logFile: File? = null

    fun install(context: Context) {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            val directory = File(context.applicationContext.filesDir, LOG_DIR)
            if (!directory.exists() && !directory.mkdirs()) return
            logFile = File(directory, LOG_NAME)
            installed = true
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                append("CRASH", "Uncaught-${thread.name}", safeStackTrace(throwable))
            }
            // Never swallow a crash. Delegate to Android's normal handler after
            // the local record is written.
            previous?.uncaughtException(thread, throwable)
        }

        logEvent("Application", "Diagnostics initialised for FleetPulse ${BuildInfo.VERSION_NAME}")
    }

    fun logEvent(component: String, message: String) {
        append("INFO", component, message)
    }

    fun logError(component: String, throwable: Throwable) {
        append("ERROR", component, safeStackTrace(throwable))
    }

    fun logError(component: String, message: String) {
        append("ERROR", component, message)
    }

    /**
     * Exception messages can contain file names, record labels, or values from
     * a parser. Diagnostics keep the exception type and stack frames, but not
     * the message itself. The user's own description remains the source of
     * context.
     */
    fun safeStackTrace(throwable: Throwable): String = buildString {
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 5 && seen.add(current)) {
            if (depth > 0) appendLine("Caused by:")
            appendLine(current.javaClass.name)
            current.stackTrace.take(40).forEach { appendLine("  at $it") }
            current = current.cause
            depth++
        }
    }

    /** Recent redacted lines, including the rotated file if present. */
    fun recent(maxChars: Int = RECENT_LIMIT): String {
        val file = logFile ?: return ""
        return synchronized(lock) {
            val previous = File(file.parentFile, ROTATED_NAME)
            buildString {
                if (previous.exists()) {
                    runCatching { append(previous.readText()) }
                }
                runCatching { append(file.readText()) }
            }.takeLast(maxChars)
        }
    }

    fun recentErrors(maxChars: Int = 3_000): String {
        val selected = mutableListOf<String>()
        var includeBlock = false
        recent(RECENT_LIMIT).lineSequence().forEach { line ->
            if (LOG_ENTRY.matches(line)) {
                includeBlock = line.contains(" ERROR ") || line.contains(" CRASH ")
            }
            if (includeBlock) selected += line
        }
        return selected.joinToString("\n").takeLast(maxChars)
    }

    private val LOG_ENTRY = Regex("""^\S+ (?:INFO|ERROR|CRASH) \[""")

    private fun append(level: String, component: String, message: String) {
        val file = logFile ?: return
        val safeMessage = DiagnosticsRedactor.redact(message)
            .lineSequence()
            .joinToString("\n") { it.take(500) }
            .take(4_000)
        val line = "${timestampFormat.format(ZonedDateTime.now())} ${level.uppercase()} " +
            "[${component.take(80)}] $safeMessage\n"
        synchronized(lock) {
            runCatching {
                rotateIfNeeded(file, line.length)
                file.appendText(line)
            }
        }
    }

    private fun rotateIfNeeded(file: File, upcomingBytes: Int) {
        if (!file.exists() || file.length() + upcomingBytes <= MAX_BYTES) return
        val rotated = File(file.parentFile, ROTATED_NAME)
        if (rotated.exists()) rotated.delete()
        file.renameTo(rotated)
    }
}

/** Build metadata read without exposing it through the rest of the UI. */
object BuildInfo {
    val VERSION_NAME: String
        get() = com.fleetpulse.app.BuildConfig.VERSION_NAME

    val VERSION_CODE: Long
        get() = com.fleetpulse.app.BuildConfig.VERSION_CODE.toLong()

    val BUILD_TYPE: String
        get() = com.fleetpulse.app.BuildConfig.BUILD_TYPE
}
