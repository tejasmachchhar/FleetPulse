package com.fleetpulse.app.support

import java.net.URLEncoder

/**
 * Single source of truth for every external destination shown by the
 * Feedback & Support Centre.
 *
 * Only verified values belong here. Optional destinations stay null and the UI
 * hides them instead of showing a dead button. No secrets, access tokens, or
 * payment credentials are stored in the APK.
 */
object SupportConfig {

    const val REPOSITORY_URL = "https://github.com/tejasmachchhar/FleetPulse"
    const val RELEASES_URL = "$REPOSITORY_URL/releases"
    const val ISSUES_URL = "$REPOSITORY_URL/issues"
    const val NEW_ISSUE_URL = "$REPOSITORY_URL/issues/new"
    const val DOCUMENTATION_URL = "$REPOSITORY_URL/blob/main/docs/USER_GUIDE.md"
    const val PRIVACY_URL = "$REPOSITORY_URL/blob/main/PRIVACY.md"
    const val SUPPORT_URL = "$REPOSITORY_URL/blob/main/SUPPORT.md"
    const val SECURITY_URL = "$REPOSITORY_URL/security/policy"
    const val CONTRIBUTING_URL = "$REPOSITORY_URL/blob/main/CONTRIBUTING.md"

    /**
     * Manual GitHub issue forms. The in-app composer deliberately uses the
     * direct issue URL with a prefilled body because GitHub issue *forms* do not
     * accept body prefill parameters. The forms remain useful for reports typed
     * directly on GitHub.
     */
    const val BUG_TEMPLATE = "bug_report.yml"
    const val FEATURE_TEMPLATE = "feature_request.yml"
    const val FEEDBACK_TEMPLATE = "general_feedback.yml"

    const val UPI_ID = "tejasmachchhar1@axl"
    const val UPI_PAYEE_NAME = "FleetPulse Developer"
    const val UPI_NOTE = "Support FleetPulse"

    /**
     * Verified 2026-11-19: https://github.com/sponsors/tejasmachchhar redirects
     * to the normal profile and GitHub reports hasSponsorsListing=false.
     * To enable it, create a GitHub Sponsors profile, wait for approval, then
     * set this value to the public sponsor URL and add it to the docs.
     */
    val GITHUB_SPONSORS_URL: String? = null

    /** Verified 2026-11-19: returns the maintainer's public Buy Me a Coffee page. */
    val BUY_ME_A_COFFEE_URL: String? = "https://buymeacoffee.com/tejasmachchhar"

    /**
     * No project support mailbox is configured. Do not invent one. When a
     * monitored mailbox exists, add it here and the app will expose an email
     * action after the same report-preview consent step.
     */
    val SUPPORT_EMAIL: String? = null

    const val CONFIG_VERIFIED_ON = "2026-11-19"

    val UPI_PAYMENT_URI: String = buildUpiUri()

    fun buildUpiUri(
        upiId: String = UPI_ID,
        payeeName: String = UPI_PAYEE_NAME,
        note: String = UPI_NOTE,
    ): String {
        fun enc(value: String): String =
            URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
        return "upi://pay?pa=${enc(upiId)}&pn=${enc(payeeName)}&tn=${enc(note)}"
    }
}
