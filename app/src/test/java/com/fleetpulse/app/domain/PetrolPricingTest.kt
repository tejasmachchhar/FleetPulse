package com.fleetpulse.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetrolPricingTest {

    @Test
    fun `quantity times unit price gives amount`() {
        assertEquals(500.0, PetrolPricing.amount(10.0, 50.0)!!, 1e-9)
        assertEquals(487.458, PetrolPricing.amount(5.015, 97.2)!!, 1e-9)
    }

    @Test
    fun `amount divided by quantity gives unit price`() {
        assertEquals(50.0, PetrolPricing.unitPrice(500.0, 10.0)!!, 1e-9)
        assertEquals(106.5, PetrolPricing.unitPrice(532.5, 5.0)!!, 1e-9)
    }

    @Test
    fun `amount divided by unit price gives quantity`() {
        assertEquals(10.0, PetrolPricing.quantity(500.0, 50.0)!!, 1e-9)
        assertEquals(4.694835680751174, PetrolPricing.quantity(500.0, 106.5)!!, 1e-9)
    }

    @Test
    fun `empty zero negative and non-finite inputs are rejected`() {
        assertNull(PetrolPricing.amount(null, 50.0))
        assertNull(PetrolPricing.amount(10.0, null))
        assertNull(PetrolPricing.amount(0.0, 50.0))
        assertNull(PetrolPricing.amount(10.0, -1.0))
        assertNull(PetrolPricing.amount(Double.NaN, 50.0))
        assertNull(PetrolPricing.amount(Double.POSITIVE_INFINITY, 50.0))

        assertNull(PetrolPricing.unitPrice(500.0, 0.0))
        assertNull(PetrolPricing.unitPrice(null, 10.0))
        assertNull(PetrolPricing.quantity(500.0, 0.0))
        assertNull(PetrolPricing.quantity(Double.NaN, 50.0))
    }
}
