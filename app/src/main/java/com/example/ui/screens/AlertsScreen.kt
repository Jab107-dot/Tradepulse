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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AlertCondition
import com.example.data.local.PriceAlertEntity
import com.example.data.model.AssetMarketData
import com.example.ui.MainViewModel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    showCreateModalOnOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(showCreateModalOnOpen) }

    val activeAlerts = remember(allAlerts) { allAlerts.filter { !it.isTriggered } }
    val triggeredAlerts = remember(allAlerts) { allAlerts.filter { it.isTriggered } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .testTag("alerts_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header
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
                            "Real-Time Price Alerts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${activeAlerts.size} active alerts monitoring live tick quotes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_new_alert_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Active Alerts Section
            item {
                Text(
                    "Active Price Monitors (${activeAlerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (activeAlerts.isEmpty()) {
                item {
                    EmptyAlertsCard(onCreateClick = { showCreateDialog = true })
                }
            } else {
                items(activeAlerts, key = { it.id }) { alert ->
                    val asset = assets.find { it.id == alert.assetId }
                    ActiveAlertCard(
                        alert = alert,
                        currentPrice = asset?.currentPrice ?: 0.0,
                        onToggle = { isEnabled -> viewModel.toggleAlert(alert.id, isEnabled) },
                        onDelete = { viewModel.deleteAlert(alert.id) }
                    )
                }
            }

            // Triggered Alerts History Section
            if (triggeredAlerts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Trigger History (${triggeredAlerts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { viewModel.clearTriggeredAlerts() },
                            modifier = Modifier.testTag("clear_history_btn")
                        ) {
                            Text("Clear", color = BearishRed, fontSize = 12.sp)
                        }
                    }
                }

                items(triggeredAlerts, key = { it.id }) { alert ->
                    TriggeredAlertCard(alert = alert, onDelete = { viewModel.deleteAlert(alert.id) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to add alert quickly
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = CryptoCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_create_alert")
        ) {
            Icon(Icons.Default.AddAlert, contentDescription = "Add Price Alert")
        }
    }

    if (showCreateDialog) {
        CreateAlertDialog(
            assets = assets,
            defaultAsset = selectedAsset ?: assets.firstOrNull(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { assetId, symbol, condition, targetValue, note ->
                viewModel.createAlert(assetId, symbol, condition, targetValue, note)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ActiveAlertCard(
    alert: PriceAlertEntity,
    currentPrice: Double,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val conditionEnum = AlertCondition.values().find { it.name == alert.condition } ?: AlertCondition.CROSSES_ABOVE
    val diff = if (currentPrice > 0.0) ((alert.targetValue - currentPrice) / currentPrice) * 100.0 else 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_alert_card_${alert.id}"),
        shape = RoundedCornerShape(14.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CryptoCyan.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = CryptoCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alert.assetSymbol,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            conditionEnum.label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Target: ${formatPrice(alert.targetValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CryptoCyan
                    )
                    if (currentPrice > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${if (diff >= 0) "+" else ""}${String.format(Locale.US, "%.2f", diff)}% away)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (alert.note.isNotBlank()) {
                    Text(
                        alert.note,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Switch(
                checked = alert.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = CryptoCyan,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("alert_toggle_${alert.id}")
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_alert_${alert.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Alert",
                    tint = BearishRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TriggeredAlertCard(
    alert: PriceAlertEntity,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    val triggeredTime = alert.triggeredAt?.let { timeFormat.format(Date(it)) } ?: "Recently"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("triggered_alert_card_${alert.id}"),
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${alert.assetSymbol} Triggered at ${formatPrice(alert.triggeredPrice ?: alert.targetValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        triggeredTime,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (alert.note.isNotBlank()) {
                    Text(
                        alert.note,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete History Item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAlertsCard(onCreateClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_alerts_card"),
        shape = RoundedCornerShape(14.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "No Active Price Alerts",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Set triggers for Gold, Bitcoin, or Equity Indices to get notified when prices cross key levels.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onCreateClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CryptoCyan)
            ) {
                Text("Set First Alert")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlertDialog(
    assets: List<AssetMarketData>,
    defaultAsset: AssetMarketData?,
    onDismiss: () -> Unit,
    onConfirm: (assetId: String, symbol: String, condition: AlertCondition, targetValue: Double, note: String) -> Unit
) {
    var selectedAsset by remember { mutableStateOf(defaultAsset ?: assets.firstOrNull()) }
    var selectedCondition by remember { mutableStateOf(AlertCondition.CROSSES_ABOVE) }
    var targetText by remember {
        mutableStateOf(
            selectedAsset?.let {
                val p = it.currentPrice * 1.01
                String.format(Locale.US, "%.2f", p)
            } ?: ""
        )
    }
    var noteText by remember { mutableStateOf("") }
    var assetExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Price Alert",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_alert_form"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Asset Selector
                ExposedDropdownMenuBox(
                    expanded = assetExpanded,
                    onExpandedChange = { assetExpanded = !assetExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAsset?.symbol ?: "Select Asset",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Asset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CryptoCyan,
                            unfocusedBorderColor = TerminalSurfaceBorder
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = assetExpanded,
                        onDismissRequest = { assetExpanded = false }
                    ) {
                        assets.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text("${asset.symbol} - ${asset.name}") },
                                onClick = {
                                    selectedAsset = asset
                                    targetText = String.format(Locale.US, "%.2f", asset.currentPrice * 1.01)
                                    assetExpanded = false
                                }
                            )
                        }
                    }
                }

                // Condition Selector
                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = !conditionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCondition.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trigger Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CryptoCyan,
                            unfocusedBorderColor = TerminalSurfaceBorder
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        AlertCondition.values().forEach { cond ->
                            DropdownMenuItem(
                                text = { Text(cond.label) },
                                onClick = {
                                    selectedCondition = cond
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Price Input
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Price / Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoCyan,
                        unfocusedBorderColor = TerminalSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_target_input")
                )

                // Quick Offset Buttons (+1%, +2%, -1%, -2%)
                selectedAsset?.let { asset ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-2.0, -1.0, 1.0, 2.0).forEach { pct ->
                            val calculated = asset.currentPrice * (1.0 + pct / 100.0)
                            Button(
                                onClick = {
                                    targetText = String.format(Locale.US, "%.2f", calculated)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (pct > 0) BullishGreen else BearishRed
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${if (pct > 0) "+" else ""}${pct.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Custom Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Custom Note (Optional)") },
                    placeholder = { Text("e.g. Take profit on long position") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoCyan,
                        unfocusedBorderColor = TerminalSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val asset = selectedAsset ?: return@Button
                    val targetVal = targetText.toDoubleOrNull() ?: asset.currentPrice
                    onConfirm(asset.id, asset.symbol, selectedCondition, targetVal, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CryptoCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("alert_confirm_btn")
            ) {
                Text("Set Alert", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = TerminalSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000.0) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
