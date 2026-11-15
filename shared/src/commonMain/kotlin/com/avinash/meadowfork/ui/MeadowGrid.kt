package com.avinash.meadowfork.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.avinash.meadowfork.simulation.MeadowRules
import com.avinash.meadowfork.simulation.WorldState

val ReadyGrassColor = Color(0xFF4E9F3D)
val GrazedGrassColor = Color(0xFFA1887F)
val SheepColor = Color(0xFFFFFFFF)
val WolfColor = Color(0xFF263238)

@Composable
fun MeadowGrid(world: WorldState, description: String, modifier: Modifier = Modifier) {
    val sheepTiles = world.sheep.map { it.tile }.toSet()
    val wolfTiles = world.wolves.map { it.tile }.toSet()
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = description }) {
        val cell = size.width / MeadowRules.SIZE
        for (tile in 0 until MeadowRules.TILES) {
            val origin = Offset(tile % MeadowRules.SIZE * cell, tile / MeadowRules.SIZE * cell)
            val timer = world.grassTimers[tile]
            val grass = if (timer == 0) {
                ReadyGrassColor
            } else {
                lerp(ReadyGrassColor, GrazedGrassColor, 0.4f + 0.6f * timer / MeadowRules.REGROWTH_DAYS)
            }
            drawRect(grass, origin, Size(cell, cell))
            if (tile in sheepTiles) {
                val center = origin + Offset(cell * 0.35f, cell * 0.5f)
                drawCircle(SheepColor, cell * 0.28f, center)
                drawCircle(WolfColor, cell * 0.28f, center, style = Stroke(cell * 0.06f))
            }
            if (tile in wolfTiles) {
                drawRect(WolfColor, origin + Offset(cell * 0.5f, cell * 0.25f), Size(cell * 0.42f, cell * 0.42f))
            }
        }
    }
}
