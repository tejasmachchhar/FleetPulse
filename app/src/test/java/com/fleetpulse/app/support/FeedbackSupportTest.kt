package com.fleetpulse.app.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackSupportTest {

    private val diagnostics = DiagnosticSnapshot(
        appName = "FleetPulse",
        versionName = "4.3.0",
        versionCode = 4,
        buildType = "debug",
        manufacturer = "Google",
        model = "Pixel 8",
        androidRelease = "15",
        apiLevel = 35,
        screenWidthDp = 411,
        screenHeightDp = 891,
        densityDpi = 420,
        language = "en_IN",
        timestamp = "2026-11-17T20:00:00+05:30",
        currentScreen = "Dashboard",
        errorInformation = "java.lang.IllegalStateException: synthetic failure",
        recentDiagnostics = "2026-11-17T20:00:00+05:30 ERROR [Test] synthetic failure",
    )

    @Test
    fun `central config uses verified destinations and leaves sponsors disabled`() {
        assertEquals("https://github.com/tejasmachchhar/FleetPulse", SupportConfig.REPOSITORY_URL)
        assertEquals("https://buymeacoffee.com/tejasmachchhar", SupportConfig.BUY_ME_A_COFFEE_URL)
        assertNull(SupportConfig.GITHUB_SPONSORS_URL)
        assertEquals("tejasmachchhar2019@gmail.com", SupportConfig.SUPPORT_EMAIL)
        assertEquals("tejasmachchhar1@axl", SupportConfig.UPI_ID)
    }

    @Test
    fun `upi uri has the expected payment fields and no amount`() {
        assertEquals(
            "upi://pay?pa=tejasmachchhar1%40axl&pn=FleetPulse%20Developer&tn=Support%20FleetPulse&cu=INR",
            SupportConfig.UPI_PAYMENT_URI,
        )
        assertFalse(SupportConfig.UPI_PAYMENT_URI.contains("&am="))
        assertTrue(SupportConfig.UPI_PAYMENT_URI.contains("&cu=INR"))
    }

    @Test
    fun `bug report includes selected category and safe diagnostics`() {
        val draft = BugReportDraft(
            category = FeedbackCategories.bug.first(),
            description = "The dashboard card went blank.",
            currentScreen = "Dashboard",
        )
        val report = FeedbackReportBuilder.buildBugReport(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = true,
            includeLogs = true,
        )

        assertTrue(report.contains("FleetPulse Bug Report"))
        assertTrue(report.contains("Category: App crashed or stopped responding"))
        assertTrue(report.contains("Version: 4.3.0"))
        assertTrue(report.contains("Model: Pixel 8"))
        assertTrue(report.contains("Current Screen:"))
        assertTrue(report.contains("Dashboard"))
        assertTrue(report.contains("synthetic failure"))
        assertTrue(report.contains("No vehicle records"))
        assertFalse(report.contains("DEMO-0001"))
        assertFalse(report.contains("odometer reading is"))
    }

    @Test
    fun `bug report honours opt out without inventing missing values`() {
        val draft = BugReportDraft(
            category = FeedbackCategories.bug.last(),
            description = "",
            currentScreen = "Settings",
        )
        val report = FeedbackReportBuilder.buildBugReport(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = false,
            includeEnvironment = false,
            includeLogs = false,
        )

        assertTrue(report.contains("Application details: omitted"))
        assertTrue(report.contains("Device and environment details: omitted"))
        assertTrue(report.contains("omitted at the user's request"))
        assertFalse(report.contains("Pixel 8"))
        assertFalse(report.contains("4.3.0"))
        assertFalse(report.contains("synthetic failure"))
    }

    @Test
    fun `feature request carries only the minimal optional fields`() {
        val draft = FeatureRequestDraft(
            category = FeedbackCategories.feature.first(),
            suggestion = "Add a monthly charging estimate",
            problem = "I have to add it up myself",
            expected = "",
            currentScreen = "Analysis · costs",
        )
        val report = FeedbackReportBuilder.buildFeatureReport(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = false,
        )
        assertTrue(report.contains("FleetPulse Feature Request"))
        assertTrue(report.contains("Suggestion:"))
        assertTrue(report.contains("Add a monthly charging estimate"))
        assertTrue(report.contains("Device and environment details: omitted"))
        assertFalse(report.contains("Pixel 8"))
    }

    @Test
    fun `bug email prefills subject recipient fields and agreed diagnostics`() {
        val draft = EmailBugDraft(
            category = FeedbackCategories.bug[1],
            description = "Mileage changed after edit",
            steps = "1. Edit a charge\n2. Open Analysis",
            expected = "The mileage stays the same",
            actual = "It shows a dash",
            currentScreen = "Analysis · mileage",
        )
        val email = FeedbackReportBuilder.buildBugEmail(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = true,
            includeLogs = true,
        )

        assertEquals("[FleetPulse Bug Report] - Mileage changed after edit", email.subject)
        assertTrue(email.body.startsWith("Hello FleetPulse Developer,"))
        assertTrue(email.body.contains("Issue category:"))
        assertTrue(email.body.contains("Incorrect calculation or mileage"))
        assertTrue(email.body.contains("Steps to reproduce:"))
        assertTrue(email.body.contains("Application Version:"))
        assertTrue(email.body.contains("4.3.0"))
        assertTrue(email.body.contains("Android Version:"))
        assertTrue(email.body.contains("Device:"))
        assertTrue(email.body.contains("Pixel 8"))
        assertTrue(email.body.contains("synthetic failure"))
        assertTrue(email.body.contains("No vehicle records"))
        assertFalse(email.body.contains("DEMO-0001"))
    }

    @Test
    fun `email report honours opt out`() {
        val draft = EmailBugDraft(
            category = FeedbackCategories.bug.last(),
            description = "",
            steps = "",
            expected = "",
            actual = "",
            currentScreen = "Settings",
        )
        val email = FeedbackReportBuilder.buildBugEmail(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = false,
            includeEnvironment = false,
            includeLogs = false,
        )

        assertTrue(email.body.contains("Application Version: omitted"))
        assertTrue(email.body.contains("Device and Android version: omitted"))
        assertTrue(email.body.contains("omitted at the user's request"))
        assertFalse(email.body.contains("Pixel 8"))
        assertFalse(email.body.contains("4.3.0"))
        assertFalse(email.body.contains("synthetic failure"))
    }

    @Test
    fun `feature and general emails use the requested subjects`() {
        val feature = FeedbackReportBuilder.buildFeatureEmail(
            draft = FeatureRequestDraft(
                category = FeedbackCategories.feature.first(),
                suggestion = "Add a monthly charging estimate",
                problem = "",
                expected = "One monthly total instead of manual addition",
                currentScreen = "Analysis",
            ),
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = false,
        )
        assertEquals(
            "[FleetPulse Feature Request] - Add a monthly charging estimate",
            feature.subject,
        )
        assertTrue(feature.body.contains("My suggestion:"))
        assertTrue(feature.body.contains("Expected benefit"))

        val general = FeedbackReportBuilder.buildGeneralEmail(
            draft = GeneralFeedbackDraft(message = "The new support flow is clear.", currentScreen = "About"),
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = false,
        )
        assertEquals("[FleetPulse Feedback]", general.subject)
        assertTrue(general.body.contains("The new support flow is clear."))
    }

    @Test
    fun `github issue url prefills title body and labels`() {
        val draft = BugReportDraft(
            category = FeedbackCategories.bug[1],
            description = "Mileage changed after edit",
            currentScreen = "Usage log",
        )
        val report = FeedbackReportBuilder.buildBugReport(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = true,
            includeEnvironment = true,
            includeLogs = false,
        )
        val issue = FeedbackReportBuilder.bugIssue(draft, report)
        val url = FeedbackReportBuilder.issueUrl(issue)

        assertTrue(url.startsWith("https://github.com/tejasmachchhar/FleetPulse/issues/new?"))
        assertTrue(url.contains("title=%5BBug%5D%20Mileage%20changed%20after%20edit"))
        assertTrue(url.contains("body=FleetPulse%20Bug%20Report"))
        assertTrue(url.contains("labels=bug%2Ccalculation"))
        assertEquals(listOf("bug", "calculation"), issue.labels)
    }

    @Test
    fun `long report is shortened to keep the github link usable`() {
        val hugeDiagnostics = diagnostics.copy(
            errorInformation = "E".repeat(20_000),
            recentDiagnostics = "D".repeat(20_000),
        )
        val draft = BugReportDraft(
            category = FeedbackCategories.bug[1],
            description = "Mileage is wrong",
            currentScreen = "Analysis",
        )
        val fullReport = FeedbackReportBuilder.buildBugReport(
            draft = draft,
            diagnostics = hugeDiagnostics,
            includeAppInfo = true,
            includeEnvironment = true,
            includeLogs = true,
        )
        val fitted = FeedbackReportBuilder.fitReportForIssue(fullReport)
        val issue = FeedbackReportBuilder.bugIssue(draft, fitted)
        val url = FeedbackReportBuilder.issueUrl(issue)

        assertTrue(fitted.length < fullReport.length)
        assertTrue(fitted.contains("Report shortened locally"))
        assertTrue(url.length < 6_000)
        assertTrue(url.startsWith("https://github.com/tejasmachchhar/FleetPulse/issues/new?"))
        val shortReport = fullReport.take(500)
        assertEquals(shortReport, FeedbackReportBuilder.fitReportForIssue(shortReport))
    }

    @Test
    fun `safe stack trace keeps the type and frames but drops the message`() {
        val throwable = IllegalStateException("Vehicle Demo EV with MH12AB1234 failed at 123456")
        val trace = DiagnosticsLogger.safeStackTrace(throwable)

        assertTrue(trace.contains("java.lang.IllegalStateException"))
        assertTrue(trace.contains("at "))
        assertFalse(trace.contains("Demo EV"))
        assertFalse(trace.contains("MH12AB1234"))
        assertFalse(trace.contains("123456"))
    }

    @Test
    fun `redactor removes common sensitive patterns`() {
        val text = "mail me at user@example.com from /data/user/0/com.fleetpulse.app " +
            "for MH12AB1234 and 123456789 token=abc123\n" +
            "content://com.example.provider/backup/user-notes.json\n" +
            "file:///storage/emulated/0/Download/fleetpulse-backup.json"
        val redacted = DiagnosticsRedactor.redact(text)
        assertFalse(redacted.contains("user@example.com"))
        assertFalse(redacted.contains("/data/user/0"))
        assertFalse(redacted.contains("MH12AB1234"))
        assertFalse(redacted.contains("123456789"))
        assertFalse(redacted.contains("abc123"))
        assertFalse(redacted.contains("user-notes.json"))
        assertFalse(redacted.contains("fleetpulse-backup.json"))
        assertTrue(redacted.contains("[redacted-email]"))
        assertTrue(redacted.contains("[redacted-registration]"))
        assertTrue(redacted.contains("[redacted-number]"))
        assertTrue(redacted.contains("[redacted-path]") || redacted.contains("[redacted-uri]"))
        assertTrue(redacted.contains("[redacted-uri]"))
    }
}
