package com.fleetpulse.app.support

/**
 * Best-effort redaction for diagnostic text.
 *
 * FleetPulse never writes vehicle records, odometer values, expenses, notes,
 * database rows, location, or file contents to its diagnostics log. This
 * redactor is a second line of defence for exception messages and file paths
 * that may appear in a stack trace.
 */
object DiagnosticsRedactor {

    private data class Rule(val pattern: Regex, val replacement: String)

    private val rules = listOf(
        Rule(
            Regex("""(?i)\b(token|password|passwd|secret|authorization|api[_-]?key)\s*[:=]\s*[^\n]+"""),
            "$1=[redacted]",
        ),
        Rule(
            Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""),
            "[redacted-email]",
        ),
        Rule(
            Regex("""\b[A-Z]{2}[ -]?\d{1,2}[ -]?[A-Z]{0,3}[ -]?\d{4}\b"""),
            "[redacted-registration]",
        ),
        Rule(
            Regex("""(?:content|file)://[^\s:]+"""),
            "[redacted-uri]",
        ),
        Rule(
            Regex("""(?:/data|/storage|/sdcard|/home|/root)/[^\s:]+"""),
            "[redacted-path]",
        ),
        Rule(
            Regex("""[A-Za-z]:\\[^\s:]+"""),
            "[redacted-path]",
        ),
        Rule(
            Regex("""\b\d{6,}\b"""),
            "[redacted-number]",
        ),
    )

    fun redact(text: String): String {
        var safe = text.replace("\u0000", "")
        rules.forEach { rule ->
            safe = rule.pattern.replace(safe, rule.replacement)
        }
        return safe
    }
}
