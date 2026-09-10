package com.example.data.model

enum class AssetCategory(val displayName: String) {
    COMMODITIES("Commodities"),
    CRYPTO("Crypto"),
    INDICES("Equity Indices")
}

enum class Timeframe(val label: String, val daysSpan: Int) {
    H1("1H", 3),
    H4("4H", 14),
    D1("1D", 90),
    W1("1W", 365)
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val sentiment: Double // 0 to 100
)

data class AssetMarketData(
    val id: String,
    val symbol: String,
    val name: String,
    val category: AssetCategory,
    val currentPrice: Double,
    val previousClose: Double,
    val change24h: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val spread: Double,
    val sentimentScore: Int, // 0 - 100
    val sentimentLabel: String,
    val retailLongPct: Int,
    val institutionalLongPct: Int,
    val socialSentiment: Int, // -100 to +100
    val sparkline: List<Double>,
    val candlesH1: List<Candle>,
    val candlesH4: List<Candle>,
    val candlesD1: List<Candle>,
    val candlesW1: List<Candle>
) {
    fun getCandlesForTimeframe(timeframe: Timeframe): List<Candle> = when (timeframe) {
        Timeframe.H1 -> candlesH1
        Timeframe.H4 -> candlesH4
        Timeframe.D1 -> candlesD1
        Timeframe.W1 -> candlesW1
    }
}
