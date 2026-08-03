package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class OrchestratorRepository private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val profileDao = db.cardProfileDao()
    private val cardDao = db.paymentCardDao()
    private val jobDao = db.workerJobDao()
    private val logDao = db.telemetryLogDao()
    private val configDao = db.configDao()

    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    val profiles: Flow<List<CardProfileEntity>> = profileDao.getAllProfiles()
    val allCards: Flow<List<PaymentCardEntity>> = cardDao.getAllCards()
    val workerJobs: Flow<List<WorkerJobEntity>> = jobDao.getAllWorkerJobs()
    val telemetryLogs: Flow<List<TelemetryLogEntity>> = logDao.getRecentLogs()
    val config: Flow<OrchestratorConfigEntity> = configDao.getConfigFlow()
        .map { it ?: OrchestratorConfigEntity() }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (configDao.getConfig() == null) {
            configDao.saveConfig(OrchestratorConfigEntity())
        }
        logDao.insertLog(
            TelemetryLogEntity(
                timestampStr = timeFormatter.format(Date()),
                level = "INFO",
                message = "System database and config initialized."
            )
        )
    }

    suspend fun saveProfile(profile: CardProfileEntity) {
        profileDao.insertProfile(profile)
        insertLog("INFO", "Profile saved/updated: ${profile.id} (${profile.holderFirstName} ${profile.holderLastName})")
    }

    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfileById(profileId)
        cardDao.deleteCardsForProfile(profileId)
        insertLog("WARN", "Profile deleted: $profileId along with associated payment cards.")
    }

    suspend fun saveCard(card: PaymentCardEntity) {
        cardDao.insertCard(card)
        insertLog("INFO", "Card ${card.id} updated/added for Profile ${card.profileId}")
    }

    suspend fun setPrimaryCard(profileId: String, cardId: String) {
        val cards = cardDao.getCardsForProfileList(profileId)
        cards.forEach { c ->
            cardDao.insertCard(c.copy(isPrimary = c.id == cardId))
        }
        insertLog("INFO", "Primary card set to $cardId for Profile $profileId")
    }

    suspend fun deleteCard(cardId: String) {
        cardDao.deleteCardById(cardId)
        insertLog("WARN", "Payment card deleted: $cardId")
    }

    suspend fun insertOrUpdateJob(job: WorkerJobEntity) {
        jobDao.insertOrUpdateJob(job)
    }

    suspend fun deleteJob(jobId: String) {
        jobDao.deleteJobById(jobId)
    }

    suspend fun clearAllJobs() {
        jobDao.clearAllJobs()
        insertLog("INFO", "Cleared all worker jobs queue.")
    }

    suspend fun insertLog(level: String, message: String, jobId: String? = null) {
        logDao.insertLog(
            TelemetryLogEntity(
                workerJobId = jobId,
                timestampStr = timeFormatter.format(Date()),
                level = level,
                message = message
            )
        )
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
        insertLog("INFO", "Telemetry logs cleared by user.")
    }

    suspend fun saveConfig(config: OrchestratorConfigEntity) {
        configDao.saveConfig(config)
        insertLog("INFO", "Orchestrator configuration updated. Headless: ${config.headlessMode}, Max Parallel: ${config.maxParallelWorkers}")
    }

    suspend fun getConfig(): OrchestratorConfigEntity {
        return configDao.getConfig() ?: OrchestratorConfigEntity()
    }

    suspend fun exportManifestJson(): String {
        val profilesList = profileDao.getAllProfilesList()
        val cardsList = cardDao.getAllCardsList()
        val configObj = configDao.getConfig() ?: OrchestratorConfigEntity()

        val root = JSONObject()
        root.put("version", "2.0")
        root.put("timestamp", System.currentTimeMillis())

        val profilesArr = JSONArray()
        profilesList.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("targetPortalUrl", p.targetPortalUrl)
            pObj.put("balanceSelector", p.balanceSelector)
            pObj.put("holderFirstName", p.holderFirstName)
            pObj.put("holderLastName", p.holderLastName)
            pObj.put("holderEmail", p.holderEmail)
            pObj.put("holderPhone", p.holderPhone)
            pObj.put("billingStreet", p.billingStreet)
            pObj.put("billingUnit", p.billingUnit)
            pObj.put("billingCity", p.billingCity)
            pObj.put("billingState", p.billingState)
            pObj.put("billingZip", p.billingZip)
            pObj.put("billingCountry", p.billingCountry)
            pObj.put("cardNumber", p.cardNumber)
            pObj.put("cardExpMonth", p.cardExpMonth)
            pObj.put("cardExpYear", p.cardExpYear)
            pObj.put("cardCvc", p.cardCvc)

            val pCards = cardsList.filter { it.profileId == p.id }
            val cardsArr = JSONArray()
            pCards.forEach { c ->
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("cardNumber", c.cardNumber)
                cObj.put("cardExpMonth", c.cardExpMonth)
                cObj.put("cardExpYear", c.cardExpYear)
                cObj.put("cardCvc", c.cardCvc)
                cObj.put("cardholderName", c.cardholderName)
                cObj.put("cardSerialRef", c.cardSerialRef)
                cObj.put("cardBrand", c.cardBrand)
                cObj.put("balance", c.balance)
                cObj.put("isPrimary", c.isPrimary)
                cObj.put("notes", c.notes)
                cardsArr.put(cObj)
            }
            pObj.put("associatedCards", cardsArr)
            profilesArr.put(pObj)
        }
        root.put("profiles", profilesArr)

        val cfgObj = JSONObject()
        cfgObj.put("discordWebhookUrl", configObj.discordWebhookUrl)
        cfgObj.put("maxParallelWorkers", configObj.maxParallelWorkers)
        cfgObj.put("headlessMode", configObj.headlessMode)
        cfgObj.put("randomizeUserAgent", configObj.randomizeUserAgent)
        cfgObj.put("uAStringOverride", configObj.uAStringOverride)
        cfgObj.put("heuristicTimeoutMs", configObj.heuristicTimeoutMs)
        root.put("config", cfgObj)

        return root.toString(2)
    }

    suspend fun importManifestJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("profiles")) {
                val profilesArr = root.getJSONArray("profiles")
                for (i in 0 until profilesArr.length()) {
                    val pObj = profilesArr.getJSONObject(i)
                    val prof = CardProfileEntity(
                        id = pObj.optString("id", "PRF-${UUID.randomUUID().toString().take(4).uppercase()}"),
                        targetPortalUrl = pObj.optString("targetPortalUrl", "https://www.myprepaidcenter.com/login/card"),
                        balanceSelector = pObj.optString("balanceSelector", ".card-balance"),
                        holderFirstName = pObj.optString("holderFirstName", "John"),
                        holderLastName = pObj.optString("holderLastName", "Doe"),
                        holderEmail = pObj.optString("holderEmail", "john.doe@example.com"),
                        holderPhone = pObj.optString("holderPhone", "+1 416-555-0199"),
                        billingStreet = pObj.optString("billingStreet", "100 King St W"),
                        billingUnit = pObj.optString("billingUnit", ""),
                        billingCity = pObj.optString("billingCity", "Toronto"),
                        billingState = pObj.optString("billingState", "ON"),
                        billingZip = pObj.optString("billingZip", "M5X 1A9"),
                        billingCountry = pObj.optString("billingCountry", "Canada"),
                        cardNumber = pObj.optString("cardNumber", "5442129412345678"),
                        cardExpMonth = pObj.optString("cardExpMonth", "12"),
                        cardExpYear = pObj.optString("cardExpYear", "2028"),
                        cardCvc = pObj.optString("cardCvc", "123")
                    )
                    profileDao.insertProfile(prof)

                    if (pObj.has("associatedCards")) {
                        val cardsArr = pObj.getJSONArray("associatedCards")
                        for (j in 0 until cardsArr.length()) {
                            val cObj = cardsArr.getJSONObject(j)
                            val card = PaymentCardEntity(
                                id = cObj.optString("id", "CARD-${UUID.randomUUID().toString().take(6).uppercase()}"),
                                profileId = prof.id,
                                cardNumber = cObj.optString("cardNumber", prof.cardNumber),
                                cardExpMonth = cObj.optString("cardExpMonth", prof.cardExpMonth),
                                cardExpYear = cObj.optString("cardExpYear", prof.cardExpYear),
                                cardCvc = cObj.optString("cardCvc", prof.cardCvc),
                                cardholderName = cObj.optString("cardholderName", "${prof.holderFirstName} ${prof.holderLastName}"),
                                cardSerialRef = cObj.optString("cardSerialRef", "REF-${(1000..9999).random()}"),
                                cardBrand = cObj.optString("cardBrand", "Mastercard"),
                                balance = cObj.optDouble("balance", 0.0),
                                isPrimary = cObj.optBoolean("isPrimary", j == 0),
                                notes = cObj.optString("notes", "Imported via Manifest JSON")
                            )
                            cardDao.insertCard(card)
                        }
                    }
                }
            }
            if (root.has("config")) {
                val cfgObj = root.getJSONObject("config")
                val cfg = OrchestratorConfigEntity(
                    id = 1,
                    discordWebhookUrl = cfgObj.optString("discordWebhookUrl", ""),
                    maxParallelWorkers = cfgObj.optInt("maxParallelWorkers", 4),
                    headlessMode = cfgObj.optBoolean("headlessMode", true),
                    randomizeUserAgent = cfgObj.optBoolean("randomizeUserAgent", true),
                    uAStringOverride = cfgObj.optString("uAStringOverride", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/123.0.0.0"),
                    heuristicTimeoutMs = cfgObj.optLong("heuristicTimeoutMs", 45000L)
                )
                configDao.saveConfig(cfg)
            }
            insertLog("INFO", "Manifest JSON imported successfully.")
            true
        } catch (e: Exception) {
            insertLog("ERROR", "Failed to import JSON manifest: ${e.localizedMessage}")
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: OrchestratorRepository? = null

        fun getInstance(context: Context): OrchestratorRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = OrchestratorRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
