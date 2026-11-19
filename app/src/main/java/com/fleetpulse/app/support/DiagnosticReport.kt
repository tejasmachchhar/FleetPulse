package com.fleetpulse.app.support

import android.content.Context
import android.os.Build
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FeedbackCategory(
    val label: String,
    val issueLabel: String,
)

object FeedbackCategories {
    val bug = listOf(
        FeedbackCategory("App crashed or stopped responding", "crash"),
        FeedbackCategory("Incorrect calculation or mileage", "calculation"),
        FeedbackCategory("Data missing or not saved", "data-integrity"),
        FeedbackCategory("UI or layout problem", "ui-ux"),
        FeedbackCategory("Navigation problem", "navigation"),
        FeedbackCategory("Performance problem", "performance"),
        FeedbackCategory("Other", "bug"),
    )

    val feature = listOf(
        FeedbackCategory("New feature", "enhancement"),
        FeedbackCategory("Existing feature improvement", "enhancement"),
        FeedbackCategory("UI/UX improvement", "ui-ux"),
        FeedbackCategory("Performance improvement", "performance"),
        FeedbackCategory("Calculation improvement", "calculation"),
        FeedbackCategory("Other", "enhancement"),
    )
}

data class DiagnosticSnapshot(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val apiLevel: Int,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val densityDpi: Int,
    val language: String,
    val timestamp: String,
    val currentScreen: String,
    val errorInformation: String,
    val recentDiagnostics: String,
)

object DiagnosticsCollector {

    fun collect(
        context: Context,
        currentScreen: String,
        throwable: Throwable? = null,
    ): DiagnosticSnapshot {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = configuration.locales.takeIf { it.size() > 0 }?.get(0) ?: Locale.getDefault()
        val errorInformation = throwable?.let {
            DiagnosticsRedactor.redact(DiagnosticsLogger.safeStackTrace(it)).take(4_000)
        }.orEmpty().ifBlank {
            DiagnosticsLogger.recentErrors()
        }
        return DiagnosticSnapshot(
            appName = context.getString(com.fleetpulse.app.R.string.app_name),
            versionName = BuildInfo.VERSION_NAME,
            versionCode = BuildInfo.VERSION_CODE,
            buildType = BuildInfo.BUILD_TYPE,
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Unknown" },
            model = Build.MODEL.orEmpty().ifBlank { "Unknown" },
            androidRelease = Build.VERSION.RELEASE.orEmpty().ifBlank { "Unknown" },
            apiLevel = Build.VERSION.SDK_INT,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            densityDpi = resources.displayMetrics.densityDpi,
            language = "${locale.language}_${locale.country}".trimEnd('_') +
                (locale.script.takeIf { it.isNotBlank() }?.let { "-$it" } ?: ""),
            timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
            currentScreen = currentScreen.ifBlank { "Not available" },
            errorInformation = errorInformation,
            recentDiagnostics = DiagnosticsLogger.recent(),
        )
    }
}

data class BugReportDraft(
    val category: FeedbackCategory,
    val description: String,
    val currentScreen: String,
)

data class FeatureRequestDraft(
    val category: FeedbackCategory,
    val suggestion: String,
    val problem: String,
    val expected: String,
    val currentScreen: String,
)

data class GeneralFeedbackDraft(
    val message: String,
    val currentScreen: String,
)

data class EmailBugDraft(
    val category: FeedbackCategory,
    val description: String,
    val steps: String,
    val expected: String,
    val actual: String,
    val currentScreen: String,
)

data class EmailDraft(
    val subject: String,
    val body: String,
)

data class GitHubIssueDraft(
    val title: String,
    val body: String,
    val labels: List<String>,
)

/**
 * Pure report/URL formatting so it can be tested without Android UI.
 *
 * The report intentionally carries no vehicle identifiers, odometer readings,
 * energy/fuel records, expense values, notes, database contents, location, or
 * file contents. The user sees this exact text before any external action.
 */
object FeedbackReportBuilder {

    /**
     * GitHub rejects or fails prefilled issue links once the request URI grows
     * too large. Keep the encoded body well below that limit so a long stack
     * trace cannot produce a broken "Open GitHub issue" action.
     */
    private const val MAX_ENCODED_REPORT_CHARS = 4_800

    private const val REPORT_TRUNCATION_NOTE =
        "\n\n---\nReport shortened locally to keep the prefilled GitHub link within its length limit. " +
            "This preview matches the text that will be shared or copied. No vehicle records, registration " +
            "numbers, odometer readings, expenses, notes, database contents, location, or files were included."

    fun buildBugReport(
        draft: BugReportDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
        includeLogs: Boolean,
    ): String = buildString {
        appendLine("FleetPulse Bug Report")
        appendLine()
        appendLine("Category: ${draft.category.label}")
        appendLine("Description:")
        appendLine(draft.description.ifBlank { "(not provided)" })
        appendLine()
        appendCommonDiagnostics(diagnostics, includeAppInfo, includeEnvironment)
        if (includeEnvironment) {
            appendLine("Current Screen:")
            appendLine(draft.currentScreen.ifBlank { diagnostics.currentScreen })
            appendLine()
        }
        if (includeLogs) {
            appendLine("Error Information:")
            appendLine(diagnostics.errorInformation.ifBlank { "No recent error was recorded." })
            appendLine()
            appendLine("Additional Diagnostics:")
            appendLine(diagnostics.recentDiagnostics.ifBlank { "No recent diagnostics were recorded." })
            appendLine()
        } else {
            appendLine("Error Information and Additional Diagnostics: omitted at the user's request.")
            appendLine()
        }
        appendPrivacyFooter("bug report")
    }

    fun buildFeatureReport(
        draft: FeatureRequestDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
    ): String = buildString {
        appendLine("FleetPulse Feature Request")
        appendLine()
        appendLine("Category: ${draft.category.label}")
        appendLine()
        appendLine("Suggestion:")
        appendLine(draft.suggestion.ifBlank { "(not provided)" })
        appendLine()
        appendLine("Problem it solves:")
        appendLine(draft.problem.ifBlank { "(not provided)" })
        appendLine()
        appendLine("Expected behaviour:")
        appendLine(draft.expected.ifBlank { "(not provided)" })
        appendLine()
        appendCommonDiagnostics(diagnostics, includeAppInfo, includeEnvironment)
        appendPrivacyFooter("feature request")
    }

    fun buildGeneralFeedback(
        draft: GeneralFeedbackDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
    ): String = buildString {
        appendLine("FleetPulse General Feedback")
        appendLine()
        appendLine("Message:")
        appendLine(draft.message.ifBlank { "(not provided)" })
        appendLine()
        if (includeEnvironment) {
            appendLine("Context:")
            appendLine("Screen: ${draft.currentScreen.ifBlank { diagnostics.currentScreen }}")
            appendLine()
        }
        appendCommonDiagnostics(diagnostics, includeAppInfo, includeEnvironment)
        appendPrivacyFooter("feedback")
    }

    fun buildBugEmail(
        draft: EmailBugDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
        includeLogs: Boolean,
    ): EmailDraft {
        val subject = "[FleetPulse Bug Report] - " +
            (firstLine(draft.description) ?: draft.category.label)
        val body = buildString {
            appendLine("Hello FleetPulse Developer,")
            appendLine()
            appendLine("I would like to report a problem in FleetPulse.")
            appendLine()
            appendLine("Issue category:")
            appendLine(draft.category.label)
            appendLine()
            appendLine("Description:")
            appendLine(draft.description.ifBlank { "(not provided)" })
            appendLine()
            appendOptionalField("Steps to reproduce", draft.steps)
            appendOptionalField("Expected behaviour", draft.expected)
            appendOptionalField("Actual behaviour", draft.actual)
            appendEmailDiagnostics(
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
                currentScreen = draft.currentScreen,
            )
            if (includeLogs) {
                appendLine("Error Information:")
                appendLine(diagnostics.errorInformation.ifBlank { "No recent error was recorded." })
                appendLine()
                appendLine("Additional Diagnostics:")
                appendLine(diagnostics.recentDiagnostics.ifBlank { "No recent diagnostics were recorded." })
                appendLine()
            } else {
                appendLine("Error Information and Additional Diagnostics: omitted at the user's request.")
                appendLine()
            }
            appendPrivacyFooter("email bug report")
            appendLine()
            appendLine("Thank you.")
        }
        return EmailDraft(subject = subject.take(120), body = body)
    }

    fun buildFeatureEmail(
        draft: FeatureRequestDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
    ): EmailDraft {
        val subject = "[FleetPulse Feature Request] - " +
            (firstLine(draft.suggestion) ?: draft.category.label)
        val body = buildString {
            appendLine("Hello FleetPulse Developer,")
            appendLine()
            appendLine("I would like to suggest an improvement for FleetPulse.")
            appendLine()
            appendLine("Category:")
            appendLine(draft.category.label)
            appendLine()
            appendLine("My suggestion:")
            appendLine(draft.suggestion.ifBlank { "(not provided)" })
            appendLine()
            appendOptionalField("Problem it solves", draft.problem)
            appendOptionalField("Expected benefit or behaviour", draft.expected)
            appendEmailDiagnostics(
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
                currentScreen = draft.currentScreen,
            )
            appendPrivacyFooter("email feature request")
            appendLine()
            appendLine("Thank you.")
        }
        return EmailDraft(subject = subject.take(120), body = body)
    }

    fun buildGeneralEmail(
        draft: GeneralFeedbackDraft,
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
    ): EmailDraft {
        val body = buildString {
            appendLine("Hello FleetPulse Developer,")
            appendLine()
            appendLine("Message:")
            appendLine(draft.message.ifBlank { "(not provided)" })
            appendLine()
            appendEmailDiagnostics(
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
                currentScreen = draft.currentScreen,
            )
            appendPrivacyFooter("email feedback")
            appendLine()
            appendLine("Thank you.")
        }
        return EmailDraft(subject = "[FleetPulse Feedback]", body = body)
    }

    fun bugIssue(draft: BugReportDraft, report: String): GitHubIssueDraft {
        val summary = firstLine(draft.description)
            ?: draft.category.label
        return GitHubIssueDraft(
            title = "[Bug] ${summary.take(90)}",
            body = report,
            labels = listOf("bug", draft.category.issueLabel).distinct(),
        )
    }

    fun featureIssue(draft: FeatureRequestDraft, report: String): GitHubIssueDraft {
        val summary = firstLine(draft.suggestion) ?: draft.category.label
        return GitHubIssueDraft(
            title = "[Feature] ${summary.take(90)}",
            body = report,
            labels = listOf("feature-request", "enhancement", draft.category.issueLabel).distinct(),
        )
    }

    fun generalIssue(draft: GeneralFeedbackDraft, report: String): GitHubIssueDraft {
        val summary = firstLine(draft.message) ?: "General feedback"
        return GitHubIssueDraft(
            title = "[Feedback] ${summary.take(90)}",
            body = report,
            labels = listOf("feedback"),
        )
    }

    fun issueUrl(issue: GitHubIssueDraft): String {
        return buildString {
            append(SupportConfig.NEW_ISSUE_URL)
            append("?title=").append(encode(issue.title))
            append("&body=").append(encode(issue.body))
            if (issue.labels.isNotEmpty()) {
                append("&labels=").append(encode(issue.labels.joinToString(",")))
            }
        }
    }

    /**
     * Returns the exact report text to preview and share. Long reports are cut
     * to a safe encoded size with an explicit note; the same text is used for
     * GitHub, the share sheet, and the clipboard.
     */
    fun fitReportForIssue(report: String): String {
        if (encodedLength(report) <= MAX_ENCODED_REPORT_CHARS) return report

        var low = 0
        var high = report.length
        while (low < high) {
            val middle = (low + high + 1) / 2
            val candidate = buildTruncatedReport(report, middle)
            if (encodedLength(candidate) <= MAX_ENCODED_REPORT_CHARS) {
                low = middle
            } else {
                high = middle - 1
            }
        }
        return buildTruncatedReport(report, low)
    }

    private fun buildTruncatedReport(report: String, prefixLength: Int): String {
        val prefix = report.take(prefixLength).trimEnd()
        return if (prefix.isEmpty()) REPORT_TRUNCATION_NOTE.trimStart() else prefix + REPORT_TRUNCATION_NOTE
    }

    private fun encodedLength(value: String): Int = encode(value).length

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun StringBuilder.appendOptionalField(label: String, value: String) {
        if (value.isNotBlank()) {
            appendLine("$label:")
            appendLine(value)
            appendLine()
        }
    }

    private fun StringBuilder.appendEmailDiagnostics(
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
        currentScreen: String,
    ) {
        if (includeAppInfo) {
            appendLine("Application Version:")
            appendLine(
                "${diagnostics.versionName} (versionCode ${diagnostics.versionCode}, " +
                    "${diagnostics.buildType} build)",
            )
            appendLine()
        } else {
            appendLine("Application Version: omitted at the user's request.")
            appendLine()
        }

        if (includeEnvironment) {
            appendLine("Android Version:")
            appendLine("${diagnostics.androidRelease} (API ${diagnostics.apiLevel})")
            appendLine()
            appendLine("Device:")
            appendLine("${diagnostics.manufacturer} ${diagnostics.model}".trim())
            appendLine()
            appendLine("Screen:")
            appendLine(currentScreen.ifBlank { diagnostics.currentScreen })
            appendLine()
            appendLine("Environment:")
            appendLine(
                "${diagnostics.screenWidthDp} × ${diagnostics.screenHeightDp} dp @ " +
                    "${diagnostics.densityDpi} dpi · ${diagnostics.language}",
            )
            appendLine(diagnostics.timestamp)
            appendLine()
        } else {
            appendLine("Device and Android version: omitted at the user's request.")
            appendLine()
        }
    }

    private fun StringBuilder.appendCommonDiagnostics(
        diagnostics: DiagnosticSnapshot,
        includeAppInfo: Boolean,
        includeEnvironment: Boolean,
    ) {
        if (includeAppInfo) {
            appendLine("Application:")
            appendLine("- Name: ${diagnostics.appName}")
            appendLine("- Version: ${diagnostics.versionName}")
            appendLine("- Version Code: ${diagnostics.versionCode}")
            appendLine("- Build: ${diagnostics.buildType}")
            appendLine()
        } else {
            appendLine("Application details: omitted at the user's request.")
            appendLine()
        }

        if (includeEnvironment) {
            appendLine("Device:")
            appendLine("- Manufacturer: ${diagnostics.manufacturer}")
            appendLine("- Model: ${diagnostics.model}")
            appendLine("- Android Version: ${diagnostics.androidRelease} (API ${diagnostics.apiLevel})")
            appendLine()
            appendLine("Environment:")
            appendLine("- Screen: ${diagnostics.currentScreen}")
            appendLine("- Display: ${diagnostics.screenWidthDp} × ${diagnostics.screenHeightDp} dp @ ${diagnostics.densityDpi} dpi")
            appendLine("- Language: ${diagnostics.language}")
            appendLine("- Timestamp: ${diagnostics.timestamp}")
            appendLine()
        } else {
            appendLine("Device and environment details: omitted at the user's request.")
            appendLine()
        }
    }

    private fun StringBuilder.appendPrivacyFooter(kind: String) {
        appendLine("---")
        appendLine(
            "Generated locally by FleetPulse. This $kind was prepared on the device and shared only " +
                "because the user chose to share it. No vehicle records, registration numbers, odometer " +
                "readings, expenses, notes, database contents, location, or files were included.",
        )
    }

    private fun firstLine(text: String): String? =
        text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }?.take(90)
}
