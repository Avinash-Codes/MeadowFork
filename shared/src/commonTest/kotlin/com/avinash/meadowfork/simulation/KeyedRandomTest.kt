package com.avinash.meadowfork.simulation

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyedRandomTest {

    @Test
    fun publishedVectorsMatch() {
        assertEquals(16, KeyedRandom.draw(42, 0, DrawPurpose.InitialGrassReady, listOf(0), 100))
        assertEquals(141, KeyedRandom.draw(42, 0, DrawPurpose.FounderPlacement, listOf(0, 0), 400))
        assertEquals(1, KeyedRandom.draw(42, 29, DrawPurpose.Movement, listOf(0, 0), 5))
        assertEquals(63, KeyedRandom.draw(42, 29, DrawPurpose.Reproduction, listOf(0, 0, 14), 100))
    }

    @Test
    fun drawingOtherEventsFirstHasNoEffect() {
        val alone = KeyedRandom.draw(42, 29, DrawPurpose.Movement, listOf(0, 0), 5)
        repeat(50) { KeyedRandom.draw(42, 29, DrawPurpose.Movement, listOf(1, it), 5) }
        assertEquals(alone, KeyedRandom.draw(42, 29, DrawPurpose.Movement, listOf(0, 0), 5))
    }
}
