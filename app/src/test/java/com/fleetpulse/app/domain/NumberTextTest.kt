package com.fleetpulse.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberTextTest {

    @Test
    fun `editable text never contains digit grouping`() {
        assertEquals("31713", NumberText.plain(31713.0))
        assertEquals("31713.5", NumberText.plain(31713.5))
        assertEquals("5.43", NumberText.plain(5.43))
        assertEquals("0", NumberText.plain(0.0))
        assertEquals("", NumberText.plain(Double.NaN))
    }

    @Test
    fun `parse accepts plain and pasted grouped text`() {
        assertEquals(31713.0, NumberText.parse("31,713")!!, 0.0)
        assertEquals(31713.0, NumberText.parse(" 31 713 ")!!, 0.0)
        assertEquals(31.5, NumberText.parse("31.5")!!, 0.0)
        assertNull(NumberText.parse(""))
        assertNull(NumberText.parse("abc"))
        assertNull(NumberText.parse("NaN"))
    }

    @Test
    fun `fixed text uses the requested precision`() {
        assertEquals("500.02", NumberText.fixed(500.0175, 2))
        assertEquals("4.695", NumberText.fixed(4.694835680751174, 3))
        assertEquals("106.50", NumberText.fixed(106.5, 2))
    }
}
