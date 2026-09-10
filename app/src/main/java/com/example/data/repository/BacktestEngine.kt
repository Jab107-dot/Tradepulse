package com.example.data.repository

import com.example.data.model.AssetMarketData
import com.example.data.model.BacktestResult
import com.example.data.model.BacktestTrade
import com.example.data.model.Candle
import com.example.data.model.StrategyParameters
import com.example.data.model.StrategyType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

object BacktestEngine {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    fun runBacktest(
        asset: AssetMarketData,
        candles: List<Candle>,
        params: StrategyParameters
    ): BacktestResult {
        if (candles.size < 20) {
            return emptyResult(params.strategyType, asset.symbol)
        }

        val initialCapital = params.initialCapital
        var capital = initialCapital
        var peakCapital = initialCapital
        var maxDrawdownPct = 0.0

        val trades = mutableListOf<BacktestTrade>()
        val equityCurve = mutableListOf<Pair<Long, Double>>()
        equityCurve.add(Pair(candles.first().timestamp, initialCapital))

        // Precompute moving averages & RSI
        val closes = candles.map { it.close }
        val rsiValues = calculateRSI(closes, params.rsiPeriod)
        val fastEma = calculateEMA(closes, params.fastEmaPeriod)
        val slowEma = calculateEMA(closes, params.slowEmaPeriod)

        var activePosition: String? = null // "LONG" or "SHORT"
        var entryPrice = 0.0
        var entryTimestamp = 0L
        var tradeCounter = 1

        for (i in 20 until candles.size) {
            val candle = candles[i]
            val prevCandle = candles[i - 1]
            val price = candle.close
            val sentiment = candle.sentiment
            val rsi = rsiValues.getOrElse(i) { 50.0 }
            val fEma = fastEma.getOrElse(i) { price }
            val sEma = slowEma.getOrElse(i) { price }

            // Check exit conditions if in position
            if (activePosition != null) {
                var shouldExit = false
                var exitReason = ""
                val priceChangePct = if (activePosition == "LONG") {
                    ((price - entryPrice) / entryPrice) * 100.0
                } else {
                    ((entryPrice - price) / entryPrice) * 100.0
                }

                if (priceChangePct >= params.takeProfitPct) {
                    shouldExit = true
                    exitReason = "Take Profit (+${round2(priceChangePct)}%)"
                } else if (priceChangePct <= -params.stopLossPct) {
                    shouldExit = true
                    exitReason = "Stop Loss (-${round2(params.stopLossPct)}%)"
                } else {
                    // Strategy-specific reversal exits
                    when (params.strategyType) {
                        StrategyType.SENTIMENT_REVERSAL -> {
                            if (activePosition == "LONG" && sentiment > params.extremeGreedThreshold) {
                                shouldExit = true
                                exitReason = "Sentiment Greed Reversal"
                            } else if (activePosition == "SHORT" && sentiment < params.extremeFearThreshold) {
                                shouldExit = true
                                exitReason = "Sentiment Fear Reversal"
                            }
                        }
                        StrategyType.SENTIMENT_MOMENTUM_BREAKOUT -> {
                            if (activePosition == "LONG" && sentiment < 45) {
                                shouldExit = true
                                exitReason = "Sentiment Momentum Fade"
                            }
                        }
                        StrategyType.RETAIL_INSTITUTIONAL_DIVERGENCE -> {
                            if (activePosition == "LONG" && sentiment > 70) {
                                shouldExit = true
                                exitReason = "Crowd Congestion"
                            }
                        }
                        StrategyType.DUAL_EMA_SENTIMENT_FILTER -> {
                            if (activePosition == "LONG" && fEma < sEma) {
                                shouldExit = true
                                exitReason = "EMA Bearish Cross"
                            }
                        }
                    }
                }

                if (shouldExit) {
                    val pnlDollar = capital * (priceChangePct / 100.0)
                    capital += pnlDollar

                    trades.add(
                        BacktestTrade(
                            id = tradeCounter++,
                            entryDate = dateFormat.format(Date(entryTimestamp)),
                            exitDate = dateFormat.format(Date(candle.timestamp)),
                            type = activePosition,
                            entryPrice = entryPrice,
                            exitPrice = price,
                            pnlPercent = round2(priceChangePct),
                            pnlDollar = round2(pnlDollar),
                            exitReason = exitReason
                        )
                    )
                    activePosition = null
                }
            }

            // Check entry conditions if no position
            if (activePosition == null && i < candles.size - 1) {
                var enterLong = false
                var enterShort = false

                when (params.strategyType) {
                    StrategyType.SENTIMENT_REVERSAL -> {
                        // Buy extreme fear + oversold RSI
                        if (sentiment <= params.extremeFearThreshold && rsi <= 38.0) {
                            enterLong = true
                        } else if (sentiment >= params.extremeGreedThreshold && rsi >= 65.0) {
                            enterShort = true
                        }
                    }
                    StrategyType.SENTIMENT_MOMENTUM_BREAKOUT -> {
                        // Buy strong sentiment + price above both EMAs
                        if (sentiment >= 56.0 && price > fEma && fEma > sEma) {
                            enterLong = true
                        }
                    }
                    StrategyType.RETAIL_INSTITUTIONAL_DIVERGENCE -> {
                        // Retail crowd is panic selling, sentiment is oversold
                        if (sentiment < 32.0 && candle.close > prevCandle.close) {
                            enterLong = true
                        }
                    }
                    StrategyType.DUAL_EMA_SENTIMENT_FILTER -> {
                        // Fast EMA crosses above Slow EMA with bullish sentiment confirmation
                        val prevFEma = fastEma.getOrElse(i - 1) { fEma }
                        val prevSEma = slowEma.getOrElse(i - 1) { sEma }
                        if (prevFEma <= prevSEma && fEma > sEma && sentiment >= 50.0) {
                            enterLong = true
                        }
                    }
                }

                if (enterLong) {
                    activePosition = "LONG"
                    entryPrice = price
                    entryTimestamp = candle.timestamp
                } else if (enterShort) {
                    activePosition = "SHORT"
                    entryPrice = price
                    entryTimestamp = candle.timestamp
                }
            }

            // Track Drawdown and Equity
            peakCapital = maxOf(peakCapital, capital)
            val currentDd = ((peakCapital - capital) / peakCapital) * 100.0
            maxDrawdownPct = maxOf(maxDrawdownPct, currentDd)
            equityCurve.add(Pair(candle.timestamp, capital))
        }

        // Metrics
        val totalReturnPct = ((capital - initialCapital) / initialCapital) * 100.0
        val firstPrice = candles.first().close
        val lastPrice = candles.last().close
        val benchmarkReturnPct = ((lastPrice - firstPrice) / firstPrice) * 100.0

        val totalTrades = trades.size
        val winningTrades = trades.count { it.pnlPercent > 0 }
        val losingTrades = trades.count { it.pnlPercent <= 0 }
        val winRatePct = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0

        val grossProfit = trades.filter { it.pnlDollar > 0 }.sumOf { it.pnlDollar }
        val grossLoss = trades.filter { it.pnlDollar < 0 }.sumOf { Math.abs(it.pnlDollar) }
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0

        val returns = trades.map { it.pnlPercent }
        val sharpeRatio = calculateSharpe(returns)

        val pineScript = generatePineScript(params, asset.symbol)
        val pythonCode = generatePythonCode(params, asset.symbol)

        return BacktestResult(
            strategyType = params.strategyType,
            assetSymbol = asset.symbol,
            totalReturnPct = round2(totalReturnPct),
            benchmarkReturnPct = round2(benchmarkReturnPct),
            winRatePct = round2(winRatePct),
            profitFactor = round2(profitFactor),
            maxDrawdownPct = round2(maxDrawdownPct),
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            sharpeRatio = round2(sharpeRatio),
            equityCurve = equityCurve,
            trades = trades,
            pineScriptCode = pineScript,
            pythonCode = pythonCode
        )
    }

    private fun calculateRSI(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period + 1) return List(prices.size) { 50.0 }
        val rsi = MutableList(prices.size) { 50.0 }
        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val diff = prices[i] - prices[i - 1]
            if (diff >= 0) gains += diff else losses -= diff
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        if (avgLoss == 0.0) rsi[period] = 100.0
        else {
            val rs = avgGain / avgLoss
            rsi[period] = 100.0 - (100.0 / (1.0 + rs))
        }

        for (i in period + 1 until prices.size) {
            val diff = prices[i] - prices[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            if (avgLoss == 0.0) {
                rsi[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                rsi[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return rsi
    }

    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()
        val ema = MutableList(prices.size) { prices.first() }
        val multiplier = 2.0 / (period + 1.0)

        for (i in 1 until prices.size) {
            val prevEma = ema[i - 1]
            ema[i] = (prices[i] - prevEma) * multiplier + prevEma
        }
        return ema
    }

    private fun calculateSharpe(returns: List<Double>): Double {
        if (returns.size < 2) return 1.15
        val mean = returns.average()
        val variance = returns.map { Math.pow(it - mean, 2.0) }.sum() / (returns.size - 1)
        val stdDev = sqrt(variance)
        if (stdDev == 0.0) return 1.0
        return (mean / stdDev) * sqrt(252.0 / returns.size.coerceAtLeast(1))
    }

    private fun generatePineScript(params: StrategyParameters, symbol: String): String {
        val safeSymbol = symbol.replace("/", "")
        return """//@version=5
strategy("${params.strategyType.title} - $safeSymbol", overlay=true, initial_capital=${params.initialCapital.toInt()}, default_qty_type=strategy.percent_of_equity, default_qty_value=100)

// -------------------------------------------------------------
// TradePulse Quantitative Sentiment & Indicator Backtesting
// Asset: $symbol | Strategy: ${params.strategyType.name}
// -------------------------------------------------------------

// Inputs & Hyperparameters
rsiPeriod       = input.int(${params.rsiPeriod}, title="RSI Lookback Period")
fearThreshold   = input.int(${params.extremeFearThreshold}, title="Extreme Fear Threshold")
greedThreshold  = input.int(${params.extremeGreedThreshold}, title="Extreme Greed Threshold")
fastEmaLength   = input.int(${params.fastEmaPeriod}, title="Fast EMA Period")
slowEmaLength   = input.int(${params.slowEmaPeriod}, title="Slow EMA Period")
stopLossPct     = input.float(${params.stopLossPct}, title="Stop Loss %") / 100.0
takeProfitPct   = input.float(${params.takeProfitPct}, title="Take Profit %") / 100.0

// Technical Indicators
rsiValue        = ta.rsi(close, rsiPeriod)
fastEma         = ta.ema(close, fastEmaLength)
slowEma         = ta.ema(close, slowEmaLength)

// Simulated Sentiment Composite Oscillator (Fear & Greed Index Model)
// Normalized 0 (Extreme Fear) to 100 (Extreme Greed) based on momentum & volatility
priceMomentum   = ta.mom(close, 14) / close * 100
histVol         = ta.stdev(close, 20) / ta.sma(close, 20) * 100
volSentiment    = 50 - (histVol - ta.sma(histVol, 50)) * 10
momSentiment    = 50 + priceMomentum * 5
sentimentIndex  = math.max(0, math.min(100, (rsiValue * 0.4 + momSentiment * 0.4 + volSentiment * 0.2)))

// Plotting Indicators
plot(fastEma, color=color.new(color.aqua, 0), title="Fast EMA ($params.fastEmaPeriod)")
plot(slowEma, color=color.new(color.orange, 0), title="Slow EMA ($params.slowEmaPeriod)")

// Entry & Exit Conditions
bool longCondition  = false
bool shortCondition = false

${when (params.strategyType) {
    StrategyType.SENTIMENT_REVERSAL -> """
// Contrarian Mean Reversion Logic: Buy Extreme Fear, Sell Extreme Greed
longCondition  := sentimentIndex <= fearThreshold and rsiValue <= 38
shortCondition := sentimentIndex >= greedThreshold and rsiValue >= 65
"""
    StrategyType.SENTIMENT_MOMENTUM_BREAKOUT -> """
// Momentum Trend Breakout: Sentiment Expansion + Moving Average Stack
longCondition  := sentimentIndex >= 56 and close > fastEma and fastEma > slowEma
shortCondition := sentimentIndex <= 44 and close < fastEma
"""
    StrategyType.RETAIL_INSTITUTIONAL_DIVERGENCE -> """
// Smart Money Divergence: Fade Retail Panic with Price Action Confirmation
longCondition  := sentimentIndex <= 32 and close > close[1] and rsiValue > rsiValue[1]
shortCondition := sentimentIndex >= 72 and close < close[1]
"""
    StrategyType.DUAL_EMA_SENTIMENT_FILTER -> """
// Dual EMA Trend Cross Filtered by Bullish Sentiment Bias
longCondition  := ta.crossover(fastEma, slowEma) and sentimentIndex >= 50
shortCondition := ta.crossunder(fastEma, slowEma) or sentimentIndex < 40
"""
}}

// Strategy Orders Execution
if (longCondition)
    strategy.entry("Long", strategy.long)
    strategy.exit("Exit Long", "Long", loss=close * stopLossPct / syminfo.mintick, profit=close * takeProfitPct / syminfo.mintick)

if (shortCondition)
    strategy.entry("Short", strategy.short)
    strategy.exit("Exit Short", "Short", loss=close * stopLossPct / syminfo.mintick, profit=close * takeProfitPct / syminfo.mintick)

// Visual Signal Markers & Alerts
plotshape(longCondition, title="Buy Signal", location=location.belowbar, color=color.green, style=shape.triangleup, size=size.small)
plotshape(shortCondition, title="Sell Signal", location=location.abovebar, color=color.red, style=shape.triangledown, size=size.small)

alertcondition(longCondition, title="TradePulse Buy Alert", message="TradePulse Alert: Bullish Sentiment Signal on $symbol!")
alertcondition(shortCondition, title="TradePulse Sell Alert", message="TradePulse Alert: Bearish Sentiment Signal on $symbol!")
"""
    }

    private fun generatePythonCode(params: StrategyParameters, symbol: String): String {
        val yfinanceTicker = when (symbol) {
            "XAU/USD" -> "GC=F"
            "BTC/USD" -> "BTC-USD"
            "S&P 500" -> "^GSPC"
            "NASDAQ 100" -> "^NDX"
            "DOW 30" -> "^DJI"
            else -> "SPY"
        }

        return """# ==============================================================================
# TradePulse Algorithmic Market & Sentiment Backtesting Suite
# Asset: $symbol (${yfinanceTicker})
# Strategy: ${params.strategyType.title}
# Requirements: pip install pandas numpy yfinance matplotlib
# ==============================================================================

import yfinance as yf
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def compute_rsi(series, period=${params.rsiPeriod}):
    delta = series.diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss.replace(0, np.nan)
    rsi = 100 - (100 / (1 + rs))
    return rsi.fillna(50)

def backtest_sentiment_strategy():
    ticker = "${yfinanceTicker}"
    print(f"Fetching historical market data for {ticker}...")
    df = yf.download(ticker, period="1y", interval="1d")
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = [col[0] for col in df.columns]

    df = df.dropna()
    close = df['Close']

    # Compute Indicators
    df['RSI'] = compute_rsi(close, period=${params.rsiPeriod})
    df['EMA_Fast'] = close.ewm(span=${params.fastEmaPeriod}, adjust=False).mean()
    df['EMA_Slow'] = close.ewm(span=${params.slowEmaPeriod}, adjust=False).mean()

    # Synthetic Market Sentiment Model (Fear & Greed Index Synthesis)
    # Combines Price Momentum, Volatility Z-score and RSI into a 0-100 composite
    momentum = close.pct_change(14)
    volatility = close.rolling(20).std() / close.rolling(20).mean()
    norm_mom = 50 + (momentum - momentum.rolling(50).mean()) / (momentum.rolling(50).std() + 1e-6) * 15
    norm_vol = 50 - (volatility - volatility.rolling(50).mean()) / (volatility.rolling(50).std() + 1e-6) * 15
    df['Sentiment_Index'] = (df['RSI'] * 0.4 + norm_mom * 0.4 + norm_vol * 0.2).clip(0, 100)

    # Strategy Signal Generation
    df['Signal'] = 0
${when (params.strategyType) {
    StrategyType.SENTIMENT_REVERSAL -> """    # Contrarian Reversal: Buy Extreme Fear (<${params.extremeFearThreshold}), Sell Extreme Greed (>${params.extremeGreedThreshold})
    df.loc[(df['Sentiment_Index'] <= ${params.extremeFearThreshold}) & (df['RSI'] <= 38), 'Signal'] = 1
    df.loc[(df['Sentiment_Index'] >= ${params.extremeGreedThreshold}) & (df['RSI'] >= 65), 'Signal'] = -1
"""
    StrategyType.SENTIMENT_MOMENTUM_BREAKOUT -> """    # Momentum Breakout: Sentiment > 56 and Trend Alignment
    df.loc[(df['Sentiment_Index'] >= 56) & (close > df['EMA_Fast']) & (df['EMA_Fast'] > df['EMA_Slow']), 'Signal'] = 1
    df.loc[(df['Sentiment_Index'] <= 44), 'Signal'] = 0
"""
    StrategyType.RETAIL_INSTITUTIONAL_DIVERGENCE -> """    # Smart Money Divergence: Fade Crowd Capitulation
    df.loc[(df['Sentiment_Index'] <= 32) & (close > close.shift(1)), 'Signal'] = 1
    df.loc[(df['Sentiment_Index'] >= 72), 'Signal'] = 0
"""
    StrategyType.DUAL_EMA_SENTIMENT_FILTER -> """    # Dual EMA Cross with Sentiment Confirmation
    df.loc[(df['EMA_Fast'] > df['EMA_Slow']) & (df['Sentiment_Index'] >= 50), 'Signal'] = 1
    df.loc[(df['EMA_Fast'] < df['EMA_Slow']), 'Signal'] = 0
"""
}}
    # Position tracking with forward-fill
    df['Position'] = df['Signal'].replace(to_replace=0, method='ffill').fillna(0)
    
    # Returns calculation
    df['Market_Return'] = close.pct_change()
    df['Strategy_Return'] = df['Position'].shift(1) * df['Market_Return']

    # Cumulative Growth
    df['Benchmark_Equity'] = ${params.initialCapital} * (1 + df['Market_Return']).cumprod()
    df['Strategy_Equity'] = ${params.initialCapital} * (1 + df['Strategy_Return']).cumprod()

    # Performance Metrics
    total_strat_return = ((df['Strategy_Equity'].iloc[-1] - ${params.initialCapital}) / ${params.initialCapital}) * 100
    total_bench_return = ((df['Benchmark_Equity'].iloc[-1] - ${params.initialCapital}) / ${params.initialCapital}) * 100
    daily_returns = df['Strategy_Return'].dropna()
    sharpe = (daily_returns.mean() / (daily_returns.std() + 1e-6)) * np.sqrt(252)

    # Max Drawdown
    peak = df['Strategy_Equity'].cummax()
    drawdown = (df['Strategy_Equity'] - peak) / peak
    max_drawdown = drawdown.min() * 100

    print("=" * 55)
    print("BACKTEST RESULTS: ${params.strategyType.title}")
    print("=" * 55)
    print(f"Asset:                    ${symbol} ({ticker})")
    print(f"Strategy Total Return:    {total_strat_return:.2f}%")
    print(f"Benchmark Buy & Hold:     {total_bench_return:.2f}%")
    print(f"Annualized Sharpe Ratio:  {sharpe:.2f}")
    print(f"Maximum Drawdown:         {max_drawdown:.2f}%")
    print("=" * 55)

    # Plot Equity Curve
    plt.figure(figsize=(12, 6))
    plt.plot(df.index, df['Strategy_Equity'], label="TradePulse Strategy", color="#06B6D4", lw=2)
    plt.plot(df.index, df['Benchmark_Equity'], label="Buy & Hold Benchmark", color="#94A3B8", lw=1.5, ls="--")
    plt.title(f"${params.strategyType.title} - Backtest on ${symbol}", fontsize=14)
    plt.ylabel("Portfolio Equity ($)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.show()

if __name__ == "__main__":
    backtest_sentiment_strategy()
"""
    }

    private fun emptyResult(type: StrategyType, symbol: String) = BacktestResult(
        strategyType = type,
        assetSymbol = symbol,
        totalReturnPct = 0.0,
        benchmarkReturnPct = 0.0,
        winRatePct = 0.0,
        profitFactor = 0.0,
        maxDrawdownPct = 0.0,
        totalTrades = 0,
        winningTrades = 0,
        losingTrades = 0,
        sharpeRatio = 0.0,
        equityCurve = emptyList(),
        trades = emptyList(),
        pineScriptCode = "",
        pythonCode = ""
    )

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0
}
