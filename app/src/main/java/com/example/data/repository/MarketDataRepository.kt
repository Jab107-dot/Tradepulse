package com.example.data.repository

import com.example.data.model.AssetCategory
import com.example.data.model.AssetMarketData
import com.example.data.model.Candle
import com.example.data.model.MarketNewsItem
import com.example.data.model.MarketSentimentDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import kotlin.random.Random

class MarketDataRepository(
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _assets = MutableStateFlow<List<AssetMarketData>>(emptyList())
    val assets: StateFlow<List<AssetMarketData>> = _assets.asStateFlow()

    private val _isLiveTicking = MutableStateFlow(true)
    val isLiveTicking: StateFlow<Boolean> = _isLiveTicking.asStateFlow()

    private val _lastTickTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastTickTimestamp: StateFlow<Long> = _lastTickTimestamp.asStateFlow()

    private val _marketSentiment = MutableStateFlow(createInitialSentiment())
    val marketSentiment: StateFlow<MarketSentimentDetail> = _marketSentiment.asStateFlow()

    private val _newsFeed = MutableStateFlow(createInitialNews())
    val newsFeed: StateFlow<List<MarketNewsItem>> = _newsFeed.asStateFlow()

    init {
        _assets.value = initializeAssets()
        startLiveTickSimulation()
    }

    fun toggleLiveTicking() {
        _isLiveTicking.value = !_isLiveTicking.value
    }

    fun triggerManualRefresh() {
        simulateTick()
    }

    private fun startLiveTickSimulation() {
        externalScope.launch {
            while (true) {
                delay(1800) // update every 1.8 seconds for smooth real-time trading vibe
                if (_isLiveTicking.value) {
                    simulateTick()
                }
            }
        }
    }

    private fun simulateTick() {
        val currentList = _assets.value
        if (currentList.isEmpty()) return

        val updated = currentList.map { asset ->
            // Volatility scale factor per asset
            val volScale = when (asset.id) {
                "BTCUSD" -> 0.0018
                "XAUUSD" -> 0.0007
                "NDX100" -> 0.0009
                "SPX500" -> 0.0006
                "DJI30" -> 0.0005
                else -> 0.0008
            }
            val randomFactor = (Random.nextDouble() - 0.49) * 2 // slight upward/downward drift
            val delta = asset.currentPrice * volScale * randomFactor
            val newPrice = (asset.currentPrice + delta).coerceAtLeast(1.0)
            val newHigh = maxOf(asset.high24h, newPrice)
            val newLow = minOf(asset.low24h, newPrice)
            val change = newPrice - asset.previousClose
            val changePct = (change / asset.previousClose) * 100.0

            // update latest candle close
            val updatedH1 = asset.candlesH1.toMutableList()
            if (updatedH1.isNotEmpty()) {
                val last = updatedH1.last()
                val updatedLast = last.copy(
                    high = maxOf(last.high, newPrice),
                    low = minOf(last.low, newPrice),
                    close = newPrice,
                    volume = last.volume + Random.nextDouble(5.0, 50.0)
                )
                updatedH1[updatedH1.lastIndex] = updatedLast
            }

            val updatedSparkline = (asset.sparkline.drop(1) + newPrice)

            asset.copy(
                currentPrice = roundToDecimals(newPrice, if (asset.id == "XAUUSD" || asset.id == "SPX500") 2 else if (asset.id == "BTCUSD") 2 else 1),
                change24h = roundToDecimals(change, 2),
                changePercent24h = roundToDecimals(changePct, 2),
                high24h = roundToDecimals(newHigh, 2),
                low24h = roundToDecimals(newLow, 2),
                sparkline = updatedSparkline,
                candlesH1 = updatedH1
            )
        }

        _assets.value = updated
        _lastTickTimestamp.value = System.currentTimeMillis()
    }

    private fun initializeAssets(): List<AssetMarketData> {
        return listOf(
            createAsset(
                id = "XAUUSD",
                symbol = "XAU/USD",
                name = "Gold Spot / US Dollar",
                category = AssetCategory.COMMODITIES,
                basePrice = 2684.50,
                previousClose = 2671.20,
                high24h = 2692.80,
                low24h = 2668.40,
                volume = 184520.0,
                spread = 0.25,
                sentimentScore = 68,
                sentimentLabel = "Greed",
                retailLongPct = 64,
                institutionalLongPct = 58,
                socialSentiment = 48
            ),
            createAsset(
                id = "BTCUSD",
                symbol = "BTC/USD",
                name = "Bitcoin / US Dollar",
                category = AssetCategory.CRYPTO,
                basePrice = 89450.00,
                previousClose = 87200.00,
                high24h = 90800.00,
                low24h = 86900.00,
                volume = 42560000.0,
                spread = 3.50,
                sentimentScore = 78,
                sentimentLabel = "Extreme Greed",
                retailLongPct = 76,
                institutionalLongPct = 81,
                socialSentiment = 82
            ),
            createAsset(
                id = "SPX500",
                symbol = "S&P 500",
                name = "S&P 500 Index (SPX)",
                category = AssetCategory.INDICES,
                basePrice = 5892.40,
                previousClose = 5865.10,
                high24h = 5912.30,
                low24h = 5858.70,
                volume = 2410000.0,
                spread = 0.50,
                sentimentScore = 62,
                sentimentLabel = "Greed",
                retailLongPct = 55,
                institutionalLongPct = 69,
                socialSentiment = 36
            ),
            createAsset(
                id = "NDX100",
                symbol = "NASDAQ 100",
                name = "Nasdaq 100 Tech Index (NDX)",
                category = AssetCategory.INDICES,
                basePrice = 20845.20,
                previousClose = 20620.00,
                high24h = 20920.80,
                low24h = 20580.40,
                volume = 3890000.0,
                spread = 1.20,
                sentimentScore = 71,
                sentimentLabel = "Greed",
                retailLongPct = 68,
                institutionalLongPct = 74,
                socialSentiment = 64
            ),
            createAsset(
                id = "DJI30",
                symbol = "DOW 30",
                name = "Dow Jones Industrial Average",
                category = AssetCategory.INDICES,
                basePrice = 43490.00,
                previousClose = 43550.00,
                high24h = 43680.00,
                low24h = 43390.00,
                volume = 1240000.0,
                spread = 2.00,
                sentimentScore = 51,
                sentimentLabel = "Neutral",
                retailLongPct = 48,
                institutionalLongPct = 53,
                socialSentiment = 12
            )
        )
    }

    private fun createAsset(
        id: String,
        symbol: String,
        name: String,
        category: AssetCategory,
        basePrice: Double,
        previousClose: Double,
        high24h: Double,
        low24h: Double,
        volume: Double,
        spread: Double,
        sentimentScore: Int,
        sentimentLabel: String,
        retailLongPct: Int,
        institutionalLongPct: Int,
        socialSentiment: Int
    ): AssetMarketData {
        val change = basePrice - previousClose
        val changePct = (change / previousClose) * 100.0

        val candlesH1 = generateRealisticCandles(basePrice, 48, TimeUnit.HOURS.toMillis(1), 0.004, sentimentScore)
        val candlesH4 = generateRealisticCandles(basePrice, 60, TimeUnit.HOURS.toMillis(4), 0.008, sentimentScore)
        val candlesD1 = generateRealisticCandles(basePrice, 90, TimeUnit.DAYS.toMillis(1), 0.015, sentimentScore)
        val candlesW1 = generateRealisticCandles(basePrice, 52, TimeUnit.DAYS.toMillis(7), 0.035, sentimentScore)

        val sparkline = candlesH1.takeLast(20).map { it.close }

        return AssetMarketData(
            id = id,
            symbol = symbol,
            name = name,
            category = category,
            currentPrice = basePrice,
            previousClose = previousClose,
            change24h = roundToDecimals(change, 2),
            changePercent24h = roundToDecimals(changePct, 2),
            high24h = high24h,
            low24h = low24h,
            volume24h = volume,
            spread = spread,
            sentimentScore = sentimentScore,
            sentimentLabel = sentimentLabel,
            retailLongPct = retailLongPct,
            institutionalLongPct = institutionalLongPct,
            socialSentiment = socialSentiment,
            sparkline = sparkline,
            candlesH1 = candlesH1,
            candlesH4 = candlesH4,
            candlesD1 = candlesD1,
            candlesW1 = candlesW1
        )
    }

    private fun generateRealisticCandles(
        targetPrice: Double,
        count: Int,
        intervalMs: Long,
        volatility: Double,
        sentimentScore: Int
    ): List<Candle> {
        val list = mutableListOf<Candle>()
        val now = System.currentTimeMillis()
        var currentPrice = targetPrice * (1.0 - (count * 0.0012 * (sentimentScore - 45) / 50.0))

        for (i in 0 until count) {
            val candleTime = now - (count - 1 - i) * intervalMs
            val trendBias = sin(i * 0.2) * 0.003 + ((sentimentScore - 50) / 100.0) * 0.002
            val randomFactor = (Random.nextDouble() - 0.48) * volatility + trendBias
            val open = currentPrice
            val close = (open * (1.0 + randomFactor)).coerceAtLeast(open * 0.5)
            val high = maxOf(open, close) * (1.0 + Random.nextDouble(0.0005, volatility * 0.6))
            val low = minOf(open, close) * (1.0 - Random.nextDouble(0.0005, volatility * 0.6))
            val volume = Random.nextDouble(100.0, 5000.0) * (if (close > open) 1.2 else 0.9)
            val candleSentiment = (sentimentScore + (sin(i * 0.3) * 15) + (if (close > open) 5 else -5)).coerceIn(10.0, 95.0)

            list.add(
                Candle(
                    timestamp = candleTime,
                    open = roundToDecimals(open, 2),
                    high = roundToDecimals(high, 2),
                    low = roundToDecimals(low, 2),
                    close = roundToDecimals(close, 2),
                    volume = roundToDecimals(volume, 1),
                    sentiment = roundToDecimals(candleSentiment, 1)
                )
            )
            currentPrice = close
        }

        // Adjust last candle close to match targetPrice nicely
        if (list.isNotEmpty()) {
            val last = list.last()
            list[list.lastIndex] = last.copy(close = targetPrice, high = maxOf(last.high, targetPrice), low = minOf(last.low, targetPrice))
        }

        return list
    }

    private fun createInitialSentiment(): MarketSentimentDetail {
        return MarketSentimentDetail(
            compositeScore = 67,
            label = "Greed",
            retailLongPercent = 65,
            retailShortPercent = 35,
            institutionalLongPercent = 71,
            institutionalShortPercent = 29,
            socialSentimentScore = 58,
            volatilityIndex = 14.8, // Low VIX implies complacency / greed
            summary = "Market sentiment leans firmly into Greed driven by institutional accumulation in Bitcoin and safe-haven rotation in Gold amidst cooling macro rate volatility.",
            momentumRsi = 61.4,
            putCallRatio = 0.72 // Bullish bias
        )
    }

    private fun createInitialNews(): List<MarketNewsItem> {
        return listOf(
            MarketNewsItem(
                id = "n1",
                title = "Gold (XAU/USD) Consolidates Near Highs as Central Bank Inflows Accelerate",
                source = "Reuters Finance",
                timeAgo = "12m ago",
                sentimentScore = 74,
                sentimentTag = "BULLISH",
                relatedAsset = "XAU/USD"
            ),
            MarketNewsItem(
                id = "n2",
                title = "Bitcoin Surges Toward Key Resistance as Institutional ETF Net Inflows Hit 3-Week High",
                source = "Bloomberg Crypto",
                timeAgo = "28m ago",
                sentimentScore = 88,
                sentimentTag = "BULLISH",
                relatedAsset = "BTC/USD"
            ),
            MarketNewsItem(
                id = "n3",
                title = "Tech Sector Leads S&P 500 and Nasdaq Higher After Robust AI Earnings Print",
                source = "Financial Times",
                timeAgo = "1h ago",
                sentimentScore = 62,
                sentimentTag = "BULLISH",
                relatedAsset = "NASDAQ 100"
            ),
            MarketNewsItem(
                id = "n4",
                title = "Dow Jones Pauses as Industrial Valuations Face Treasury Yield Pressure",
                source = "Wall Street Journal",
                timeAgo = "2h ago",
                sentimentScore = -18,
                sentimentTag = "BEARISH",
                relatedAsset = "DOW 30"
            ),
            MarketNewsItem(
                id = "n5",
                title = "Global Liquidity Index Rises 1.4% MoM Signaling Prolonged Risk-On Environment",
                source = "MarketWatch",
                timeAgo = "3h ago",
                sentimentScore = 55,
                sentimentTag = "BULLISH",
                relatedAsset = "S&P 500"
            )
        )
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return Math.round(value * multiplier) / multiplier
    }
}
