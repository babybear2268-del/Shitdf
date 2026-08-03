package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.OrchestratorConfigEntity
import com.example.ui.theme.*

@Composable
fun ConfigScreen(
    currentConfig: OrchestratorConfigEntity,
    onSaveConfig: (OrchestratorConfigEntity) -> Unit,
    onTestWebhook: (webhookUrl: String, callback: (Boolean) -> Unit) -> Unit
) {
    var webhookUrl by remember(currentConfig) { mutableStateOf(currentConfig.discordWebhookUrl) }
    var maxParallel by remember(currentConfig) { mutableStateOf(currentConfig.maxParallelWorkers.toString()) }
    var headless by remember(currentConfig) { mutableStateOf(currentConfig.headlessMode) }
    var randomizeUa by remember(currentConfig) { mutableStateOf(currentConfig.randomizeUserAgent) }
    var userAgent by remember(currentConfig) { mutableStateOf(currentConfig.uAStringOverride) }
    var timeoutMs by remember(currentConfig) { mutableStateOf(currentConfig.heuristicTimeoutMs.toString()) }
    var autoCheckEnabled by remember(currentConfig) { mutableStateOf(currentConfig.autoCheckEnabled) }
    var autoCheckInterval by remember(currentConfig) { mutableStateOf(currentConfig.autoCheckIntervalMinutes.toString()) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingWebhook by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ORCHESTRATOR SYSTEM CONFIG",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DISCORD / TELEMETRY WEBHOOK INTEGRATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted)
                )

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Discord Webhook URL", color = TextSecondary) },
                    placeholder = { Text("https://discord.com/api/webhooks/...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("config_webhook_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    testStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (msg.contains("Success")) SecondaryEmerald else ErrorRed
                            )
                        )
                    } ?: Spacer(modifier = Modifier.width(1.dp))

                    OutlinedButton(
                        onClick = {
                            isTestingWebhook = true
                            testStatusMessage = "Testing Webhook Connection..."
                            onTestWebhook(webhookUrl) { success ->
                                isTestingWebhook = false
                                testStatusMessage = if (success) "Success: Webhook Delivered!" else "Failed: Unable to post to Webhook"
                            }
                        },
                        enabled = webhookUrl.isNotBlank() && !isTestingWebhook,
                        modifier = Modifier.testTag("config_test_webhook_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST WEBHOOK", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary))
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PLAYWRIGHT & HEURISTIC ENGINE SETTINGS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Headless Execution Mode", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                    Switch(
                        checked = headless,
                        onCheckedChange = { headless = it },
                        modifier = Modifier.testTag("config_headless_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Randomize User-Agent Header", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                    Switch(
                        checked = randomizeUa,
                        onCheckedChange = { randomizeUa = it },
                        modifier = Modifier.testTag("config_random_ua_switch")
                    )
                }

                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text("Default User-Agent Override", color = TextSecondary) },
                    enabled = !randomizeUa,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = maxParallel,
                        onValueChange = { maxParallel = it },
                        label = { Text("Max Parallel Workers", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeoutMs,
                        onValueChange = { timeoutMs = it },
                        label = { Text("Timeout (ms)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "AUTOMATED PERIODIC BALANCE CHECKER",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Check Unactivated Cards", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                        Text("Continuously polls portal URLs until card is activated", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { autoCheckEnabled = it },
                        modifier = Modifier.testTag("config_autocheck_switch")
                    )
                }

                OutlinedTextField(
                    value = autoCheckInterval,
                    onValueChange = { autoCheckInterval = it },
                    label = { Text("Check Interval (Minutes)", color = TextSecondary) },
                    enabled = autoCheckEnabled,
                    modifier = Modifier.fillMaxWidth().testTag("config_autocheck_interval_input")
                )
            }
        }

        Button(
            onClick = {
                val updated = OrchestratorConfigEntity(
                    id = 1,
                    discordWebhookUrl = webhookUrl.trim(),
                    maxParallelWorkers = maxParallel.toIntOrNull() ?: 4,
                    headlessMode = headless,
                    randomizeUserAgent = randomizeUa,
                    uAStringOverride = userAgent.trim(),
                    heuristicTimeoutMs = timeoutMs.toLongOrNull() ?: 45000L,
                    autoCheckIntervalMinutes = autoCheckInterval.toIntOrNull() ?: 5,
                    autoCheckEnabled = autoCheckEnabled
                )
                onSaveConfig(updated)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            modifier = Modifier.fillMaxWidth().testTag("config_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE CONFIGURATION", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}
