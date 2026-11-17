package com.fleetpulse.app.domain

/**
 * Petrol quantity, unit-price and refilling-amount relationships.
 *
 * A usage entry only stores quantity and unit price; the amount is always the
 * consistent product of those two values. These helpers are used by the entry
 * form to derive whichever field the user did not type.
 */
object PetrolPricing {

    fun amount(quantity: Double?, unitPrice: Double?): Double? {
        if (quantity == null || unitPrice == null) return null
        if (!quantity.isFinite() || !unitPrice.isFinite()) return null
        if (quantity <= 0.0 || unitPrice <= 0.0) return null
        return quantity * unitPrice
    }

    fun unitPrice(amount: Double?, quantity: Double?): Double? {
        if (amount == null || quantity == null) return null
        if (!amount.isFinite() || !quantity.isFinite()) return null
        if (amount <= 0.0 || quantity <= 0.0) return null
        return amount / quantity
    }

    fun quantity(amount: Double?, unitPrice: Double?): Double? {
        if (amount == null || unitPrice == null) return null
        if (!amount.isFinite() || !unitPrice.isFinite()) return null
        if (amount <= 0.0 || unitPrice <= 0.0) return null
        return amount / unitPrice
    }
}
