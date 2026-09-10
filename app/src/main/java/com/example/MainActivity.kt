package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AlertCondition
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AlertNotificationBanner
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BacktestScriptScreen
import com.example.ui.screens.ChartDetailScreen
import com.example.ui.screens.CreateAlertDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SentimentScreen
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TradePulseApp()
            }
        }
    }
}

@Composable
fun TradePulseApp(
    viewModel: MainViewModel = run {
        val application = LocalContext.current.applicationContext as Application
        viewModel(factory = MainViewModel.provideFactory(application))
    }
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val isLiveTicking by viewModel.isLiveTicking.collectAsStateWithLifecycle()
    val recentlyTriggeredAlert by viewModel.recentlyTriggeredAlert.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val assets by viewModel.assets.collectAsStateWithLifecycle()

    val activeAlertCount = remember(allAlerts) { allAlerts.count { it.isEnabled && !it.isTriggered } }
    var showDirectAlertModal by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tradepulse_scaffold"),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TradePulseTopBar(
                selectedAssetSymbol = selectedAsset?.symbol ?: "XAUUSD",
                selectedAssetPrice = selectedAsset?.currentPrice ?: 0.0,
                selectedAssetChangePct = selectedAsset?.changePercent24h ?: 0.0,
                isLive = isLiveTicking
            )
        },
        bottomBar = {
            TradePulseBottomBar(
                currentTab = currentTab,
                onSelectTab = { viewModel.selectTab(it) },
                activeAlertCount = activeAlertCount
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Floating Notification Banner for Triggered Real-time Alerts
            AlertNotificationBanner(
                triggeredAlert = recentlyTriggeredAlert,
                onDismiss = { viewModel.dismissTriggeredAlert() },
                onViewAlerts = {
                    viewModel.dismissTriggeredAlert()
                    viewModel.selectTab(AppTab.ALERTS)
                }
            )

            // Screen Content Switching
            Crossfade(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onOpenAlertModal = { showDirectAlertModal = true }
                    )
                    AppTab.CHART -> ChartDetailScreen(
                        viewModel = viewModel
                    )
                    AppTab.SENTIMENT -> SentimentScreen(
                        viewModel = viewModel
                    )
                    AppTab.ALERTS -> AlertsScreen(
                        viewModel = viewModel
                    )
                    AppTab.BACKTEST -> BacktestScriptScreen(
                        viewModel = viewModel
                    )
                }
            }
        }

        if (showDirectAlertModal) {
            CreateAlertDialog(
                assets = assets,
                defaultAsset = selectedAsset ?: assets.firstOrNull(),
                onDismiss = { showDirectAlertModal = false },
                onConfirm = { assetId, symbol, condition, targetValue, note ->
                    viewModel.createAlert(assetId, symbol, condition, targetValue, note)
                    showDirectAlertModal = false
                }
            )
        }
    }
}

@Composable
fun TradePulseTopBar(
    selectedAssetSymbol: String,
    selectedAssetPrice: Double,
    selectedAssetChangePct: Double,
    isLive: Boolean
) {
    val isPositive = selectedAssetChangePct >= 0
    val changeColor = if (isPositive) BullishGreen else BearishRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_top_bar"),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, TerminalSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CryptoCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, CryptoCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "TradePulse Logo",
                        tint = CryptoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "TradePulse",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "MARKET TERMINAL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = CryptoCyan,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Real-time Mini Ticker Pill for Selected Asset
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isLive) BullishGreen else BearishRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    selectedAssetSymbol,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    formatPrice(selectedAssetPrice),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${if (isPositive) "+" else ""}${selectedAssetChangePct}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }
        }
    }
}

@Composable
fun TradePulseBottomBar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    activeAlertCount: Int
) {
    NavigationBar(
        modifier = Modifier.testTag("app_bottom_nav_bar"),
        containerColor = TerminalSurface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        // Tab 1: Dashboard
        NavigationBarItem(
            selected = currentTab == AppTab.DASHBOARD,
            onClick = { onSelectTab(AppTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CryptoCyan,
                indicatorColor = CryptoCyan,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )

        // Tab 2: Chart & Viz
        NavigationBarItem(
            selected = currentTab == AppTab.CHART,
            onClick = { onSelectTab(AppTab.CHART) },
            icon = { Icon(Icons.Default.ShowChart, contentDescription = "Charts") },
            label = { Text("Charts", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CryptoCyan,
                indicatorColor = CryptoCyan,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_chart")
        )

        // Tab 3: Sentiment
        NavigationBarItem(
            selected = currentTab == AppTab.SENTIMENT,
            onClick = { onSelectTab(AppTab.SENTIMENT) },
            icon = { Icon(Icons.Default.Speed, contentDescription = "Sentiment") },
            label = { Text("Sentiment", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CryptoCyan,
                indicatorColor = CryptoCyan,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_sentiment")
        )

        // Tab 4: Price Alerts
        NavigationBarItem(
            selected = currentTab == AppTab.ALERTS,
            onClick = { onSelectTab(AppTab.ALERTS) },
            icon = {
                if (activeAlertCount > 0) {
                    BadgedBox(badge = {
                        Badge(containerColor = CryptoCyan, contentColor = Color.Black) {
                            Text("$activeAlertCount")
                        }
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts")
                    }
                } else {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts")
                }
            },
            label = { Text("Alerts", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CryptoCyan,
                indicatorColor = CryptoCyan,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_alerts")
        )

        // Tab 5: Backtest & Code
        NavigationBarItem(
            selected = currentTab == AppTab.BACKTEST,
            onClick = { onSelectTab(AppTab.BACKTEST) },
            icon = { Icon(Icons.Default.Code, contentDescription = "Backtest") },
            label = { Text("Backtest", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CryptoCyan,
                indicatorColor = CryptoCyan,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_backtest")
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000.0) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
