package com.avinash.meadowfork.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avinash.meadowfork.experiment.BatchSummary
import com.avinash.meadowfork.experiment.Branch
import com.avinash.meadowfork.experiment.ForkRun
import com.avinash.meadowfork.experiment.PairedOutcome
import com.avinash.meadowfork.experiment.REPEAT_SEEDS
import com.avinash.meadowfork.experiment.forkMeadow
import com.avinash.meadowfork.experiment.meadowAt
import com.avinash.meadowfork.experiment.pairedOutcome
import com.avinash.meadowfork.simulation.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val GRASS_LANDMARK_PERCENT = 25

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
    var showAccounting by mutableStateOf(false)
    var batchOutcomes by mutableStateOf<List<PairedOutcome>>(emptyList())
        private set
    var batchRunning by mutableStateOf(false)
        private set
    private var batchJob: Job? = null

    val batchSummary: BatchSummary?
        get() = if (!batchRunning && batchOutcomes.isNotEmpty()) BatchSummary.of(batchOutcomes) else null

    val canFork: Boolean get() = sheepPrediction != null && grassPrediction != null && run == null && !forking

    val revealDay: Int get() = if (reachedEnd) endDay else day

    val grassEvidenceDay: Int? get() = run?.firstDayGrassBelow(Branch.Comparison, GRASS_LANDMARK_PERCENT)

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

    fun repeatAcrossSeeds(scope: CoroutineScope) {
        batchJob?.cancel()
        batchOutcomes = emptyList()
        batchRunning = true
        batchJob = scope.launch {
            for (seed in REPEAT_SEEDS) {
                val outcome = withContext(Dispatchers.Default) { pairedOutcome(seed, forkDay, endDay) }
                batchOutcomes = batchOutcomes + outcome
            }
            batchRunning = false
        }
    }

    fun showGrassEvidence() {
        branch = Branch.Comparison
        scrub(grassEvidenceDay ?: endDay)
    }

    fun snapshot(branch: Branch): Snapshot = run?.snapshot(branch, day) ?: opening

    private fun select(newDay: Int) {
        day = newDay
        if (newDay == endDay) reachedEnd = true
    }

    private companion object {
        const val DAY_MILLIS = 200L
    }
}
