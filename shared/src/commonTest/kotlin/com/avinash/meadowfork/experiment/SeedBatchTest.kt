package com.avinash.meadowfork.experiment

import kotlin.test.Test
import kotlin.test.assertEquals

private val referenceOutcomes by lazy { REPEAT_SEEDS.map { pairedOutcome(it) } }

class SeedBatchTest {

    @Test
    fun referenceSeedsMatch() {
        val summary = BatchSummary.of(referenceOutcomes)
        assertEquals(20, summary.seeds)
        assertEquals(DifferenceSummary(20, 0, 0, 81.5, 59, 118), summary.sheep)
        assertEquals(DifferenceSummary(0, 0, 20, -122.5, -206, -65), summary.grass)
        assertEquals(DifferenceSummary(20, 0, 0, 184.5, 136, 215), summary.starved)
    }

    @Test
    fun summaryIgnoresEvaluationOrder() {
        assertEquals(BatchSummary.of(referenceOutcomes), BatchSummary.of(referenceOutcomes.reversed()))
    }

    @Test
    fun pairedOutcomeIsRepeatable() {
        assertEquals(referenceOutcomes.first(), pairedOutcome(1001))
        assertEquals(PairedOutcome(1001, 63, -110, 151), pairedOutcome(1001))
    }

    @Test
    fun summaryTalliesHigherEqualAndLower() {
        assertEquals(DifferenceSummary(2, 1, 1, 1.5, -2, 5), DifferenceSummary.of(listOf(3, 0, -2, 5)))
        assertEquals(DifferenceSummary(1, 1, 1, 0.0, -1, 4), DifferenceSummary.of(listOf(4, -1, 0)))
    }
}
