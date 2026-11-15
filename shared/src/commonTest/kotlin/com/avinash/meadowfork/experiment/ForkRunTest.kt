package com.avinash.meadowfork.experiment

import kotlin.test.Test
import kotlin.test.assertEquals

class ForkRunTest {

    private val run = forkMeadow(seed = 42, forkDay = 28, endDay = 98)

    private fun counts(branch: Branch, day: Int): List<Int> {
        val world = run.snapshot(branch, day).world
        return listOf(world.sheep.size, world.wolves.size, world.readyGrass)
    }

    @Test
    fun referenceCheckpointsMatch() {
        val expected = mapOf(
            28 to (listOf(61, 33, 188) to listOf(61, 0, 188)),
            42 to (listOf(60, 35, 187) to listOf(112, 0, 133)),
            49 to (listOf(48, 39, 189) to listOf(125, 0, 91)),
            56 to (listOf(53, 38, 199) to listOf(141, 0, 94)),
            98 to (listOf(44, 25, 238) to listOf(131, 0, 92)),
        )
        for ((day, branches) in expected) {
            assertEquals(branches.first, counts(Branch.Baseline, day), "baseline day $day")
            assertEquals(branches.second, counts(Branch.Comparison, day), "removal day $day")
        }
    }

    @Test
    fun day98TotalsReconcile() {
        assertEquals(
            BranchTotals(startSheep = 61, sheepBorn = 169, sheepEaten = 147, sheepStarved = 39, endSheep = 44, endReadyGrass = 238),
            run.totals(Branch.Baseline),
        )
        assertEquals(
            BranchTotals(startSheep = 61, sheepBorn = 320, sheepEaten = 0, sheepStarved = 250, endSheep = 131, endReadyGrass = 92),
            run.totals(Branch.Comparison),
        )
        for (branch in Branch.entries) {
            val totals = run.totals(branch)
            assertEquals(totals.endSheep, totals.startSheep + totals.sheepBorn - totals.sheepEaten - totals.sheepStarved)
        }
    }

    @Test
    fun removalOnlyChangesTheCopyAtTheFork() {
        val baseline = run.snapshot(Branch.Baseline, 28)
        val copy = run.snapshot(Branch.Comparison, 28)
        assertEquals(baseline.world.copy(wolves = emptyList()), copy.world)
        assertEquals(33, copy.events.wolvesRemoved)
        assertEquals(meadowAt(42, 98), run.snapshot(Branch.Baseline, 98))
    }

    @Test
    fun forkWithoutInterventionStaysIdenticalEveryDay() {
        val control = forkMeadow(seed = 42, forkDay = 28, endDay = 98, intervention = Intervention.None)
        assertEquals(71, control.baseline.size)
        for (day in 28..98) {
            assertEquals(control.snapshot(Branch.Baseline, day), control.snapshot(Branch.Comparison, day), "day $day")
        }
    }
}
