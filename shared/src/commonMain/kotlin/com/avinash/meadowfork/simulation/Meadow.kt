package com.avinash.meadowfork.simulation

object MeadowRules {
    const val SIZE = 20
    const val TILES = SIZE * SIZE
    const val READY_GRASS_PERCENT = 65
    const val REGROWTH_DAYS = 16
    const val DAILY_ENERGY_COST = 1
    const val MAX_ENERGY = 40
}

enum class Species(
    val code: Int,
    val founders: Int,
    val startEnergy: Int,
    val feedingGain: Int,
    val breedingEnergy: Int,
    val breedingPercent: Int,
) {
    Sheep(code = 0, founders = 80, startEnergy = 8, feedingGain = 7, breedingEnergy = 12, breedingPercent = 9),
    Wolf(code = 1, founders = 20, startEnergy = 14, feedingGain = 20, breedingEnergy = 18, breedingPercent = 4),
}

data class AnimalId(val path: List<Int>) : Comparable<AnimalId> {
    val species: Species get() = Species.entries.first { it.code == path.first() }

    fun offspring(birthDay: Int) = AnimalId(path + birthDay)

    override fun compareTo(other: AnimalId): Int {
        for (i in 0 until minOf(path.size, other.path.size)) {
            val order = path[i].compareTo(other.path[i])
            if (order != 0) return order
        }
        return path.size.compareTo(other.path.size)
    }

    override fun toString() = path.joinToString(".")

    companion object {
        fun founder(species: Species, index: Int) = AnimalId(listOf(species.code, index))
    }
}

data class Animal(val id: AnimalId, val tile: Int, val energy: Int) {
    val species: Species get() = id.species
}

data class WorldState(
    val day: Int,
    val grassTimers: List<Int>,
    val sheep: List<Animal>,
    val wolves: List<Animal>,
) {
    val readyGrass: Int get() = grassTimers.count { it == 0 }
}

data class DayEvents(
    val sheepBorn: Int = 0,
    val wolvesBorn: Int = 0,
    val sheepEaten: Int = 0,
    val sheepStarved: Int = 0,
    val wolvesStarved: Int = 0,
    val wolvesRemoved: Int = 0,
)

data class Snapshot(val world: WorldState, val events: DayEvents = DayEvents())
