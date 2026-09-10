package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlertCondition
import com.example.data.local.AlertRepository
import com.example.data.local.AppDatabase
import com.example.data.local.PriceAlertEntity
import com.example.data.model.AssetMarketData
import com.example.data.model.BacktestResult
import com.example.data.model.MarketNewsItem
import com.example.data.model.MarketSentimentDetail
import com.example.data.model.StrategyParameters
import com.example.data.model.StrategyType
import com.example.data.model.Timeframe
import com.example.data.repository.BacktestEngine
import com.example.data.repository.MarketDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    CHART("Chart & Viz"),
    SENTIMENT("Sentiment"),
    ALERTS("Price Alerts"),
    BACKTEST("Backtest & Code")
}

enum class ChartDisplayType {
    CANDLESTICK,
    LINE_AREA
}

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val marketRepo: MarketDataRepository = MarketDataRepository(),
    private val alertRepo: AlertRepository = AlertRepository(AppDatabase.getInstance(application).priceAlertDao())
) : AndroidViewModel(application) {

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        return MainViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }

    val assets: StateFlow<List<AssetMarketData>> = marketRepo.assets
    val isLiveTicking: StateFlow<Boolean> = marketRepo.isLiveTicking
    val lastTickTimestamp: StateFlow<Long> = marketRepo.lastTickTimestamp
    val marketSentiment: StateFlow<MarketSentimentDetail> = marketRepo.marketSentiment
    val newsFeed: StateFlow<List<MarketNewsItem>> = marketRepo.newsFeed

    val allAlerts: StateFlow<List<PriceAlertEntity>> = alertRepo.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedAssetId = MutableStateFlow("XAUUSD")
    val selectedAssetId: StateFlow<String> = _selectedAssetId.asStateFlow()

    val selectedAsset: StateFlow<AssetMarketData?> = combine(assets, _selectedAssetId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTimeframe = MutableStateFlow(Timeframe.D1)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _chartDisplayType = MutableStateFlow(ChartDisplayType.CANDLESTICK)
    val chartDisplayType: StateFlow<ChartDisplayType> = _chartDisplayType.asStateFlow()

    private val _showFastEma = MutableStateFlow(true)
    val showFastEma: StateFlow<Boolean> = _showFastEma.asStateFlow()

    private val _showSlowEma = MutableStateFlow(true)
    val showSlowEma: StateFlow<Boolean> = _showSlowEma.asStateFlow()

    private val _showRsi = MutableStateFlow(true)
    val showRsi: StateFlow<Boolean> = _showRsi.asStateFlow()

    private val _showSentimentOverlay = MutableStateFlow(true)
    val showSentimentOverlay: StateFlow<Boolean> = _showSentimentOverlay.asStateFlow()

    private val _recentlyTriggeredAlert = MutableStateFlow<PriceAlertEntity?>(null)
    val recentlyTriggeredAlert: StateFlow<PriceAlertEntity?> = _recentlyTriggeredAlert.asStateFlow()

    // Backtest
    private val _strategyParams = MutableStateFlow(StrategyParameters())
    val strategyParams: StateFlow<StrategyParameters> = _strategyParams.asStateFlow()

    private val _backtestResult = MutableStateFlow<BacktestResult?>(null)
    val backtestResult: StateFlow<BacktestResult?> = _backtestResult.asStateFlow()

    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting: StateFlow<Boolean> = _isBacktesting.asStateFlow()

    private val previousPrices = mutableMapOf<String, Double>()

    init {
        // Monitor asset ticks and evaluate alerts
        viewModelScope.launch {
            assets.collect { assetList ->
                checkPriceAlerts(assetList)
            }
        }

        // Run an initial backtest once assets are loaded
        viewModelScope.launch {
            assets.collect { assetList ->
                if (assetList.isNotEmpty() && _backtestResult.value == null) {
                    val initialAsset = assetList.first()
                    runBacktest(initialAsset, _strategyParams.value)
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectAsset(assetId: String) {
        _selectedAssetId.value = assetId
        // Automatically rerun backtest for newly selected asset
        val asset = assets.value.find { it.id == assetId }
        if (asset != null) {
            runBacktest(asset, _strategyParams.value)
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
    }

    fun toggleLiveTicking() {
        marketRepo.toggleLiveTicking()
    }

    fun manualRefresh() {
        marketRepo.triggerManualRefresh()
    }

    fun setChartDisplayType(type: ChartDisplayType) {
        _chartDisplayType.value = type
    }

    fun toggleFastEma() {
        _showFastEma.value = !_showFastEma.value
    }

    fun toggleSlowEma() {
        _showSlowEma.value = !_showSlowEma.value
    }

    fun toggleRsi() {
        _showRsi.value = !_showRsi.value
    }

    fun toggleSentimentOverlay() {
        _showSentimentOverlay.value = !_showSentimentOverlay.value
    }

    fun dismissTriggeredAlert() {
        _recentlyTriggeredAlert.value = null
    }

    fun createAlert(
        assetId: String,
        symbol: String,
        condition: AlertCondition,
        targetValue: Double,
        note: String
    ) {
        viewModelScope.launch {
            val entity = PriceAlertEntity(
                assetId = assetId,
                assetSymbol = symbol,
                condition = condition.name,
                targetValue = targetValue,
                note = note.ifBlank { "${condition.label} $targetValue" }
            )
            alertRepo.createAlert(entity)
        }
    }

    fun toggleAlert(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            alertRepo.toggleAlert(id, isEnabled)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            alertRepo.deleteAlert(id)
        }
    }

    fun clearTriggeredAlerts() {
        viewModelScope.launch {
            alertRepo.clearTriggered()
        }
    }

    fun updateStrategyParams(params: StrategyParameters) {
        _strategyParams.value = params
        val asset = selectedAsset.value ?: return
        runBacktest(asset, params)
    }

    fun runBacktest(asset: AssetMarketData, params: StrategyParameters) {
        viewModelScope.launch {
            _isBacktesting.value = true
            val candles = asset.getCandlesForTimeframe(_selectedTimeframe.value)
            val result = BacktestEngine.runBacktest(asset, candles, params)
            _backtestResult.value = result
            _isBacktesting.value = false
        }
    }

    private fun checkPriceAlerts(assetList: List<AssetMarketData>) {
        val currentAlerts = allAlerts.value.filter { it.isEnabled && !it.isTriggered }
        if (currentAlerts.isEmpty()) return

        for (asset in assetList) {
            val currentPrice = asset.currentPrice
            val prevPrice = previousPrices[asset.id] ?: currentPrice
            val matchingAlerts = currentAlerts.filter { it.assetId == asset.id }

            for (alert in matchingAlerts) {
                var triggered = false
                when (alert.condition) {
                    AlertCondition.CROSSES_ABOVE.name -> {
                        if (currentPrice >= alert.targetValue && prevPrice < alert.targetValue) {
                            triggered = true
                        }
                    }
                    AlertCondition.CROSSES_BELOW.name -> {
                        if (currentPrice <= alert.targetValue && prevPrice > alert.targetValue) {
                            triggered = true
                        }
                    }
                    AlertCondition.PCT_CHANGE_UP.name -> {
                        if (asset.changePercent24h >= alert.targetValue) {
                            triggered = true
                        }
                    }
                    AlertCondition.PCT_CHANGE_DOWN.name -> {
                        if (asset.changePercent24h <= -alert.targetValue) {
                            triggered = true
                        }
                    }
                    AlertCondition.SENTIMENT_GREED.name -> {
                        if (asset.sentimentScore >= 70) {
                            triggered = true
                        }
                    }
                    AlertCondition.SENTIMENT_FEAR.name -> {
                        if (asset.sentimentScore <= 30) {
                            triggered = true
                        }
                    }
                }

                if (triggered) {
                    triggerAlert(alert, currentPrice)
                }
            }

            previousPrices[asset.id] = currentPrice
        }
    }

    private fun triggerAlert(alert: PriceAlertEntity, currentPrice: Double) {
        viewModelScope.launch {
            alertRepo.markTriggered(alert.id, currentPrice)
            _recentlyTriggeredAlert.value = alert.copy(
                isTriggered = true,
                triggeredPrice = currentPrice,
                triggeredAt = System.currentTimeMillis()
            )
            vibrateDevice()
        }
    }

    private fun vibrateDevice() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 150), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration service is restricted or unavailable
        }
    }
}
