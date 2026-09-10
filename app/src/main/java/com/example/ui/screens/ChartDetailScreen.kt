package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.InteractiveChart
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.util.Locale

@Composable
fun ChartDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val selectedAssetId by viewModel.selectedAssetId.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val chartDisplayType by viewModel.chartDisplayType.collectAsStateWithLifecycle()
    val showFastEma by viewModel.showFastEma.collectAsStateWithLifecycle()
    val showSlowEma by viewModel.showSlowEma.collectAsStateWithLifecycle()
    val showRsi by viewModel.showRsi.collectAsStateWithLifecycle()
    val showSentiment by viewModel.showSentimentOverlay.collectAsStateWithLifecycle()

    val selectedIndex = assets.indexOfFirst { it.id == selectedAssetId }.coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("chart_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Historical Data & Technical Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Interactive OHLCV Candlestick engine with integrated Sentiment overlay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Asset Switcher Tabs
        item {
            if (assets.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = TerminalSurface,
                    contentColor = CryptoCyan,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        if (selectedIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                color = CryptoCyan
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier.testTag("chart_asset_tab_row")
                ) {
                    assets.forEachIndexed { index, asset ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { viewModel.selectAsset(asset.id) },
                            text = {
                                Text(
                                    asset.symbol,
                                    fontSize = 13.sp,
                                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("chart_tab_${asset.id}")
                        )
                    }
                }
            }
        }

        // Chart Viewport
        item {
            selectedAsset?.let { asset ->
                val candles = asset.getCandlesForTimeframe(selectedTimeframe)
                InteractiveChart(
                    candles = candles,
                    selectedTimeframe = selectedTimeframe,
                    onSelectTimeframe = { viewModel.selectTimeframe(it) },
                    chartType = chartDisplayType,
                    onToggleChartType = { viewModel.setChartDisplayType(it) },
                    showFastEma = showFastEma,
                    onToggleFastEma = { viewModel.toggleFastEma() },
                    showSlowEma = showSlowEma,
                    onToggleSlowEma = { viewModel.toggleSlowEma() },
                    showRsi = showRsi,
                    onToggleRsi = { viewModel.toggleRsi() },
                    showSentiment = showSentiment,
                    onToggleSentiment = { viewModel.toggleSentimentOverlay() }
                )
            }
        }

        // Technical Diagnostics & Market Metrics Card
        item {
            selectedAsset?.let { asset ->
                val candles = asset.getCandlesForTimeframe(selectedTimeframe)
                val lastCandle = candles.lastOrNull()
                val isPositive = asset.changePercent24h >= 0

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("technical_metrics_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${asset.symbol} Technical Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricItemBox("Current Price", formatPrice(asset.currentPrice), CryptoCyan, Modifier.weight(1f))
                            MetricItemBox("24h Change", "${if (isPositive) "+" else ""}${asset.changePercent24h}%", if (isPositive) BullishGreen else BearishRed, Modifier.weight(1f))
                            MetricItemBox("24h Volume", formatVolume(asset.volume24h), GoldAccent, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricItemBox("Sentiment Index", "${asset.sentimentScore}/100", if (asset.sentimentScore > 60) BullishGreen else if (asset.sentimentScore < 40) BearishRed else GoldAccent, Modifier.weight(1f))
                            MetricItemBox("Retail Longs", "${asset.retailLongPct}%", BullishGreen, Modifier.weight(1f))
                            MetricItemBox("Inst. Longs", "${asset.institutionalLongPct}%", CryptoCyan, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricItemBox("Bid / Ask Spread", "${asset.spread}", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                            MetricItemBox("24h Range High", formatPrice(asset.high24h), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                            MetricItemBox("24h Range Low", formatPrice(asset.low24h), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetricItemBox(
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000.0) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", volume / 1_000_000_000)
        volume >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000)
        else -> String.format(Locale.US, "%.0f", volume)
    }
}
