package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssetCategory
import com.example.data.model.AssetMarketData
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IndicesPurple
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.util.Locale

@Composable
fun WatchlistBar(
    assets: List<AssetMarketData>,
    selectedAssetId: String,
    onSelectAsset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("watchlist_carousel"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(assets, key = { it.id }) { asset ->
            val isSelected = asset.id == selectedAssetId
            val isPositive = asset.changePercent24h >= 0

            Surface(
                modifier = Modifier
                    .width(135.dp)
                    .clickable { onSelectAsset(asset.id) }
                    .testTag("asset_card_${asset.id}"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else TerminalSurface,
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) CryptoCyan else TerminalSurfaceBorder
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            asset.symbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when (asset.category) {
                                        AssetCategory.COMMODITIES -> GoldAccent
                                        AssetCategory.CRYPTO -> CryptoCyan
                                        AssetCategory.INDICES -> IndicesPurple
                                    },
                                    CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        formatPrice(asset.currentPrice),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${if (isPositive) "+" else ""}${asset.changePercent24h}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) BullishGreen else BearishRed
                        )

                        // Mini Sparkline
                        SparklineCanvas(
                            points = asset.sparkline,
                            isPositive = isPositive,
                            modifier = Modifier
                                .width(42.dp)
                                .height(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetHeroCard(
    asset: AssetMarketData,
    onSetAlertClick: () -> Unit,
    onViewChartClick: () -> Unit,
    onBacktestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = asset.changePercent24h >= 0
    val trendColor = if (isPositive) BullishGreen else BearishRed

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_hero_card"),
        shape = RoundedCornerShape(18.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Category & Asset Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                when (asset.category) {
                                    AssetCategory.COMMODITIES -> GoldAccent.copy(alpha = 0.2f)
                                    AssetCategory.CRYPTO -> CryptoCyan.copy(alpha = 0.2f)
                                    AssetCategory.INDICES -> IndicesPurple.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            asset.category.displayName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (asset.category) {
                                AssetCategory.COMMODITIES -> GoldAccent
                                AssetCategory.CRYPTO -> CryptoCyan
                                AssetCategory.INDICES -> IndicesPurple
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Spread: ${asset.spread}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Live Pulse Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(BullishGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(BullishGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "REAL-TIME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BullishGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Asset Title
            Text(
                asset.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Huge Live Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatPrice(asset.currentPrice),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 24h Change Pill
                Row(
                    modifier = Modifier
                        .background(trendColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, trendColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${if (isPositive) "+" else ""}${formatPrice(asset.change24h)} (${if (isPositive) "+" else ""}${asset.changePercent24h}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 24h High / Low Range Indicator Bar
            RangeBar(
                low = asset.low24h,
                high = asset.high24h,
                current = asset.currentPrice
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSetAlertClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_set_alert_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = CryptoCyan
                    )
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = "Set Alert", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onViewChartClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_view_chart_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = "Chart", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chart", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onBacktestClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_backtest_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = GoldAccent
                    )
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Backtest", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backtest", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RangeBar(low: Double, high: Double, current: Double) {
    val range = (high - low).coerceAtLeast(0.01)
    val ratio = ((current - low) / range).toFloat().coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("24h Low: ${formatPrice(low)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("24h High: ${formatPrice(high)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .background(CryptoCyan, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SparklineCanvas(points: List<Double>, isPositive: Boolean, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 1.0
    val range = (max - min).coerceAtLeast(0.001)
    val color = if (isPositive) BullishGreen else BearishRed

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = width / (points.size - 1)
        val path = Path()

        points.forEachIndexed { index, p ->
            val x = index * step
            val y = (height * (1f - ((p - min) / range).toFloat())).coerceIn(1f, height - 1f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = color, style = Stroke(width = 1.5f))
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000.0) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
