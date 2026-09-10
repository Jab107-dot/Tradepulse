package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.Timeframe
import com.example.ui.ChartDisplayType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IndicatorEmaFast
import com.example.ui.theme.IndicatorEmaSlow
import com.example.ui.theme.IndicatorRsi
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InteractiveChart(
    candles: List<Candle>,
    selectedTimeframe: Timeframe,
    onSelectTimeframe: (Timeframe) -> Unit,
    chartType: ChartDisplayType,
    onToggleChartType: (ChartDisplayType) -> Unit,
    showFastEma: Boolean,
    onToggleFastEma: () -> Unit,
    showSlowEma: Boolean,
    onToggleSlowEma: () -> Unit,
    showRsi: Boolean,
    onToggleRsi: () -> Unit,
    showSentiment: Boolean,
    onToggleSentiment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hoveredCandleIndex by remember { mutableStateOf<Int?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd HH:mm", Locale.US) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_chart_container"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Controls: Timeframe and Chart Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Selector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Timeframe.values().forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTimeframe(tf) },
                            label = {
                                Text(
                                    tf.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CryptoCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("timeframe_chip_${tf.label}")
                        )
                    }
                }

                // Type Toggle (Candles vs Line)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = chartType == ChartDisplayType.CANDLESTICK,
                        onClick = { onToggleChartType(ChartDisplayType.CANDLESTICK) },
                        label = { Text("Candles", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = CryptoCyan
                        ),
                        modifier = Modifier.testTag("chart_type_candles")
                    )
                    FilterChip(
                        selected = chartType == ChartDisplayType.LINE_AREA,
                        onClick = { onToggleChartType(ChartDisplayType.LINE_AREA) },
                        label = { Text("Line", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = CryptoCyan
                        ),
                        modifier = Modifier.testTag("chart_type_line")
                    )
                }
            }

            // Indicator Toggles Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndicatorToggleChip("EMA 20", IndicatorEmaFast, showFastEma, onToggleFastEma)
                IndicatorToggleChip("EMA 50", IndicatorEmaSlow, showSlowEma, onToggleSlowEma)
                IndicatorToggleChip("RSI(14)", IndicatorRsi, showRsi, onToggleRsi)
                IndicatorToggleChip("Sentiment", GoldAccent, showSentiment, onToggleSentiment)
            }

            // Crosshair Scrubber Info Display
            val activeIndex = hoveredCandleIndex
            val candleToDisplay = if (activeIndex != null && activeIndex in candles.indices) {
                candles[activeIndex]
            } else candles.lastOrNull()

            if (candleToDisplay != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateFormat.format(Date(candleToDisplay.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriceLabel("O", candleToDisplay.open)
                        PriceLabel("H", candleToDisplay.high)
                        PriceLabel("L", candleToDisplay.low)
                        PriceLabel("C", candleToDisplay.close, isClose = true, candle = candleToDisplay)
                        Text(
                            "Sent: ${candleToDisplay.sentiment.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (candleToDisplay.sentiment > 60) BullishGreen else if (candleToDisplay.sentiment < 40) BearishRed else GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Price Chart Canvas (Candles / Line + EMAs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val step = size.width / candles.size.coerceAtLeast(1)
                                val idx = (change.position.x / step).toInt().coerceIn(0, candles.size - 1)
                                hoveredCandleIndex = idx
                            },
                            onDragEnd = { hoveredCandleIndex = null },
                            onDragCancel = { hoveredCandleIndex = null }
                        )
                    }
                    .pointerInput(candles) {
                        detectTapGestures(
                            onPress = { offset ->
                                val step = size.width / candles.size.coerceAtLeast(1)
                                hoveredCandleIndex = (offset.x / step).toInt().coerceIn(0, candles.size - 1)
                                tryAwaitRelease()
                                hoveredCandleIndex = null
                            }
                        )
                    }
            ) {
                MainChartCanvas(
                    candles = candles,
                    chartType = chartType,
                    showFastEma = showFastEma,
                    showSlowEma = showSlowEma,
                    hoveredIndex = hoveredCandleIndex
                )
            }

            // Secondary Indicator Canvas (RSI or Sentiment Overlay Subchart)
            AnimatedVisibility(visible = showRsi || showSentiment) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (showRsi) {
                            Text(
                                "RSI (14) Oscillator",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndicatorRsi
                            )
                        }
                        if (showSentiment) {
                            Text(
                                "Market Sentiment Overlay (0-100)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                    ) {
                        SubChartCanvas(
                            candles = candles,
                            showRsi = showRsi,
                            showSentiment = showSentiment,
                            hoveredIndex = hoveredCandleIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorToggleChip(
    label: String,
    accentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(6.dp)
                        .background(accentColor, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, fontSize = 10.sp)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.2f),
            selectedLabelColor = accentColor,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun PriceLabel(prefix: String, value: Double, isClose: Boolean = false, candle: Candle? = null) {
    val color = if (isClose && candle != null) {
        if (candle.close >= candle.open) BullishGreen else BearishRed
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = "$prefix: ${formatPrice(value)}",
        fontSize = 11.sp,
        fontWeight = if (isClose) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
}

@Composable
private fun MainChartCanvas(
    candles: List<Candle>,
    chartType: ChartDisplayType,
    showFastEma: Boolean,
    showSlowEma: Boolean,
    hoveredIndex: Int?
) {
    if (candles.isEmpty()) return

    val minPrice = candles.minOf { it.low }
    val maxPrice = candles.maxOf { it.high }
    val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)

    // Precompute EMAs
    val fastEmas = remember(candles, showFastEma) {
        if (showFastEma) computeEMA(candles.map { it.close }, 20) else emptyList()
    }
    val slowEmas = remember(candles, showSlowEma) {
        if (showSlowEma) computeEMA(candles.map { it.close }, 50) else emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val candleCount = candles.size
        val candleSpacing = width / candleCount.toFloat()
        val candleBodyWidth = (candleSpacing * 0.65f).coerceAtLeast(2f)

        // Draw horizontal grid lines & prices
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = Color(0x1F94A3B8),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        fun getY(price: Double): Float {
            val normalized = ((maxPrice - price) / priceRange).toFloat()
            return normalized * (height - 20f) + 10f
        }

        if (chartType == ChartDisplayType.CANDLESTICK) {
            // Draw Candlesticks
            candles.forEachIndexed { index, candle ->
                val x = index * candleSpacing + candleSpacing / 2f
                val openY = getY(candle.open)
                val closeY = getY(candle.close)
                val highY = getY(candle.high)
                val lowY = getY(candle.low)

                val isBullish = candle.close >= candle.open
                val candleColor = if (isBullish) BullishGreen else BearishRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY)
                val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x - candleBodyWidth / 2f, bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )
            }
        } else {
            // Draw Line / Area
            val path = Path()
            val fillPath = Path()

            candles.forEachIndexed { index, candle ->
                val x = index * candleSpacing + candleSpacing / 2f
                val y = getY(candle.close)
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(CryptoCyan.copy(alpha = 0.35f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = CryptoCyan,
                style = Stroke(width = 2.5f)
            )
        }

        // Draw Fast EMA (20)
        if (showFastEma && fastEmas.size == candleCount) {
            val fastEmaPath = Path()
            fastEmas.forEachIndexed { index, ema ->
                val x = index * candleSpacing + candleSpacing / 2f
                val y = getY(ema)
                if (index == 0) fastEmaPath.moveTo(x, y) else fastEmaPath.lineTo(x, y)
            }
            drawPath(
                path = fastEmaPath,
                color = IndicatorEmaFast,
                style = Stroke(width = 1.8f)
            )
        }

        // Draw Slow EMA (50)
        if (showSlowEma && slowEmas.size == candleCount) {
            val slowEmaPath = Path()
            slowEmas.forEachIndexed { index, ema ->
                val x = index * candleSpacing + candleSpacing / 2f
                val y = getY(ema)
                if (index == 0) slowEmaPath.moveTo(x, y) else slowEmaPath.lineTo(x, y)
            }
            drawPath(
                path = slowEmaPath,
                color = IndicatorEmaSlow,
                style = Stroke(width = 1.8f)
            )
        }

        // Draw Crosshair on scrubber
        if (hoveredIndex != null && hoveredIndex in candles.indices) {
            val hx = hoveredIndex * candleSpacing + candleSpacing / 2f
            val hy = getY(candles[hoveredIndex].close)

            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(hx, 0f),
                end = Offset(hx, height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(0f, hy),
                end = Offset(width, hy),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            drawCircle(
                color = CryptoCyan,
                radius = 4f,
                center = Offset(hx, hy)
            )
        }
    }
}

@Composable
private fun SubChartCanvas(
    candles: List<Candle>,
    showRsi: Boolean,
    showSentiment: Boolean,
    hoveredIndex: Int?
) {
    if (candles.isEmpty()) return

    val rsiValues = remember(candles) { computeRSI(candles.map { it.close }, 14) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val candleSpacing = width / candles.size.toFloat()

        // Draw 30 / 50 / 70 reference threshold levels
        val y70 = height * 0.3f
        val y50 = height * 0.5f
        val y30 = height * 0.7f

        drawLine(
            color = Color(0x22F43F5E),
            start = Offset(0f, y70),
            end = Offset(width, y70),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        drawLine(
            color = Color(0x1F94A3B8),
            start = Offset(0f, y50),
            end = Offset(width, y50),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        drawLine(
            color = Color(0x2210B981),
            start = Offset(0f, y30),
            end = Offset(width, y30),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        fun getSubY(score: Double): Float {
            val clamped = score.coerceIn(0.0, 100.0)
            return (height * (1f - (clamped.toFloat() / 100f))).coerceIn(0f, height)
        }

        // Sentiment Overlay
        if (showSentiment) {
            val sentPath = Path()
            candles.forEachIndexed { i, candle ->
                val x = i * candleSpacing + candleSpacing / 2f
                val y = getSubY(candle.sentiment)
                if (i == 0) sentPath.moveTo(x, y) else sentPath.lineTo(x, y)
            }
            drawPath(
                path = sentPath,
                color = GoldAccent.copy(alpha = 0.9f),
                style = Stroke(width = 1.8f)
            )
        }

        // RSI Line
        if (showRsi && rsiValues.size == candles.size) {
            val rsiPath = Path()
            rsiValues.forEachIndexed { i, rsi ->
                val x = i * candleSpacing + candleSpacing / 2f
                val y = getSubY(rsi)
                if (i == 0) rsiPath.moveTo(x, y) else rsiPath.lineTo(x, y)
            }
            drawPath(
                path = rsiPath,
                color = IndicatorRsi,
                style = Stroke(width = 2.0f)
            )
        }

        // Crosshair vertical
        if (hoveredIndex != null && hoveredIndex in candles.indices) {
            val hx = hoveredIndex * candleSpacing + candleSpacing / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(hx, 0f),
                end = Offset(hx, height),
                strokeWidth = 1f
            )
        }
    }
}

private fun computeEMA(prices: List<Double>, period: Int): List<Double> {
    if (prices.isEmpty()) return emptyList()
    val ema = MutableList(prices.size) { prices.first() }
    val k = 2.0 / (period + 1.0)
    for (i in 1 until prices.size) {
        ema[i] = prices[i] * k + ema[i - 1] * (1.0 - k)
    }
    return ema
}

private fun computeRSI(prices: List<Double>, period: Int): List<Double> {
    if (prices.size <= period) return List(prices.size) { 50.0 }
    val rsi = MutableList(prices.size) { 50.0 }
    var gains = 0.0
    var losses = 0.0

    for (i in 1..period) {
        val diff = prices[i] - prices[i - 1]
        if (diff >= 0) gains += diff else losses -= diff
    }
    var avgGain = gains / period
    var avgLoss = losses / period

    rsi[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))

    for (i in period + 1 until prices.size) {
        val diff = prices[i] - prices[i - 1]
        val gain = if (diff > 0) diff else 0.0
        val loss = if (diff < 0) -diff else 0.0
        avgGain = (avgGain * (period - 1) + gain) / period
        avgLoss = (avgLoss * (period - 1) + loss) / period
        rsi[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
    }
    return rsi
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000.0) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
