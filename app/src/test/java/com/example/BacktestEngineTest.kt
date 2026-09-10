package com.example

import com.example.data.model.AssetCategory
import com.example.data.model.AssetMarketData
import com.example.data.model.Candle
import com.example.data.model.StrategyParameters
import com.example.data.model.StrategyType
import com.example.data.repository.BacktestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestEngineTest {

    @Test
    fun backtestEngine_generatesValidResultAndCode() {
        val marketRepo = com.example.data.repository.MarketDataRepository()
        val testAsset = marketRepo.assets.value.first()
        val candles = testAsset.candlesD1

        val params = StrategyParameters(
            strategyType = StrategyType.SENTIMENT_REVERSAL,
            stopLossPct = 2.0,
            takeProfitPct = 4.0
        )

        val result = BacktestEngine.runBacktest(testAsset, candles, params)

        assertNotNull(result)
        assertEquals(testAsset.symbol, result.assetSymbol)
        assertTrue(result.equityCurve.isNotEmpty())
        assertTrue(result.winRatePct in 0.0..100.0)

        // Verify Pine Script generation
        assertTrue(result.pineScriptCode.contains("//@version=5"))
        assertTrue(result.pineScriptCode.contains("strategy("))
        assertTrue(result.pineScriptCode.contains("TradePulse"))

        // Verify Python code generation
        assertTrue(result.pythonCode.contains("import pandas as pd"))
        assertTrue(result.pythonCode.contains("def backtest_sentiment_strategy"))
    }
}
