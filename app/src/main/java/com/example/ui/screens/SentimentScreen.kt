package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AssetMarketData
import com.example.ui.MainViewModel
import com.example.ui.components.SentimentGauge
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder

@Composable
fun SentimentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val marketSentiment by viewModel.marketSentiment.collectAsStateWithLifecycle()
    val newsFeed by viewModel.newsFeed.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredNews = remember(newsFeed, selectedFilter) {
        when (selectedFilter) {
            "BULLISH" -> newsFeed.filter { it.sentimentTag == "BULLISH" }
            "BEARISH" -> newsFeed.filter { it.sentimentTag == "BEARISH" }
            else -> newsFeed
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("sentiment_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Market Sentiment & Orderflow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Quantifying Fear, Greed, and Smart Money Positioning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Composite Sentiment Speedometer Gauge
        item {
            SentimentGauge(sentiment = marketSentiment)
        }

        // Actionable Sentiment Signals Card
        item {
            SentimentSignalsCard()
        }

        // Per-Asset Sentiment Heatmap & Positioning Cards
        item {
            Text(
                "Cross-Asset Sentiment Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(assets, key = { it.id }) { asset ->
            AssetSentimentCard(asset = asset)
        }

        // News Sentiment Filter Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sentiment News Stream",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL", "BULLISH", "BEARISH").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CryptoCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // News items
        items(filteredNews, key = { it.id }) { news ->
            NewsItemCard(news = news)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SentimentSignalsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sentiment_signals_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Algorithmic Sentiment Signals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SignalRow(
                symbol = "BTC/USD",
                signal = "Greed Climax Warning",
                description = "Institutional long skew at 81% meets overbought RSI (71.5). Watch for contrarian pullback or volatility expansion.",
                accentColor = GoldAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            SignalRow(
                symbol = "XAU/USD",
                signal = "Bullish Smart Money Inflow",
                description = "Institutional positioning (+58%) divergent from retail longs, historically preceding upside continuation.",
                accentColor = BullishGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            SignalRow(
                symbol = "DOW 30",
                signal = "Neutral Indecision",
                description = "Balanced 51/100 sentiment score; wait for clear momentum breakout above 50-day EMA.",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignalRow(
    symbol: String,
    signal: String,
    description: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(symbol, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CryptoCyan)
                Text(signal, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accentColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun AssetSentimentCard(asset: AssetMarketData) {
    val badgeColor = when {
        asset.sentimentScore >= 70 -> BullishGreen
        asset.sentimentScore >= 50 -> CryptoCyan
        else -> BearishRed
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_sentiment_card_${asset.id}"),
        shape = RoundedCornerShape(14.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(asset.symbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(asset.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${asset.sentimentScore}/100 • ${asset.sentimentLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Retail vs Institutional Positioning Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Retail Crowd", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${asset.retailLongPct}% Long", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Institutions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${asset.institutionalLongPct}% Long", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CryptoCyan)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Social Score", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (asset.socialSentiment >= 0) "+${asset.socialSentiment}" else "${asset.socialSentiment}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (asset.socialSentiment >= 0) BullishGreen else BearishRed
                    )
                }
            }
        }
    }
}
