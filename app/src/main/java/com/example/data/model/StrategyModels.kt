package com.example.data.model

enum class StrategyType(
    val title: String,
    val description: String,
    val defaultStopLossPct: Double,
    val defaultTakeProfitPct: Double
) {
    SENTIMENT_REVERSAL(
        title = "Contrarian Sentiment Reversal",
        description = "Buys when Market Sentiment hits Extreme Fear (<25) + Oversold RSI; Sells/Shorts at Extreme Greed (>75).",
        defaultStopLossPct = 2.5,
        defaultTakeProfitPct = 5.0
    ),
    SENTIMENT_MOMENTUM_BREAKOUT(
        title = "Sentiment Momentum Breakout",
        description = "Enters Long when Sentiment breaks above 55 with EMA 20 crossing EMA 50 for trend continuation.",
        defaultStopLossPct = 2.0,
        defaultTakeProfitPct = 6.0
    ),
    RETAIL_INSTITUTIONAL_DIVERGENCE(
        title = "Smart Money Divergence",
        description = "Fades Retail crowd positioning when Institutional positioning is heavily skewed in the opposite direction.",
        defaultStopLossPct = 3.0,
        defaultTakeProfitPct = 7.5
    ),
    DUAL_EMA_SENTIMENT_FILTER(
        title = "Dual EMA + Sentiment Filter",
        description = "Classic Trend Following EMA(20/50) cross, filtered strictly by bullish/bearish market sentiment sentiment alignment.",
        defaultStopLossPct = 2.0,
        defaultTakeProfitPct = 4.5
    )
}

data class StrategyParameters(
    val strategyType: StrategyType = StrategyType.SENTIMENT_REVERSAL,
    val extremeFearThreshold: Int = 25,
    val extremeGreedThreshold: Int = 75,
    val rsiPeriod: Int = 14,
    val fastEmaPeriod: Int = 20,
    val slowEmaPeriod: Int = 50,
    val stopLossPct: Double = 2.5,
    val takeProfitPct: Double = 5.0,
    val initialCapital: Double = 10000.0
)

data class BacktestTrade(
    val id: Int,
    val entryDate: String,
    val exitDate: String,
    val type: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val exitPrice: Double,
    val pnlPercent: Double,
    val pnlDollar: Double,
    val exitReason: String // "Take Profit", "Stop Loss", "Signal Reversal"
)

data class BacktestResult(
    val strategyType: StrategyType,
    val assetSymbol: String,
    val totalReturnPct: Double,
    val benchmarkReturnPct: Double,
    val winRatePct: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val sharpeRatio: Double,
    val equityCurve: List<Pair<Long, Double>>,
    val trades: List<BacktestTrade>,
    val pineScriptCode: String,
    val pythonCode: String
)
