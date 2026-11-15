package com.avinash.meadowfork.experiment

import com.avinash.meadowfork.simulation.MeadowEngine
import com.avinash.meadowfork.simulation.Snapshot

enum class Intervention { None, RemoveWolves }

enum class Branch { Baseline, Comparison }

data class BranchTotals(
    val startSheep: Int,
    val sheepBorn: Int,
    val sheepEaten: Int,
    val sheepStarved: Int,
    val endSheep: Int,
    val endReadyGrass: Int,
)

class ForkRun(val seed: Long, val baseline: List<Snapshot>, val comparison: List<Snapshot>) {
    val forkDay: Int get() = baseline.first().world.day
    val endDay: Int get() = baseline.last().world.day

    fun history(branch: Branch) = when (branch) {
        Branch.Baseline -> baseline
        Branch.Comparison -> comparison
    }

    fun snapshot(branch: Branch, day: Int): Snapshot = history(branch)[day - forkDay]

    fun totals(branch: Branch): BranchTotals {
        val history = history(branch)
        val afterFork = history.drop(1).map { it.events }
        return BranchTotals(
            startSheep = history.first().world.sheep.size,
            sheepBorn = afterFork.sumOf { it.sheepBorn },
            sheepEaten = afterFork.sumOf { it.sheepEaten },
            sheepStarved = afterFork.sumOf { it.sheepStarved },
            endSheep = history.last().world.sheep.size,
            endReadyGrass = history.last().world.readyGrass,
        )
    }
}

fun meadowAt(seed: Long, day: Int): Snapshot {
    val engine = MeadowEngine(seed)
    return engine.run(Snapshot(engine.initialWorld()), day).last()
}

fun forkMeadow(
    seed: Long = 42,
    forkDay: Int = 28,
    endDay: Int = forkDay + 70,
    intervention: Intervention = Intervention.RemoveWolves,
): ForkRun {
    val engine = MeadowEngine(seed)
    val atFork = meadowAt(seed, forkDay)
    val copy = when (intervention) {
        Intervention.None -> atFork
        Intervention.RemoveWolves -> Snapshot(
            world = atFork.world.copy(wolves = emptyList()),
            events = atFork.events.copy(wolvesRemoved = atFork.world.wolves.size),
        )
    }
    return ForkRun(seed, engine.run(atFork, endDay), engine.run(copy, endDay))
}
