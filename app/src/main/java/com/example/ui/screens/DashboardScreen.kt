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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketNewsItem
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AssetHeroCard
import com.example.ui.components.InteractiveChart
import com.example.ui.components.SentimentGauge
import com.example.ui.components.WatchlistBar
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenAlertModal: () -> Unit,
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
    val isLiveTicking by viewModel.isLiveTicking.collectAsStateWithLifecycle()
    val marketSentiment by viewModel.marketSentiment.collectAsStateWithLifecycle()
    val newsFeed by viewModel.newsFeed.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ticker Header & Live Controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Market Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Gold (XAUUSD) • Bitcoin • Major Indices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live Streaming indicator & toggle
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isLiveTicking) BullishGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLiveTicking) BullishGreen.copy(alpha = 0.5f) else TerminalSurfaceBorder
                        ),
                        modifier = Modifier.testTag("live_toggle_surface")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isLiveTicking) BullishGreen else BearishRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isLiveTicking) "LIVE" else "PAUSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiveTicking) BullishGreen else BearishRed
                            )
                            IconButton(
                                onClick = { viewModel.toggleLiveTicking() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiveTicking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle Stream",
                                    tint = if (isLiveTicking) BullishGreen else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.manualRefresh() },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(36.dp)
                            .testTag("manual_refresh_btn")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Price",
                            tint = CryptoCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Horizontal Watchlist Chips Carousel
        item {
            WatchlistBar(
                assets = assets,
                selectedAssetId = selectedAssetId,
                onSelectAsset = { viewModel.selectAsset(it) }
            )
        }

        // Primary Hero Asset Card
        item {
            selectedAsset?.let { asset ->
                AssetHeroCard(
                    asset = asset,
                    onSetAlertClick = onOpenAlertModal,
                    onViewChartClick = { viewModel.selectTab(AppTab.CHART) },
                    onBacktestClick = { viewModel.selectTab(AppTab.BACKTEST) }
                )
            }
        }

        // Interactive Chart with indicators
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

        // Market Sentiment Composite Snapshot
        item {
            SentimentGauge(sentiment = marketSentiment)
        }

        // Financial & Sentiment News Feed
        item {
            Text(
                "Real-Time News & Sentiment Catalysts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(newsFeed, key = { it.id }) { news ->
            NewsItemCard(news = news)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NewsItemCard(news: MarketNewsItem) {
    val tagColor = when (news.sentimentTag) {
        "BULLISH" -> BullishGreen
        "BEARISH" -> BearishRed
        else -> GoldAccent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_card_${news.id}"),
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(top = 4.dp)
                    .background(tagColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        news.relatedAsset,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CryptoCyan
                    )
                    Text(
                        news.timeAgo,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    news.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        news.source,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${news.sentimentTag} (${if (news.sentimentScore > 0) "+" else ""}${news.sentimentScore})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                    }
                }
            }
        }
    }
}
