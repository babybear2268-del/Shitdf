package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.RealWebExecutor
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class OrchestratorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrchestratorRepository.getInstance(application)
    private val webExecutor = RealWebExecutor()
    private val notificationHelper = NotificationHelper(application)

    val activeTab = MutableStateFlow(0)
    val selectedJobDetails = MutableStateFlow<WorkerJobEntity?>(null)
    val isDeploying = MutableStateFlow(false)

    val profiles: StateFlow<List<CardProfileEntity>> = repository.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCards: StateFlow<List<PaymentCardEntity>> = repository.allCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val workerJobs: StateFlow<List<WorkerJobEntity>> = repository.workerJobs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val telemetryLogs: StateFlow<List<TelemetryLogEntity>> = repository.telemetryLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        startPeriodicAutoChecker()
    }

    private fun startPeriodicAutoChecker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val currentConfig = repository.getConfig() ?: OrchestratorConfigEntity()
                val intervalMinutes = currentConfig.autoCheckIntervalMinutes.coerceAtLeast(1)
                val delayMs = intervalMinutes * 60 * 1000L

                delay(delayMs)

                val latestConfig = repository.getConfig() ?: OrchestratorConfigEntity()
                if (latestConfig.autoCheckEnabled) {
                    checkAllUnactivatedCardsInternal()
                }
            }
        }
    }

    fun triggerManualCheckForUnactivated() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog("INFO", "Manual check triggered for all pending unactivated prepaid cards.")
            checkAllUnactivatedCardsInternal()
        }
    }

    private suspend fun checkAllUnactivatedCardsInternal() {
        val allProfilesList = repository.profiles.first()
        val allCardsList = repository.allCards.first()

        val pendingProfiles = allProfilesList.filter { profile ->
            val card = allCardsList.find { it.profileId == profile.id && it.isPrimary }
                ?: allCardsList.find { it.profileId == profile.id }
            card == null || !card.isActivated || card.balance <= 0.0
        }

        if (pendingProfiles.isNotEmpty()) {
            repository.insertLog("INFO", "Automated 5-minute check cycle running for ${pendingProfiles.size} unactivated card(s)...")
            pendingProfiles.forEach { profile ->
                runPipelineWorkerForProfile(profile)
            }
        } else {
            repository.insertLog("INFO", "Automated 5-minute check cycle: All registered cards are activated.")
        }
    }

    val config: StateFlow<OrchestratorConfigEntity> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrchestratorConfigEntity()
    )

    fun setActiveTab(tab: Int) {
        activeTab.value = tab
    }

    fun selectJobForDetails(job: WorkerJobEntity?) {
        selectedJobDetails.value = job
    }

    fun deployNewManifest() {
        viewModelScope.launch(Dispatchers.IO) {
            isDeploying.value = true
            repository.insertLog("INFO", "Initiating global manifest deployment & pipeline check across all active profiles...")

            val activeProfiles = profiles.value
            if (activeProfiles.isNotEmpty()) {
                activeProfiles.forEach { profile ->
                    runPipelineWorkerForProfile(profile)
                }
            } else {
                repository.insertLog("WARN", "No active card profiles found for manifest deployment.")
            }

            isDeploying.value = false
            repository.insertLog("INFO", "Global manifest deployment completed.")
        }
    }

    fun runPipelineForProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profiles.value.find { it.id == profileId }
            if (profile != null) {
                runPipelineWorkerForProfile(profile)
            } else {
                repository.insertLog("ERROR", "Cannot run pipeline: Profile $profileId not found.")
            }
        }
    }

    private suspend fun runPipelineWorkerForProfile(profile: CardProfileEntity) {
        val jobId = "JOB-WRK-${UUID.randomUUID().toString().take(6).uppercase()}"
        val cards = repository.allCards.first().filter { it.profileId == profile.id }
        val card = cards.find { it.isPrimary } ?: cards.firstOrNull() ?: PaymentCardEntity(
            profileId = profile.id,
            cardNumber = profile.cardNumber,
            cardExpMonth = profile.cardExpMonth,
            cardExpYear = profile.cardExpYear,
            cardCvc = profile.cardCvc
        )

        var job = WorkerJobEntity(
            jobId = jobId,
            profileId = profile.id,
            targetPortalUrl = profile.targetPortalUrl,
            status = "TRANSMITTING",
            currentStep = "1/5 Initializing Web Execution Environment",
            currentHeuristic = "USER-AGENT / COOKIE CONTAINER PREPARATION",
            progressPercentage = 0.20f,
            updatedAt = System.currentTimeMillis()
        )
        repository.insertOrUpdateJob(job)
        repository.insertLog("INFO", "Worker $jobId dispatched for target portal: ${profile.targetPortalUrl}", jobId)

        try {
            // Step 2: Target Inspection & DOM Field Discovery
            job = job.copy(
                status = "SCANNING",
                currentStep = "2/5 Executing Real HTTP Target Portal Inspection",
                currentHeuristic = "HEURISTIC FORM & CAPTCHA PROFILES SCAN",
                progressPercentage = 0.40f,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateJob(job)
            repository.insertLog("EXEC", "Scanning DOM elements & anti-bot mechanisms at ${profile.targetPortalUrl}", jobId)

            val currentConfig = config.value
            val inspectionResult = webExecutor.executeRealTargetInspection(profile.targetPortalUrl, currentConfig)

            val autoTargeted = if (inspectionResult.detectedFields.isNotEmpty()) {
                inspectionResult.detectedFields.joinToString(", ") { "${it.fieldType}: ${it.selector}" }
            } else {
                "input[name*='card'], input[name*='cvc']"
            }

            val autoPopulated = if (inspectionResult.detectedFields.isNotEmpty()) {
                inspectionResult.detectedFields.take(3).joinToString(" | ") { field ->
                    when (field.fieldType) {
                        "card_number" -> "${field.selector} = ****${card.cardNumber.takeLast(4)}"
                        "cvc" -> "${field.selector} = ***"
                        "postal_code" -> "${field.selector} = ${profile.billingZip}"
                        "exp_month" -> "${field.selector} = ${card.cardExpMonth}"
                        "exp_year" -> "${field.selector} = ${card.cardExpYear}"
                        else -> "${field.selector} = Auto-Filled"
                    }
                }
            } else {
                "Card: ****${card.cardNumber.takeLast(4)} | Zip: ${profile.billingZip}"
            }

            job = job.copy(
                currentStep = "3/5 Generating Playwright Automation Script",
                currentHeuristic = "DOM FINDER: ${inspectionResult.detectedFields.size} FIELDS FOUND | PROTECTION: ${inspectionResult.estimatedProtection.uppercase()}",
                progressPercentage = 0.60f,
                generatedPlaywrightScript = webExecutor.generatePlaywrightScript(profile, card),
                currentTargetedField = autoTargeted,
                currentPopulatedField = autoPopulated,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateJob(job)
            repository.insertLog("INFO", "Playwright script compiled. Protection: ${inspectionResult.estimatedProtection}", jobId)

            // Step 4: Transmitting payload & executing real HTTP DOM balance extraction
            job = job.copy(
                status = "TRANSMITTING",
                currentStep = "4/5 Executing Real HTTP Payload & DOM Balance Extraction",
                currentHeuristic = "SELECTOR MATCH: ${profile.balanceSelector.ifBlank { "AUTOMATIC CURRENCY DETECTOR" }}",
                progressPercentage = 0.80f,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateJob(job)

            val realResult = webExecutor.executeRealHttpExecution(profile.targetPortalUrl, profile, card, currentConfig)
            realResult.logs.forEach { logMsg ->
                repository.insertLog("EXEC", logMsg, jobId)
            }

            // Step 5: Process Real Execution Result
            val extractedBal = realResult.extractedBalance
            val outputVal = extractedBal ?: card.balance
            val now = System.currentTimeMillis()

            val newlyActivated = (extractedBal != null && extractedBal > 0.0) || card.balance > 0.0
            val updatedCard = card.copy(
                balance = outputVal,
                isActivated = newlyActivated,
                activationStatus = if (newlyActivated) "ACTIVATED" else "PENDING_ACTIVATION",
                lastCheckedAt = now
            )
            repository.saveCard(updatedCard)

            if (newlyActivated && !card.isActivated) {
                repository.insertLog("INFO", "🎉 CARD ACTIVATED! Balance found: $$outputVal for card ending in **** ${card.cardNumber.takeLast(4)}", jobId)
            } else if (!newlyActivated) {
                repository.insertLog("INFO", "Card **** ${card.cardNumber.takeLast(4)} check result: Pending activation ($$outputVal balance). Will re-check automatically in 5 minutes.", jobId)
            }

            val statusText = if (realResult.isSuccessful) "COMPLETED" else "HALTED"
            val heuristicText = when {
                extractedBal != null -> "LIVE DOM BALANCE EXTRACTED: $${extractedBal}"
                realResult.isSuccessful -> "HTTP ${realResult.statusCode} OK | Portal response verified (Pending activation)"
                else -> "HTTP ${realResult.statusCode} FAILED | Response Time: ${realResult.responseTimeMs}ms"
            }

            job = job.copy(
                status = statusText,
                currentStep = "5/5 Execution Finished - Network Response Processed",
                currentHeuristic = heuristicText,
                progressPercentage = 1.0f,
                terminalOutputValue = outputVal,
                errorMessage = if (!realResult.isSuccessful) "HTTP Response Error Code ${realResult.statusCode}" else null,
                updatedAt = now
            )
            repository.insertOrUpdateJob(job)
            repository.insertLog("INFO", "Worker $jobId finished with status $statusText. Output: $$outputVal (Server: ${realResult.serverHeader})", jobId)

            // System notification & Webhook
            notificationHelper.showJobCompletedNotification(jobId, "${profile.holderFirstName} ${profile.holderLastName}", outputVal)
            if (currentConfig.discordWebhookUrl.isNotBlank()) {
                webExecutor.postWebhookNotification(
                    currentConfig.discordWebhookUrl,
                    "Worker `$jobId` completed for `${profile.holderFirstName} ${profile.holderLastName}` on portal `${profile.targetPortalUrl}`. Balance output: **$$outputVal**"
                )
            }
        } catch (e: Throwable) {
            val now = System.currentTimeMillis()
            repository.insertLog("ERROR", "Worker $jobId encountered exception: ${e.localizedMessage ?: e.javaClass.simpleName}", jobId)
            job = job.copy(
                status = "FAILED",
                currentStep = "Worker Execution Exception",
                currentHeuristic = "ERROR: ${e.localizedMessage ?: "Network or URL error"}",
                progressPercentage = 1.0f,
                errorMessage = e.localizedMessage ?: "Pipeline execution failed",
                updatedAt = now
            )
            repository.insertOrUpdateJob(job)
        }
    }

    fun retryJob(job: WorkerJobEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog("INFO", "Retrying job ${job.jobId} for profile ${job.profileId}")
            runPipelineForProfile(job.profileId)
        }
    }

    fun clearAllJobs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllJobs()
        }
    }

    fun deleteWorkerJob(jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJob(jobId)
        }
    }

    fun saveProfile(profile: CardProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(profile)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProfile(profileId)
        }
    }

    fun saveCardForProfile(card: PaymentCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCard(card)
        }
    }

    fun setPrimaryCardForProfile(profileId: String, cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPrimaryCard(profileId, cardId)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCard(cardId)
        }
    }

    val manifestJsonState = MutableStateFlow("")

    fun refreshManifestJson() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportManifestJson()
            manifestJsonState.value = json
        }
    }

    fun exportManifestJson(): String {
        return manifestJsonState.value
    }

    fun importManifestJson(jsonStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.importManifestJson(jsonStr)
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    fun saveConfig(updatedConfig: OrchestratorConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveConfig(updatedConfig)
        }
    }

    fun testWebhook(webhookUrl: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = webExecutor.postWebhookNotification(
                webhookUrl,
                "Test message from Orchestrator Pro! System telemetry webhook connection established."
            )
            callback(success)
            if (success) {
                repository.insertLog("INFO", "Webhook test succeeded for $webhookUrl")
            } else {
                repository.insertLog("ERROR", "Webhook test failed for $webhookUrl")
            }
        }
    }
}
