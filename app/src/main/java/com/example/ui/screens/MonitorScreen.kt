package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CardProfileEntity
import com.example.data.PaymentCardEntity
import com.example.data.TelemetryLogEntity
import com.example.data.WorkerJobEntity
import com.example.network.OrchestratedCardStatusSummaryDto
import com.example.ui.UiState
import com.example.ui.components.PlaywrightLiveView
import com.example.ui.theme.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun MonitorScreen(
    workerJobs: List<WorkerJobEntity>,
    profiles: List<CardProfileEntity>,
    cards: List<PaymentCardEntity>,
    recentLogs: List<TelemetryLogEntity>,
    isDeploying: Boolean,
    backendCardStatusState: UiState<OrchestratedCardStatusSummaryDto> = UiState.Idle,
    onSyncBackendStatus: () -> Unit = {},
    onDeployNewManifest: () -> Unit,
    onJobClick: (WorkerJobEntity) -> Unit,
    onClearJobs: () -> Unit,
    onRunPipelineForProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Deployment Metrics Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM TELEMETRY ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Active Workers: ${workerJobs.size} | Profiles: ${profiles.size}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onSyncBackendStatus,
                            modifier = Modifier
                                .background(HighDensitySurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, HighDensityBorder, RoundedCornerShape(8.dp))
                                .testTag("sync_backend_status_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Retrofit Status",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = onDeployNewManifest,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                            enabled = !isDeploying,
                            modifier = Modifier.testTag("monitor_deploy_button")
                        ) {
                            if (isDeploying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimaryDark, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DEPLOYING...", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DISPATCH ALL", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val totalExtracted = cards.sumOf { it.balance }
                val activatedCount = cards.count { it.isActivated || it.balance > 0.0 }
                val pendingCount = cards.count { !it.isActivated && it.balance <= 0.0 }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(title = "PROFILES", value = "${profiles.size}", accentColor = PrimaryCyan)
                    MetricBox(title = "ACTIVATED", value = "$activatedCount", accentColor = SecondaryEmerald)
                    MetricBox(title = "PENDING 5M", value = "$pendingCount", accentColor = AccentAmber)
                    MetricBox(title = "EXTRACTED BAL", value = "$${String.format("%.2f", totalExtracted)}", accentColor = SecondaryEmerald)
                }

                // Retrofit Backend StateFlow Sync Status Indicator
                when (backendCardStatusState) {
                    is UiState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PrimaryCyan, strokeWidth = 1.5.dp)
                            Text(
                                text = backendCardStatusState.message,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondary)
                            )
                        }
                    }
                    is UiState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        val summary = backendCardStatusState.data
                        Text(
                            text = "RETROFIT SERVICE STATEFLOW: ${summary.activatedCount}/${summary.totalCards} cards active | Total Bal: $${String.format("%.2f", summary.totalOrchestratedBalance)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = SecondaryEmerald, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    is UiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "RETROFIT SERVICE STATUS: ${backendCardStatusState.message}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = ErrorRed)
                        )
                    }
                    UiState.Idle -> {}
                }
            }
        }

        var selectedJobId by remember { mutableStateOf<String?>(null) }
        val activeOrSelectedJob = workerJobs.find { it.jobId == selectedJobId } ?: workerJobs.firstOrNull()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE WORKER JOBS (${workerJobs.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
            )

            if (workerJobs.isNotEmpty()) {
                TextButton(
                    onClick = onClearJobs,
                    modifier = Modifier.testTag("monitor_clear_jobs_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLEAR JOBS", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                }
            }
        }

        if (workerJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(HighDensitySurface, RoundedCornerShape(8.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Active Worker Jobs in Pipeline",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontFamily = FontFamily.Monospace)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDeployNewManifest) {
                        Text("CLICK HERE TO DISPATCH WORKERS", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Playwright Live Runner View beside active tasks
                activeOrSelectedJob?.let { liveJob ->
                    PlaywrightLiveView(
                        job = liveJob,
                        onRetryJob = { onJobClick(liveJob) }
                    )
                }

                Text(
                    text = "DISPATCHED WORKER QUEUE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                workerJobs.forEach { job ->
                    WorkerJobCard(
                        job = job,
                        onClick = {
                            selectedJobId = job.jobId
                            onJobClick(job)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBox(title: String, value: String, accentColor: Color) {
    Column(
        modifier = Modifier
            .background(HighDensitySurfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        )
    }
}

@Composable
fun WorkerJobCard(
    job: WorkerJobEntity,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("worker_job_card_${job.jobId}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (job.status) {
                        "COMPLETED" -> SecondaryEmerald
                        "SCANNING", "TRANSMITTING" -> AccentAmber
                        else -> PrimaryCyan
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = job.jobId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = job.status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when (job.status) {
                            "COMPLETED" -> SecondaryEmerald
                            "SCANNING", "TRANSMITTING" -> AccentAmber
                            else -> TextSecondary
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = job.currentStep,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { job.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (job.status == "COMPLETED") SecondaryEmerald else PrimaryCyan,
                trackColor = HighDensitySurfaceVariant,
            )

            job.terminalOutputValue?.let { valAmt ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "EXTRACTED: $$valAmt",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald
                        )
                    )
                }
            }
        }
    }
}
