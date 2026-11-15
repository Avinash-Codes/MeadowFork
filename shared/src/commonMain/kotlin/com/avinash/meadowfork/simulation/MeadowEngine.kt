package com.avinash.meadowfork.simulation

import com.avinash.meadowfork.simulation.MeadowRules.SIZE
import com.avinash.meadowfork.simulation.MeadowRules.TILES

class MeadowEngine(val seed: Long) {

    fun initialWorld(): WorldState {
        val timers = List(TILES) { tile ->
            val readyRoll = KeyedRandom.draw(seed, 0, DrawPurpose.InitialGrassReady, listOf(tile), 100)
            if (readyRoll < MeadowRules.READY_GRASS_PERCENT) {
                0
            } else {
                1 + KeyedRandom.draw(seed, 0, DrawPurpose.InitialGrassTimer, listOf(tile), MeadowRules.REGROWTH_DAYS)
            }
        }
        return WorldState(day = 0, grassTimers = timers, sheep = founders(Species.Sheep), wolves = founders(Species.Wolf))
    }

    fun advance(world: WorldState): Snapshot {
        val day = world.day + 1
        val timers = IntArray(TILES) { maxOf(world.grassTimers[it] - 1, 0) }
        val arrivals = (world.sheep + world.wolves).map { move(it, day) }.groupBy { it.tile }

        val fed = mutableListOf<Animal>()
        var sheepEaten = 0
        for (tile in arrivals.keys.sorted()) {
            val (sheep, wolves) = arrivals.getValue(tile).partition { it.species == Species.Sheep }
            val catches = minOf(sheep.size, wolves.size)
            sheepEaten += catches
            wolves.forEachIndexed { index, wolf -> fed += if (index < catches) wolf.fed() else wolf }
            sheep.drop(catches).forEachIndexed { index, sheepOnTile ->
                if (index == 0 && timers[tile] == 0) {
                    timers[tile] = MeadowRules.REGROWTH_DAYS
                    fed += sheepOnTile.fed()
                } else {
                    fed += sheepOnTile
                }
            }
        }

        val (starved, survivors) = fed.partition { it.energy <= 0 }
        val nextGeneration = survivors.flatMap { breed(it, day) }
        val sheep = nextGeneration.filter { it.species == Species.Sheep }.sortedBy { it.id }
        val wolves = nextGeneration.filter { it.species == Species.Wolf }.sortedBy { it.id }

        val events = DayEvents(
            sheepBorn = sheep.size - survivors.count { it.species == Species.Sheep },
            wolvesBorn = wolves.size - survivors.count { it.species == Species.Wolf },
            sheepEaten = sheepEaten,
            sheepStarved = starved.count { it.species == Species.Sheep },
            wolvesStarved = starved.count { it.species == Species.Wolf },
        )
        return Snapshot(WorldState(day, timers.toList(), sheep, wolves), events)
    }

    fun run(from: Snapshot, toDay: Int): List<Snapshot> =
        generateSequence(from) { if (it.world.day < toDay) advance(it.world) else null }.toList()

    private fun founders(species: Species) = List(species.founders) { index ->
        val id = AnimalId.founder(species, index)
        val tile = KeyedRandom.draw(seed, 0, DrawPurpose.FounderPlacement, id.path, TILES)
        Animal(id, tile, species.startEnergy)
    }

    private fun move(animal: Animal, day: Int): Animal {
        val row = animal.tile / SIZE
        val column = animal.tile % SIZE
        val (nextRow, nextColumn) = when (KeyedRandom.draw(seed, day, DrawPurpose.Movement, animal.id.path, 5)) {
            1 -> row - 1 to column
            2 -> row to column + 1
            3 -> row + 1 to column
            4 -> row to column - 1
            else -> row to column
        }
        val onGrid = nextRow in 0 until SIZE && nextColumn in 0 until SIZE
        val tile = if (onGrid) nextRow * SIZE + nextColumn else animal.tile
        return animal.copy(tile = tile, energy = animal.energy - MeadowRules.DAILY_ENERGY_COST)
    }

    private fun Animal.fed() = copy(energy = minOf(energy + species.feedingGain, MeadowRules.MAX_ENERGY))

    private fun breed(parent: Animal, day: Int): List<Animal> {
        val species = parent.species
        if (parent.energy < species.breedingEnergy) return listOf(parent)
        val roll = KeyedRandom.draw(seed, day, DrawPurpose.Reproduction, parent.id.path, 100)
        if (roll >= species.breedingPercent) return listOf(parent)
        val offspringEnergy = parent.energy / 2
        return listOf(
            parent.copy(energy = parent.energy - offspringEnergy),
            Animal(parent.id.offspring(day), parent.tile, offspringEnergy),
        )
    }
}
