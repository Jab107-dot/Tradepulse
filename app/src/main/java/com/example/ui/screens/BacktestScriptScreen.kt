package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BacktestResult
import com.example.data.model.BacktestTrade
import com.example.data.model.StrategyParameters
import com.example.data.model.StrategyType
import com.example.ui.MainViewModel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.util.Locale

enum class CodeFormat {
    PINE_SCRIPT,
    PYTHON
}

@Composable
fun BacktestScriptScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val selectedAssetId by viewModel.selectedAssetId.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val strategyParams by viewModel.strategyParams.collectAsStateWithLifecycle()
    val backtestResult by viewModel.backtestResult.collectAsStateWithLifecycle()
    val isBacktesting by viewModel.isBacktesting.collectAsStateWithLifecycle()

    var activeCodeFormat by remember { mutableStateOf(CodeFormat.PINE_SCRIPT) }
    var showParamsEditor by remember { mutableStateOf(false) }

    val selectedAssetIndex = assets.indexOfFirst { it.id == selectedAssetId }.coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("backtest_script_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Strategy Backtesting & Code Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Test sentiment & market trends historically, then export directly to Pine Script or Python",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Asset Selector Row
        item {
            if (assets.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedAssetIndex,
                    containerColor = TerminalSurface,
                    contentColor = CryptoCyan,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        if (selectedAssetIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedAssetIndex]),
                                color = CryptoCyan
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier.testTag("backtest_asset_tabs")
                ) {
                    assets.forEachIndexed { index, asset ->
                        Tab(
                            selected = index == selectedAssetIndex,
                            onClick = { viewModel.selectAsset(asset.id) },
                            text = { Text(asset.symbol, fontSize = 12.sp, fontWeight = if (index == selectedAssetIndex) FontWeight.Bold else FontWeight.Medium) },
                            modifier = Modifier.testTag("backtest_tab_${asset.id}")
                        )
                    }
                }
            }
        }

        // Strategy Selector Chips
        item {
            Column {
                Text(
                    "Select Strategy Model",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StrategyType.values().forEach { st ->
                        val isSelected = st == strategyParams.strategyType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateStrategyParams(
                                    strategyParams.copy(
                                        strategyType = st,
                                        stopLossPct = st.defaultStopLossPct,
                                        takeProfitPct = st.defaultTakeProfitPct
                                    )
                                )
                            },
                            label = { Text(st.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CryptoCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = TerminalSurface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("strategy_chip_${st.name}")
                        )
                    }
                }
            }
        }

        // Strategy Info & Parameters Toggle
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            strategyParams.strategyType.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedButton(
                            onClick = { showParamsEditor = !showParamsEditor },
                            modifier = Modifier.testTag("toggle_params_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showParamsEditor) "Hide Tuner" else "Tune Params", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        strategyParams.strategyType.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    // Expandable Parameter Tuner
                    AnimatedVisibility(visible = showParamsEditor) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Stop Loss Slider
                            Text("Stop Loss: ${String.format(Locale.US, "%.1f", strategyParams.stopLossPct)}%", fontSize = 11.sp, color = BearishRed)
                            Slider(
                                value = strategyParams.stopLossPct.toFloat(),
                                onValueChange = {
                                    viewModel.updateStrategyParams(strategyParams.copy(stopLossPct = it.toDouble()))
                                },
                                valueRange = 0.5f..10.0f,
                                steps = 19,
                                colors = SliderDefaults.colors(thumbColor = BearishRed, activeTrackColor = BearishRed)
                            )

                            // Take Profit Slider
                            Text("Take Profit: ${String.format(Locale.US, "%.1f", strategyParams.takeProfitPct)}%", fontSize = 11.sp, color = BullishGreen)
                            Slider(
                                value = strategyParams.takeProfitPct.toFloat(),
                                onValueChange = {
                                    viewModel.updateStrategyParams(strategyParams.copy(takeProfitPct = it.toDouble()))
                                },
                                valueRange = 1.0f..20.0f,
                                steps = 19,
                                colors = SliderDefaults.colors(thumbColor = BullishGreen, activeTrackColor = BullishGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedAsset?.let { viewModel.runBacktest(it, strategyParams) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_backtest_btn")
                    ) {
                        if (isBacktesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Computing Historical Backtest...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Backtest on ${selectedAsset?.symbol ?: "Asset"}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Backtest Performance Dashboard
        backtestResult?.let { result ->
            item {
                BacktestPerformanceCard(result = result)
            }

            // Interactive Equity Curve Canvas
            item {
                EquityCurveCard(result = result)
            }

            // Trade Log Table
            if (result.trades.isNotEmpty()) {
                item {
                    TradeLogCard(trades = result.trades)
                }
            }

            // Pine Script & Python Code Generator Studio
            item {
                CodeStudioCard(
                    result = result,
                    activeFormat = activeCodeFormat,
                    onSelectFormat = { activeCodeFormat = it },
                    onCopyCode = { code, langName ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("$langName Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "$langName copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun BacktestPerformanceCard(result: BacktestResult) {
    val isPositive = result.totalReturnPct >= 0
    val stratColor = if (isPositive) BullishGreen else BearishRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backtest_metrics_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Backtest Performance Metrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    result.assetSymbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CryptoCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Return Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(stratColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, stratColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Strategy Return", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${if (isPositive) "+" else ""}${result.totalReturnPct}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = stratColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Buy & Hold (Bench)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${if (result.benchmarkReturnPct >= 0) "+" else ""}${result.benchmarkReturnPct}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (result.benchmarkReturnPct >= 0) BullishGreen else BearishRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniBox("Win Rate", "${result.winRatePct}%", if (result.winRatePct >= 50) BullishGreen else BearishRed, Modifier.weight(1f))
                StatMiniBox("Profit Factor", "${result.profitFactor}", if (result.profitFactor >= 1.5) BullishGreen else GoldAccent, Modifier.weight(1f))
                StatMiniBox("Max Drawdown", "-${result.maxDrawdownPct}%", BearishRed, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniBox("Total Trades", "${result.totalTrades}", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                StatMiniBox("Win / Loss", "${result.winningTrades}W / ${result.losingTrades}L", BullishGreen, Modifier.weight(1f))
                StatMiniBox("Sharpe Ratio", "${result.sharpeRatio}", CryptoCyan, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatMiniBox(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun EquityCurveCard(result: BacktestResult) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equity_curve_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Backtest Equity Growth Curve",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(CryptoCyan, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Portfolio ($)", fontSize = 10.sp, color = CryptoCyan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                EquityCurveCanvas(equityPoints = result.equityCurve)
            }
        }
    }
}

@Composable
private fun EquityCurveCanvas(equityPoints: List<Pair<Long, Double>>) {
    if (equityPoints.size < 2) return

    val minEquity = equityPoints.minOf { it.second }
    val maxEquity = equityPoints.maxOf { it.second }
    val range = (maxEquity - minEquity).coerceAtLeast(1.0)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = width / (equityPoints.size - 1).toFloat()

        // Draw baseline
        val baselineY = height * (1f - ((equityPoints.first().second - minEquity) / range).toFloat())
        drawLine(
            color = Color(0x3394A3B8),
            start = Offset(0f, baselineY),
            end = Offset(width, baselineY),
            strokeWidth = 1f
        )

        val path = Path()
        equityPoints.forEachIndexed { i, pt ->
            val x = i * step
            val y = (height * (1f - ((pt.second - minEquity) / range).toFloat())).coerceIn(4f, height - 4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = CryptoCyan,
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun TradeLogCard(trades: List<BacktestTrade>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade_log_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Execution Trade Log (${trades.size} completed)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            trades.take(6).forEach { trade ->
                val isWin = trade.pnlPercent > 0
                val color = if (isWin) BullishGreen else BearishRed

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                trade.type,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (trade.type == "LONG") BullishGreen else BearishRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${trade.entryDate} -> ${trade.exitDate}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            trade.exitReason,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${if (isWin) "+" else ""}${trade.pnlPercent}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = color
                        )
                        Text(
                            "${if (isWin) "+" else ""}$${trade.pnlDollar}",
                            fontSize = 10.sp,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeStudioCard(
    result: BacktestResult,
    activeFormat: CodeFormat,
    onSelectFormat: (CodeFormat) -> Unit,
    onCopyCode: (code: String, lang: String) -> Unit
) {
    val codeToDisplay = if (activeFormat == CodeFormat.PINE_SCRIPT) result.pineScriptCode else result.pythonCode
    val langLabel = if (activeFormat == CodeFormat.PINE_SCRIPT) "Pine Script (v5)" else "Python (Pandas)"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("code_studio_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Copy Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Algorithmic Indicator & Strategy Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Export ready-to-run indicators to TradingView or Python",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { onCopyCode(codeToDisplay, langLabel) },
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("copy_code_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Code Format Switcher Tabs
            TabRow(
                selectedTabIndex = if (activeFormat == CodeFormat.PINE_SCRIPT) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = CryptoCyan,
                indicator = { tabPositions ->
                    val idx = if (activeFormat == CodeFormat.PINE_SCRIPT) 0 else 1
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color = CryptoCyan
                    )
                }
            ) {
                Tab(
                    selected = activeFormat == CodeFormat.PINE_SCRIPT,
                    onClick = { onSelectFormat(CodeFormat.PINE_SCRIPT) },
                    text = { Text("Pine Script v5 (TradingView)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_pinescript")
                )
                Tab(
                    selected = activeFormat == CodeFormat.PYTHON,
                    onClick = { onSelectFormat(CodeFormat.PYTHON) },
                    text = { Text("Python (Pandas / Backtrader)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_python")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Terminal Code Box with Monospace Typography
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(TerminalBackground, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = codeToDisplay,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Usage Guide Tip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (activeFormat == CodeFormat.PINE_SCRIPT) {
                        "Paste directly into TradingView's bottom Pine Editor tab and click 'Add to chart'."
                    } else {
                        "Run in Jupyter Notebook or Google Colab with `pip install yfinance pandas matplotlib`."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
