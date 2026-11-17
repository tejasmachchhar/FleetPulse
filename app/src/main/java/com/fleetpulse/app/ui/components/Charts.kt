@file:OptIn(ExperimentalTextApi::class)

package com.fleetpulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Double?>,
)

/** Rounded "nice" upper bound for an axis. */
private fun niceMax(raw: Double): Double {
    if (raw <= 0 || raw.isNaN() || raw.isInfinite()) return 1.0
    val exponent = floor(log10(raw))
    val fraction = raw / 10.0.pow(exponent)
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * 10.0.pow(exponent)
}

private fun axisMax(values: List<Double?>): Double {
    val max = values.filterNotNull().maxOrNull() ?: 0.0
    return niceMax(max * 1.05)
}

@Composable
fun ChartLegend(series: List<ChartSeries>, suffix: String = "") {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(s.color, RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    s.name + suffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun GroupedBarChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    stacked: Boolean = false,
    valuePrefix: String = "",
    showLegend: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = onSurface)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showLegend) ChartLegend(series)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            drawBarChart(
                labels = labels,
                series = series,
                stacked = stacked,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                gridColor = grid,
                valuePrefix = valuePrefix,
            )
        }
    }
}

private fun DrawScope.drawBarChart(
    labels: List<String>,
    series: List<ChartSeries>,
    stacked: Boolean,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    gridColor: Color,
    valuePrefix: String,
) {
    if (labels.isEmpty()) return
    val left = 42.dp.toPx()
    val right = 6.dp.toPx()
    val top = 8.dp.toPx()
    val bottom = 20.dp.toPx()
    val plotW = size.width - left - right
    val plotH = size.height - top - bottom
    if (plotW <= 0 || plotH <= 0) return

    val totals = labels.indices.map { i ->
        if (stacked) series.sumOf { it.values.getOrNull(i) ?: 0.0 }
        else series.maxOfOrNull { it.values.getOrNull(i) ?: 0.0 } ?: 0.0
    }
    val max = axisMax(totals)
    val steps = 4
    for (s in 0..steps) {
        val y = top + plotH - (plotH * s / steps)
        drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1f)
        val value = max * s / steps
        val text = if (value >= 1000) "${(value / 1000).toInt()}k" else {
            if (max < 10) "%.1f".format(value) else "%.0f".format(value)
        }
        val layout = textMeasurer.measure(AnnotatedString(text), labelStyle)
        drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
    }

    val groupW = plotW / labels.size
    val barW = if (stacked) {
        (groupW * 0.5f).coerceAtMost(22.dp.toPx())
    } else {
        val count = series.size.coerceAtLeast(1)
        ((groupW * 0.62f) / count).coerceAtMost(16.dp.toPx()).coerceAtLeast(2.dp.toPx())
    }
    val labelStep = ceil(labels.size / 6.0).toInt().coerceAtLeast(1)

    labels.forEachIndexed { i, label ->
        val center = left + groupW * i + groupW / 2f
        if (stacked) {
            var yBase = top + plotH
            series.forEach { s ->
                val v = s.values.getOrNull(i) ?: 0.0
                if (v > 0) {
                    val h = (v / max * plotH).toFloat()
                    drawRect(
                        color = s.color,
                        topLeft = Offset(center - barW / 2f, yBase - h),
                        size = Size(barW, h),
                    )
                    yBase -= h
                }
            }
        } else {
            val totalW = barW * series.size + (series.size - 1) * 2.dp.toPx()
            var x = center - totalW / 2f
            series.forEach { s ->
                val v = s.values.getOrNull(i) ?: 0.0
                val h = (v / max * plotH).toFloat()
                if (h > 0) {
                    drawRoundRect(
                        color = s.color,
                        topLeft = Offset(x, top + plotH - h),
                        size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                }
                x += barW + 2.dp.toPx()
            }
        }
        if (i % labelStep == 0) {
            val layout = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(
                layout,
                topLeft = Offset(center - layout.size.width / 2f, top + plotH + 4.dp.toPx()),
            )
        }
    }
}

@Composable
fun LineChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    valueSuffix: String = "",
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = onSurface)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartLegend(series, valueSuffix)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            drawLineChart(labels, series, textMeasurer, labelStyle, grid)
        }
    }
}

private fun DrawScope.drawLineChart(
    labels: List<String>,
    series: List<ChartSeries>,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    gridColor: Color,
) {
    if (labels.isEmpty()) return
    val left = 42.dp.toPx()
    val right = 8.dp.toPx()
    val top = 10.dp.toPx()
    val bottom = 20.dp.toPx()
    val plotW = size.width - left - right
    val plotH = size.height - top - bottom
    if (plotW <= 0 || plotH <= 0) return

    val max = axisMax(series.flatMap { it.values })
    val steps = 4
    for (s in 0..steps) {
        val y = top + plotH - (plotH * s / steps)
        drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1f)
        val value = max * s / steps
        val text = if (value >= 1000) "${(value / 1000).toInt()}k" else if (max < 10) "%.1f".format(value) else "%.0f".format(value)
        val layout = textMeasurer.measure(AnnotatedString(text), labelStyle)
        drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
    }

    val xFor = { i: Int ->
        if (labels.size == 1) left + plotW / 2f
        else left + plotW * i / (labels.size - 1).toFloat()
    }
    val yFor = { v: Double -> top + plotH - (v / max * plotH).toFloat() }

    series.forEach { s ->
        val path = Path()
        var started = false
        s.values.forEachIndexed { i, value ->
            if (value == null) {
                started = false
            } else {
                val x = xFor(i)
                val y = yFor(value)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
        }
        drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        s.values.forEachIndexed { i, value ->
            if (value != null) {
                drawCircle(s.color, radius = 3.2.dp.toPx(), center = Offset(xFor(i), yFor(value)))
                drawCircle(Color.White, radius = 1.4.dp.toPx(), center = Offset(xFor(i), yFor(value)))
            }
        }
    }

    val labelStep = ceil(labels.size / 6.0).toInt().coerceAtLeast(1)
    labels.forEachIndexed { i, label ->
        if (i % labelStep == 0) {
            val layout = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(
                layout,
                topLeft = Offset(xFor(i) - layout.size.width / 2f, top + plotH + 4.dp.toPx()),
            )
        }
    }
}

data class DonutSlice(val name: String, val value: Double, val color: Color)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String,
    centerValue: String,
    height: Dp = 170.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val total = slices.sumOf { it.value }
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(height), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(height)) {
                val stroke = 22.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                if (total <= 0) {
                    drawArc(
                        color = onSurfaceVariant.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke),
                    )
                } else {
                    var start = -90f
                    slices.filter { it.value > 0 }.forEach { slice ->
                        val sweep = (slice.value / total * 360).toFloat()
                        drawArc(
                            color = slice.color,
                            startAngle = start + 1.5f,
                            sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        )
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                )
                Text(
                    centerValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            slices.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(s.color, RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        s.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatINR(s.value, 0),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Distance bars with a secondary cost line (own colour-coded axes). */
@Composable
fun TrendChart(
    labels: List<String>,
    distance: ChartSeries,
    cost: ChartSeries,
    modifier: Modifier = Modifier,
    height: Dp = 210.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = onSurfaceVariant)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartLegend(listOf(distance, cost))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val left = 42.dp.toPx()
            val right = 44.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 20.dp.toPx()
            val plotW = size.width - left - right
            val plotH = size.height - top - bottom
            if (plotW <= 0 || plotH <= 0) return@Canvas

            val maxKm = axisMax(distance.values)
            val maxCost = axisMax(cost.values)
            for (s in 0..4) {
                val y = top + plotH - (plotH * s / 4)
                drawLine(grid, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1f)
                val kmText = if (maxKm < 10) "%.1f".format(maxKm * s / 4) else "%.0f".format(maxKm * s / 4)
                val layout = textMeasurer.measure(AnnotatedString(kmText), labelStyle)
                drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
                val costText = if (maxCost >= 1000) "₹${(maxCost * s / 4 / 1000).toInt()}k" else "₹${(maxCost * s / 4).toInt()}"
                val cl = textMeasurer.measure(AnnotatedString(costText), labelStyle.copy(color = cost.color))
                drawText(cl, topLeft = Offset(size.width - right + 4.dp.toPx(), y - cl.size.height / 2f))
            }

            val groupW = plotW / labels.size
            labels.forEachIndexed { i, _ ->
                val v = distance.values.getOrNull(i) ?: 0.0
                val h = (v / maxKm * plotH).toFloat()
                val barW = (groupW * 0.46f).coerceAtMost(20.dp.toPx())
                val center = left + groupW * i + groupW / 2f
                if (h > 0) {
                    drawRoundRect(
                        color = distance.color.copy(alpha = 0.85f),
                        topLeft = Offset(center - barW / 2, top + plotH - h),
                        size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                }
            }

            val path = Path()
            var started = false
            cost.values.forEachIndexed { i, value ->
                if (value == null) {
                    started = false
                } else {
                    val x = left + groupW * i + groupW / 2f
                    val y = top + plotH - (value / maxCost * plotH).toFloat()
                    if (!started) {
                        path.moveTo(x, y); started = true
                    } else {
                        path.lineTo(x, y)
                    }
                    drawCircle(cost.color, 3.dp.toPx(), Offset(x, y))
                }
            }
            drawPath(path, cost.color, style = Stroke(width = 2.5.dp.toPx()))

            val labelStep = ceil(labels.size / 6.0).toInt().coerceAtLeast(1)
            labels.forEachIndexed { i, label ->
                if (i % labelStep == 0) {
                    val layout = textMeasurer.measure(AnnotatedString(label), labelStyle)
                    val x = left + groupW * i + groupW / 2f
                    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, top + plotH + 4.dp.toPx()))
                }
            }
        }
    }
}
