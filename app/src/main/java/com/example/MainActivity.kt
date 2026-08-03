package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CardProfileEntity
import com.example.ui.OrchestratorViewModel
import com.example.ui.components.AddCardToProfileDialog
import com.example.ui.components.AddEditProfileDialog
import com.example.ui.components.HighDensityNavBar
import com.example.ui.components.JobDetailsDialog
import com.example.ui.components.JsonManifestDialog
import com.example.ui.components.PhotoCardScanDialog
import com.example.ui.components.SystemHeader
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: OrchestratorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                OrchestratorApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OrchestratorApp(viewModel: OrchestratorViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val workerJobs by viewModel.workerJobs.collectAsStateWithLifecycle()
    val telemetryLogs by viewModel.telemetryLogs.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val selectedJobDetails by viewModel.selectedJobDetails.collectAsStateWithLifecycle()
    val isDeploying by viewModel.isDeploying.collectAsStateWithLifecycle()
    val manifestJsonState by viewModel.manifestJsonState.collectAsStateWithLifecycle()
    val isPerformingOperation by viewModel.isPerformingOperation.collectAsStateWithLifecycle()
    val operationMessage by viewModel.operationMessage.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val isCheckingUnactivated by viewModel.isCheckingUnactivated.collectAsStateWithLifecycle()
    val backendCardStatusState by viewModel.backendCardStatusState.collectAsStateWithLifecycle()

    var showAddEditProfileDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<CardProfileEntity?>(null) }
    var profileForCardAdd by remember { mutableStateOf<CardProfileEntity?>(null) }
    var showJsonManifestDialog by remember { mutableStateOf(false) }
    var showPhotoCardScanDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = com.example.ui.theme.HighDensityBackground,
        topBar = {
            SystemHeader(
                title = "Orchestrator Pro",
                onSyncClick = { viewModel.deployNewManifest() },
                onMenuClick = { }
            )
        },
        bottomBar = {
            HighDensityNavBar(
                selectedTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                com.example.ui.components.OperationLoadingBanner(
                    operationState = operationState
                )

                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(targetState = activeTab, label = "TabCrossfade") { tab ->
                        when (tab) {
                            0 -> MonitorScreen(
                                workerJobs = workerJobs,
                                profiles = profiles,
                                cards = allCards,
                                recentLogs = telemetryLogs,
                                isDeploying = isDeploying,
                                backendCardStatusState = backendCardStatusState,
                                onSyncBackendStatus = { viewModel.fetchBackendOrchestratedCardStatuses() },
                                onDeployNewManifest = { viewModel.deployNewManifest() },
                                onJobClick = { viewModel.selectJobForDetails(it) },
                                onClearJobs = { viewModel.clearAllJobs() },
                                onRunPipelineForProfile = { id -> viewModel.runPipelineForProfile(id) }
                            )

                            1 -> ProfilesScreen(
                                profiles = profiles,
                                cards = allCards,
                                isCheckingUnactivated = isCheckingUnactivated,
                                onAddProfileClick = {
                                    profileToEdit = null
                                    showAddEditProfileDialog = true
                                },
                                onEditProfileClick = { profile ->
                                    profileToEdit = profile
                                    showAddEditProfileDialog = true
                                },
                                onDeleteProfileClick = { id -> viewModel.deleteProfile(id) },
                                onRunPipelineForProfile = { id -> viewModel.runPipelineForProfile(id) },
                                onAddCardToProfileClick = { profile -> profileForCardAdd = profile },
                                onSelectPrimaryCard = { profileId, cardId -> viewModel.setPrimaryCardForProfile(profileId, cardId) },
                                onDeleteCardClick = { cardId -> viewModel.deleteCard(cardId) },
                                onJsonManifestClick = {
                                    viewModel.refreshManifestJson()
                                    showJsonManifestDialog = true
                                },
                                onTriggerCheckUnactivated = { viewModel.triggerManualCheckForUnactivated() },
                                onScanCardPhotoClick = { showPhotoCardScanDialog = true }
                            )

                            2 -> LogsScreen(
                                logs = telemetryLogs,
                                onClearLogsClick = { viewModel.clearLogs() }
                            )

                            3 -> ConfigScreen(
                                currentConfig = config,
                                onSaveConfig = { updatedConfig -> viewModel.saveConfig(updatedConfig) },
                                onTestWebhook = { webhookUrl, callback -> viewModel.testWebhook(webhookUrl, callback) }
                            )
                        }
                    }
                }
            }

            // Dialog Modals
            selectedJobDetails?.let { job ->
                JobDetailsDialog(
                    job = job,
                    onRetryJob = {
                        viewModel.retryJob(job)
                        viewModel.selectJobForDetails(null)
                    },
                    onDismiss = { viewModel.selectJobForDetails(null) }
                )
            }

            if (showAddEditProfileDialog) {
                AddEditProfileDialog(
                    existingProfile = profileToEdit,
                    onSave = { profile ->
                        viewModel.saveProfile(profile)
                        showAddEditProfileDialog = false
                    },
                    onDismiss = { showAddEditProfileDialog = false }
                )
            }

            profileForCardAdd?.let { prof ->
                AddCardToProfileDialog(
                    profile = prof,
                    onSaveCard = { newCard ->
                        viewModel.saveCardForProfile(newCard)
                        profileForCardAdd = null
                    },
                    onDismiss = { profileForCardAdd = null },
                    onScanPhotoClick = {
                        profileForCardAdd = null
                        showPhotoCardScanDialog = true
                    }
                )
            }

            if (showPhotoCardScanDialog) {
                PhotoCardScanDialog(
                    profiles = profiles,
                    initialProfileId = profileForCardAdd?.id,
                    onSaveExtractedCard = { card, profileId ->
                        viewModel.saveCardForProfile(card.copy(profileId = profileId))
                        showPhotoCardScanDialog = false
                    },
                    onDismiss = { showPhotoCardScanDialog = false }
                )
            }

            if (showJsonManifestDialog) {
                JsonManifestDialog(
                    jsonText = manifestJsonState,
                    onImportJson = { jsonStr -> viewModel.importManifestJson(jsonStr) },
                    onDismiss = { showJsonManifestDialog = false }
                )
            }
        }
    }
}

