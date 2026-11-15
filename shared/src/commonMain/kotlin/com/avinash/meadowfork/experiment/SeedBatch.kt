package com.avinash.meadowfork.experiment

val REPEAT_SEEDS: LongRange = 1001L..1020L

data class PairedOutcome(
    val seed: Long,
    val sheepDifference: Int,
    val grassDifference: Int,
    val starvedDifference: Int,
)

data class DifferenceSummary(
    val higher: Int,
    val equal: Int,
    val lower: Int,
    val median: Double,
    val min: Int,
    val max: Int,
) {
    companion object {
        fun of(differences: List<Int>): DifferenceSummary {
            require(differences.isNotEmpty())
            val sorted = differences.sorted()
            val middle = sorted.size / 2
            val median = if (sorted.size % 2 == 1) {
                sorted[middle].toDouble()
            } else {
                (sorted[middle - 1] + sorted[middle]) / 2.0
            }
            return DifferenceSummary(
                higher = sorted.count { it > 0 },
                equal = sorted.count { it == 0 },
                lower = sorted.count { it < 0 },
                median = median,
                min = sorted.first(),
                max = sorted.last(),
            )
        }
    }
}

data class BatchSummary(
    val seeds: Int,
    val sheep: DifferenceSummary,
    val grass: DifferenceSummary,
    val starved: DifferenceSummary,
) {
    companion object {
        fun of(outcomes: List<PairedOutcome>) = BatchSummary(
            seeds = outcomes.size,
            sheep = DifferenceSummary.of(outcomes.map { it.sheepDifference }),
            grass = DifferenceSummary.of(outcomes.map { it.grassDifference }),
            starved = DifferenceSummary.of(outcomes.map { it.starvedDifference }),
        )
    }
}

fun pairedOutcome(seed: Long, forkDay: Int = 28, endDay: Int = forkDay + 70): PairedOutcome {
    val run = forkMeadow(seed, forkDay, endDay)
    val baseline = run.totals(Branch.Baseline)
    val removal = run.totals(Branch.Comparison)
    return PairedOutcome(
        seed = seed,
        sheepDifference = removal.endSheep - baseline.endSheep,
        grassDifference = removal.endReadyGrass - baseline.endReadyGrass,
        starvedDifference = removal.sheepStarved - baseline.sheepStarved,
    )
}
