package com.avinash.meadowfork.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avinash.meadowfork.experiment.Branch

val WithWolvesLineColor = Color(0xFF37474F)
val WolvesRemovedLineColor = Color(0xFFD84315)

private val Branch.lineColor: Color
    get() = when (this) {
        Branch.Baseline -> WithWolvesLineColor
        Branch.Comparison -> WolvesRemovedLineColor
    }

private val Branch.dashed: Boolean get() = this == Branch.Baseline

@Composable
fun ComparisonChart(
    title: String,
    series: Map<Branch, List<Int>>,
    firstDay: Int,
    selectedDay: Int,
    revealDay: Int,
    maxValue: Int,
    format: (Int) -> String,
    branchLabel: (Branch) -> String,
    landmarkValue: Int? = null,
    markerDay: Int? = null,
    footnote: String? = null,
) {
    val selectedIndex = selectedDay - firstDay
    val revealIndex = revealDay - firstDay
    val cursorColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Text("$title · day $selectedDay", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Branch.entries.forEach { branch ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Canvas(Modifier.size(20.dp, 10.dp)) {
                        drawLine(
                            branch.lineColor,
                            Offset(0f, size.height / 2),
                            Offset(size.width, size.height / 2),
                            strokeWidth = 3.dp.toPx(),
                            pathEffect = if (branch.dashed) dashes() else null,
                        )
                    }
                    Text(
                        "${branchLabel(branch)}: ${format(series.getValue(branch)[selectedIndex])}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(120.dp).semantics {
                contentDescription = "$title chart from day $firstDay to day ${firstDay + revealIndex}"
            },
        ) {
            val lastIndex = series.values.first().lastIndex
            fun x(index: Int) = size.width * index / lastIndex
            fun y(value: Int) = size.height * (1f - value.toFloat() / maxValue)

            drawLine(cursorColor.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height))
            landmarkValue?.let {
                drawLine(cursorColor, Offset(0f, y(it)), Offset(size.width, y(it)), pathEffect = dots())
            }
            markerDay?.let { day ->
                if (day <= firstDay + revealIndex) {
                    val markerX = x(day - firstDay)
                    drawLine(WolvesRemovedLineColor, Offset(markerX, 0f), Offset(markerX, size.height), pathEffect = dots())
                }
            }
            Branch.entries.forEach { branch ->
                val values = series.getValue(branch)
                val path = Path().apply {
                    moveTo(x(0), y(values[0]))
                    for (index in 1..revealIndex) lineTo(x(index), y(values[index]))
                }
                drawPath(
                    path,
                    branch.lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), pathEffect = if (branch.dashed) dashes() else null),
                )
            }
            val cursorX = x(selectedIndex)
            drawLine(cursorColor, Offset(cursorX, 0f), Offset(cursorX, size.height), strokeWidth = 1.5.dp.toPx())
        }
        footnote?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}

private fun DrawScope.dashes() = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))

private fun DrawScope.dots() = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
