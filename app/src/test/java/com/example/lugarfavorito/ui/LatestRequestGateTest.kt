package com.example.lugarfavorito.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestRequestGateTest {
    @Test
    fun `starting a new request invalidates the previous one`() {
        val gate = LatestRequestGate()
        val first = gate.next()
        val second = gate.next()

        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }

    @Test
    fun `invalidate rejects the active request`() {
        val gate = LatestRequestGate()
        val active = gate.next()

        gate.invalidate()

        assertFalse(gate.isCurrent(active))
    }
}
