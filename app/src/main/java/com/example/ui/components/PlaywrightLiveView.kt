package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkerJobEntity
import com.example.ui.theme.*

@Composable
fun PlaywrightLiveView(
    job: WorkerJobEntity,
    onRetryJob: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Live DOM, 1: Console, 2: Playwright Node.js, 3: Python Script

    Card(
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
            .testTag("playwright_live_view_card_${job.jobId}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Simulated Browser Window Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityBackground, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "REAL DOM & WEB INSPECTOR ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            fontSize = 10.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (job.status) {
                        "COMPLETED" -> SecondaryEmerald
                        "SCANNING", "TRANSMITTING" -> AccentAmber
                        else -> TextMuted
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 10.sp
                        )
                    )
                    if (onClose != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close View", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Browser URL Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = "HTTPS Secure", tint = SecondaryEmerald, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = job.jobId,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "|",
                    style = MaterialTheme.typography.labelSmall.copy(color = HighDensityBorder, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = job.currentStep,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }

            LinearProgressIndicator(
                progress = { job.progressPercentage },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (job.status == "COMPLETED") SecondaryEmerald else PrimaryCyan,
                trackColor = HighDensityBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // View Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(title = "LIVE DOM", icon = Icons.Default.Language, isSelected = activeTab == 0) { activeTab = 0 }
                TabButton(title = "CONSOLE", icon = Icons.Default.Terminal, isSelected = activeTab == 1) { activeTab = 1 }
                TabButton(title = "NODE.JS", icon = Icons.Default.Code, isSelected = activeTab == 2) { activeTab = 2 }
                TabButton(title = "PYTHON", icon = Icons.Default.DataObject, isSelected = activeTab == 3) { activeTab = 3 }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Body according to selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(HighDensityBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                when (activeTab) {
                    0 -> LiveDomTabContent(job = job)
                    1 -> ConsoleTabContent(job = job)
                    2 -> ScriptTabContent(script = job.generatedPlaywrightScript ?: "// Playwright script pending generation...")
                    3 -> ScriptTabContent(script = generatePythonPlaywrightScriptSample(job))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEURISTIC: ${job.currentHeuristic}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onRetryJob,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RE-EXECUTE", style = MaterialTheme.typography.labelSmall.copy(color = OnPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) PrimaryCyan.copy(alpha = 0.15f) else HighDensitySurfaceVariant,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryCyan else HighDensityBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) PrimaryCyan else TextMuted, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PrimaryCyan else TextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun LiveDomTabContent(job: WorkerJobEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME DOM INSPECTOR FRAME",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontSize = 10.sp)
            )
            Text(
                text = "STATUS: ${job.status}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (job.status == "COMPLETED") SecondaryEmerald else AccentAmber,
                    fontSize = 10.sp
                )
            )
        }

        job.terminalOutputValue?.let { balance ->
            Surface(
                color = SecondaryEmerald.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("EXTRACTED LIVE DOM BALANCE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                        Text("$$balance", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, color = SecondaryEmerald, fontWeight = FontWeight.Bold))
                    }
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(24.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HighDensitySurfaceVariant, RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("CURRENT EXECUTION PIPELINE:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 9.sp))
            Text("• Step: ${job.currentStep}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
            Text("• Protection Profile: ${job.currentHeuristic}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
            job.currentTargetedField?.let {
                Text("• Targeted Selector: $it", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryCyan, fontSize = 11.sp))
            }
            job.currentPopulatedField?.let {
                Text("• Field Filling: $it", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryEmerald, fontSize = 11.sp))
            }
            Text("• Updated At: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.CANADA).format(java.util.Date(job.updatedAt))}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 10.sp))
        }
    }
}

@Composable
private fun ConsoleTabContent(job: WorkerJobEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val realLogs = listOf(
            "[HTTP DISPATCH] Target URL: ${job.targetPortalUrl}",
            "[DOM INSPECTOR] Current Step: ${job.currentStep}",
            "[SECURITY PROFILE] ${job.currentHeuristic}",
            job.currentTargetedField?.let { "[TARGET FIELD] Discovered selector: $it" },
            job.currentPopulatedField?.let { "[FIELD FILLING] Value set: $it" },
            job.errorMessage?.let { "[ERROR] $it" },
            if (job.terminalOutputValue != null) "[EXTRACTED BALANCE] $${job.terminalOutputValue}" else "[STATUS] ${job.status}"
        ).filterNotNull()

        realLogs.forEach { logLine ->
            Text(
                text = logLine,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        logLine.contains("EXTRACTED BALANCE") -> SecondaryEmerald
                        logLine.contains("ERROR") -> Color(0xFFFF5F56)
                        logLine.contains("FIELD") -> PrimaryCyan
                        else -> TextSecondary
                    },
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ScriptTabContent(script: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = script,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = SecondaryEmerald,
                fontSize = 10.sp
            )
        )
    }
}

private fun generatePythonPlaywrightScriptSample(job: WorkerJobEntity): String {
    return """
    # Orchestrator Pro - Python Async Playwright Script
    # Worker Job ID: ${job.jobId}
    import asyncio
    from playwright.async_api import async_playwright

    async def run():
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            context = await browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
            )
            page = await context.new_page()
            print("[PLAYWRIGHT PYTHON] Navigating to ${job.targetPortalUrl}...")
            await page.goto("${job.targetPortalUrl}", wait_until="networkidle")
            
            # Fill card credentials
            await page.fill("input[name*='card'], input[id*='card']", "****")
            await page.click("button[type='submit'], input[type='submit'], .submit-btn")
            
            print("[PLAYWRIGHT PYTHON] Navigation & heuristic field filling complete")
            await browser.close()

    asyncio.run(run())
    """.trimIndent()
}
