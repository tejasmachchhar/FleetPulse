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
    const val CHANGELOG_URL = "$REPOSITORY_URL/blob/main/CHANGELOG.md"
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
     * Public support mailbox verified for FleetPulse feedback. The app only
     * opens the user's email app with a prefilled draft; it never sends mail
     * silently and cannot confirm delivery.
     */
    const val SUPPORT_EMAIL = "tejasmachchhar2019@gmail.com"

    const val CONFIG_VERIFIED_ON = "2026-11-19"

    val UPI_PAYMENT_URI: String = buildUpiUri()

    fun buildUpiUri(
        upiId: String = UPI_ID,
        payeeName: String = UPI_PAYEE_NAME,
        note: String = UPI_NOTE,
        currency: String = "INR",
    ): String {
        // Build the query with the same escaping guarantees as Uri.Builder
        // without depending on android.net.Uri in JVM tests. The amount is
        // intentionally omitted so the payer chooses it inside their UPI app.
        fun enc(value: String): String =
            URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
        return "upi://pay?pa=${enc(upiId)}&pn=${enc(payeeName)}&tn=${enc(note)}&cu=${enc(currency)}"
    }
}
