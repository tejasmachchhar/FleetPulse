package com.fleetpulse.app.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Locale-independent conversion between user-editable numeric text and values.
 *
 * Editable fields must never be prefilled with grouped display strings such as
 * "31,713.0": the comma is not accepted by the numeric keyboard filter and
 * breaks Backspace/cursor editing. Display-only screens keep using formatNum.
 */
object NumberText {

    /** Plain, comma-free text suitable for an editable decimal field. */
    fun plain(value: Double?, maxDecimals: Int = 6): String {
        if (value == null || !value.isFinite()) return ""
        val rounded = BigDecimal.valueOf(value)
            .setScale(maxDecimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return rounded.toPlainString()
    }

    /** Accepts pasted values with digit grouping or spaces. */
    fun parse(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** Fixed-decimal text for automatically calculated fields. */
    fun fixed(value: Double, decimals: Int): String {
        if (!value.isFinite()) return ""
        return BigDecimal.valueOf(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toPlainString()
    }
}
