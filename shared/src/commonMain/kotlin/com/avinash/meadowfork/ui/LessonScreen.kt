package com.avinash.meadowfork.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avinash.meadowfork.experiment.Branch
import com.avinash.meadowfork.experiment.BranchTotals
import com.avinash.meadowfork.experiment.ForkRun
import com.avinash.meadowfork.simulation.MeadowRules
import com.avinash.meadowfork.simulation.WorldState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Branch.label: String
    get() = when (this) {
        Branch.Baseline -> "With wolves"
        Branch.Comparison -> "Wolves removed"
    }

@Composable
fun LessonScreen() {
    val lesson = remember { LessonState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(lesson.playing) {
        if (lesson.playing) lesson.play()
    }
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (lesson.run == null) {
                        Button(
                            onClick = { scope.launch { lesson.fork() } },
                            enabled = lesson.canFork,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (lesson.forking) "Copying the meadow…" else "Remove wolves in a copy")
                        }
                    } else {
                        OutlinedButton(onClick = { lesson.playing = !lesson.playing }, modifier = Modifier.weight(1f)) {
                            Text(if (lesson.playing) "Pause" else "Play")
                        }
                        Button(
                            onClick = lesson::skipToResults,
                            enabled = lesson.day < lesson.endDay,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Skip to results")
                        }
                    }
                }
            }
        },
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "If we remove the wolves, will there be more sheep ten weeks later, and what happens to the grass?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            val run = lesson.run
            if (run == null) {
                Prediction(lesson)
                OpeningMeadow(lesson)
            } else {
                ForkedMeadow(lesson, run)
                Charts(lesson, run)
                if (lesson.reachedEnd) {
                    Summary(lesson, run) {
                        lesson.showGrassEvidence()
                        scope.launch { scrollState.animateScrollTo(0) }
                    }
                }
            }
            Legend()
        }
    }
}

@Composable
private fun OpeningMeadow(lesson: LessonState) {
    val world = lesson.opening.world
    MeadowGrid(world, describe("Meadow", world), Modifier.fillMaxWidth())
    Text("Day ${world.day}: ${world.sheep.size} sheep, ${world.wolves.size} wolves, ${grassText(world.readyGrass)} ready grass")
}

@Composable
private fun Prediction(lesson: LessonState) {
    Text("Make a prediction for day ${lesson.endDay} in a copy with no wolves.", fontWeight = FontWeight.SemiBold)
    PredictionRow("Sheep", lesson.sheepPrediction, Trend::sheepLabel) { lesson.sheepPrediction = it }
    PredictionRow("Ready grass", lesson.grassPrediction, Trend::grassLabel) { lesson.grassPrediction = it }
}

@Composable
private fun PredictionRow(title: String, selected: Trend?, label: (Trend) -> String, onSelect: (Trend) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Trend.entries.forEach { trend ->
                FilterChip(selected = selected == trend, onClick = { onSelect(trend) }, label = { Text(label(trend)) })
            }
        }
    }
}

@Composable
private fun ForkedMeadow(lesson: LessonState, run: ForkRun) {
    val shown = lesson.snapshot(lesson.branch).world
    MeadowGrid(shown, describe(lesson.branch.label, shown), Modifier.fillMaxWidth())
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Branch.entries.forEachIndexed { index, branch ->
            SegmentedButton(
                selected = lesson.branch == branch,
                onClick = { lesson.branch = branch },
                shape = SegmentedButtonDefaults.itemShape(index, Branch.entries.size),
            ) {
                Text(branch.label)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Branch.entries.forEach { branch ->
            val world = lesson.snapshot(branch).world
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text(branch.label, fontWeight = FontWeight.SemiBold)
                    Text("${world.sheep.size} sheep")
                    Text("${grassText(world.readyGrass)} ready grass")
                    Text("${world.wolves.size} wolves")
                }
            }
        }
    }
    Column {
        Text("Day ${lesson.day} · week ${(lesson.day - run.forkDay) / 7} of 10", fontWeight = FontWeight.SemiBold)
        Slider(
            value = lesson.day.toFloat(),
            onValueChange = { lesson.scrub(it.roundToInt()) },
            valueRange = run.forkDay.toFloat()..run.endDay.toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("▲ Fork: wolves removed on day ${run.forkDay}", style = MaterialTheme.typography.labelMedium)
            Text("Day ${run.endDay}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Charts(lesson: LessonState, run: ForkRun) {
    val sheep = Branch.entries.associateWith { branch -> run.history(branch).map { it.world.sheep.size } }
    val grass = Branch.entries.associateWith { branch -> run.history(branch).map { it.world.readyGrass } }
    val evidenceDay = lesson.grassEvidenceDay
    ComparisonChart(
        title = "Sheep alive",
        series = sheep,
        firstDay = run.forkDay,
        selectedDay = lesson.day,
        revealDay = lesson.revealDay,
        maxValue = sheep.values.maxOf { it.max() } * 11 / 10,
        format = { "$it" },
        branchLabel = { it.label },
    )
    val crossing = evidenceDay?.takeIf { it <= lesson.revealDay }
        ?.let { " ${Branch.Comparison.label} first fell below it on day $it." }
        .orEmpty()
    ComparisonChart(
        title = "Ready grass",
        series = grass,
        firstDay = run.forkDay,
        selectedDay = lesson.day,
        revealDay = lesson.revealDay,
        maxValue = MeadowRules.TILES,
        format = ::percentText,
        branchLabel = { it.label },
        landmarkValue = MeadowRules.TILES * GRASS_LANDMARK_PERCENT / 100,
        markerDay = evidenceDay,
        footnote = "Dotted line: $GRASS_LANDMARK_PERCENT% ready grass.$crossing",
    )
}

@Composable
private fun Summary(lesson: LessonState, run: ForkRun, onGrassEvidence: () -> Unit) {
    val baseline = run.totals(Branch.Baseline)
    val removal = run.totals(Branch.Comparison)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("After 10 weeks (day ${run.endDay})", style = MaterialTheme.typography.titleMedium)
            SummaryRow("", Branch.Baseline.label, Branch.Comparison.label, "Difference", bold = true)
            SummaryRow("Sheep alive", "${baseline.endSheep}", "${removal.endSheep}", signed(removal.endSheep - baseline.endSheep))
            SummaryRow(
                "Ready grass",
                percentText(baseline.endReadyGrass),
                percentText(removal.endReadyGrass),
                "${signed(removal.endReadyGrass - baseline.endReadyGrass)} tiles",
            )
            SummaryRow(
                "Sheep that starved",
                "${baseline.sheepStarved}",
                "${removal.sheepStarved}",
                signed(removal.sheepStarved - baseline.sheepStarved),
            )
            val evidenceDay = lesson.grassEvidenceDay
            OutlinedButton(onClick = onGrassEvidence, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (evidenceDay != null) {
                        "Ready grass: show day $evidenceDay, first below $GRASS_LANDMARK_PERCENT%"
                    } else {
                        "Ready grass: never below $GRASS_LANDMARK_PERCENT%, show day ${run.endDay}"
                    },
                )
            }
            OutlinedButton(onClick = { lesson.showAccounting = !lesson.showAccounting }, modifier = Modifier.fillMaxWidth()) {
                Text(if (lesson.showAccounting) "Hide sheep accounting" else "Sheep that starved: show accounting")
            }
            if (lesson.showAccounting) SheepAccounting(baseline, removal, run)
            val sheep = lesson.sheepPrediction?.sheepLabel?.lowercase()
            val grass = lesson.grassPrediction?.grassLabel?.lowercase()
            Text("You predicted: sheep $sheep, ready grass $grass.", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SheepAccounting(baseline: BranchTotals, removal: BranchTotals, run: ForkRun) {
    Text(
        "Sheep accounting, totals over days ${run.forkDay + 1}–${run.endDay}",
        style = MaterialTheme.typography.titleSmall,
    )
    SummaryRow("", Branch.Baseline.label, Branch.Comparison.label, "Difference", bold = true)
    listOf<Pair<String, (BranchTotals) -> Int>>(
        "Start (day ${run.forkDay})" to BranchTotals::startSheep,
        "+ Born" to BranchTotals::sheepBorn,
        "− Eaten by wolves" to BranchTotals::sheepEaten,
        "− Starved" to BranchTotals::sheepStarved,
        "= Alive (day ${run.endDay})" to BranchTotals::endSheep,
    ).forEach { (label, value) ->
        SummaryRow(label, "${value(baseline)}", "${value(removal)}", signed(value(removal) - value(baseline)))
    }
    Branch.entries.forEach { branch ->
        val t = run.totals(branch)
        Text(
            "${branch.label}: ${t.startSheep} + ${t.sheepBorn} − ${t.sheepEaten} − ${t.sheepStarved} = ${t.endSheep}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Text(
        "How can the wolf-free meadow end with more sheep alive and also record more sheep that starved?",
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SummaryRow(label: String, baseline: String, removal: String, difference: String, bold: Boolean = false) {
    val weight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        listOf(label, baseline, removal, difference).forEachIndexed { index, value ->
            Text(
                value,
                Modifier.weight(if (index == 0) 1.3f else 1f),
                fontWeight = if (index == 0) FontWeight.SemiBold else weight,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("Ready grass") { Box(Modifier.size(14.dp).background(ReadyGrassColor)) }
        LegendItem("Regrowing") { Box(Modifier.size(14.dp).background(GrazedGrassColor)) }
        LegendItem("Sheep") {
            Box(Modifier.size(14.dp).background(SheepColor, CircleShape).border(1.dp, WolfColor, CircleShape))
        }
        LegendItem("Wolf") { Box(Modifier.size(14.dp).background(WolfColor)) }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun describe(label: String, world: WorldState) =
    "$label meadow on day ${world.day}: ${world.sheep.size} sheep, ${world.wolves.size} wolves, " +
        "${grassText(world.readyGrass)} ready grass"

private fun grassText(tiles: Int) = "${percentText(tiles)} ($tiles tiles)"

private fun percentText(tiles: Int): String {
    val tenths = tiles * 1000 / MeadowRules.TILES
    return if (tenths % 10 == 0) "${tenths / 10}%" else "${tenths / 10}.${tenths % 10}%"
}

private fun signed(value: Int) = if (value > 0) "+$value" else if (value < 0) "−${-value}" else "0"
