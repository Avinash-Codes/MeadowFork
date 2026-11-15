package com.avinash.meadowfork.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avinash.meadowfork.experiment.Branch
import com.avinash.meadowfork.experiment.ForkRun
import com.avinash.meadowfork.experiment.forkMeadow
import com.avinash.meadowfork.experiment.meadowAt
import com.avinash.meadowfork.simulation.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class Trend(val sheepLabel: String, val grassLabel: String) {
    Up("More", "More"),
    Same("About the same", "About the same"),
    Down("Fewer", "Less"),
}

class LessonState(
    private val seed: Long = 42,
    val forkDay: Int = 28,
    val endDay: Int = forkDay + 70,
) {
    val opening: Snapshot = meadowAt(seed, forkDay)

    var sheepPrediction by mutableStateOf<Trend?>(null)
    var grassPrediction by mutableStateOf<Trend?>(null)
    var run by mutableStateOf<ForkRun?>(null)
        private set
    var forking by mutableStateOf(false)
        private set
    var day by mutableStateOf(forkDay)
        private set
    var branch by mutableStateOf(Branch.Comparison)
    var playing by mutableStateOf(false)
    var reachedEnd by mutableStateOf(false)
        private set

    val canFork: Boolean get() = sheepPrediction != null && grassPrediction != null && run == null && !forking

    suspend fun fork() {
        forking = true
        run = withContext(Dispatchers.Default) { forkMeadow(seed, forkDay, endDay) }
        forking = false
        select(forkDay)
        playing = true
    }

    suspend fun play() {
        if (day == endDay) select(forkDay)
        while (day < endDay) {
            delay(DAY_MILLIS)
            select(day + 1)
        }
        playing = false
    }

    fun scrub(toDay: Int) {
        playing = false
        select(toDay.coerceIn(forkDay, endDay))
    }

    fun skipToResults() = scrub(endDay)

    fun snapshot(branch: Branch): Snapshot = run?.snapshot(branch, day) ?: opening

    private fun select(newDay: Int) {
        day = newDay
        if (newDay == endDay) reachedEnd = true
    }

    private companion object {
        const val DAY_MILLIS = 200L
    }
}
