package com.fleetpulse.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the exact suite shown in the app's Calculation Verification screen.
 * A failure here means the UI would also report a mismatch.
 */
class VerificationSuiteTest {

    @Test
    fun everyReferenceCasePasses() {
        val results = VerificationSuite.run()
        val failures = results.filter { !it.pass }
        assertTrue(
            buildString {
                append("${failures.size} of ${results.size} verification cases failed:\n")
                failures.forEach {
                    append("• ${it.name}: expected ${it.expected}, actual ${it.actual}\n")
                }
            },
            failures.isEmpty(),
        )
        assertTrue("Expected the full suite, found ${results.size} cases", results.size >= 25)
    }
}
