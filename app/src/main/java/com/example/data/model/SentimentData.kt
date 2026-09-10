package com.example.data.model

data class MarketSentimentDetail(
    val compositeScore: Int, // 0-100 Fear to Greed
    val label: String,
    val retailLongPercent: Int,
    val retailShortPercent: Int,
    val institutionalLongPercent: Int,
    val institutionalShortPercent: Int,
    val socialSentimentScore: Int, // -100 to +100
    val volatilityIndex: Double, // e.g. VIX or Implied Vol
    val summary: String,
    val momentumRsi: Double,
    val putCallRatio: Double
)

data class MarketNewsItem(
    val id: String,
    val title: String,
    val source: String,
    val timeAgo: String,
    val sentimentScore: Int, // -100 to 100
    val sentimentTag: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val relatedAsset: String
)
