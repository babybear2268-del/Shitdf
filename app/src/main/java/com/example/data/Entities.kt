package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_cards")
data class PaymentCardEntity(
    @PrimaryKey val id: String = "CARD-${java.util.UUID.randomUUID().toString().take(6).uppercase()}",
    val profileId: String,
    val cardNumber: String,
    val cardExpMonth: String,
    val cardExpYear: String,
    val cardCvc: String,
    val cardholderName: String = "VALUED CARDHOLDER",
    val cardSerialRef: String = "",
    val cardBrand: String = "Mastercard",
    val balance: Double = 0.0,
    val isPrimary: Boolean = false,
    val notes: String = "",
    val isActivated: Boolean = false,
    val activationStatus: String = "PENDING_ACTIVATION",
    val lastCheckedAt: Long = 0L
)

@Entity(tableName = "card_profiles")
data class CardProfileEntity(
    @PrimaryKey val id: String,
    val targetPortalUrl: String,
    val balanceSelector: String,
    val holderFirstName: String,
    val holderLastName: String,
    val holderEmail: String,
    val holderPhone: String,
    val billingStreet: String,
    val billingUnit: String,
    val billingCity: String,
    val billingState: String,
    val billingZip: String,
    val billingCountry: String,
    val cardNumber: String,
    val cardExpMonth: String,
    val cardExpYear: String,
    val cardCvc: String
)

@Entity(tableName = "worker_jobs")
data class WorkerJobEntity(
    @PrimaryKey val jobId: String,
    val profileId: String,
    val targetPortalUrl: String,
    val status: String, // "TRANSMITTING", "SCANNING", "COMPLETED", "HALTED", "QUEUED"
    val currentStep: String,
    val currentHeuristic: String,
    val progressPercentage: Float,
    val terminalOutputValue: Double? = null,
    val errorMessage: String? = null,
    val generatedPlaywrightScript: String? = null,
    val currentTargetedField: String? = null,
    val currentPopulatedField: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telemetry_logs")
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerJobId: String? = null,
    val timestampStr: String,
    val level: String, // "INFO", "WARN", "ERROR", "EXEC"
    val message: String
)

@Entity(tableName = "orchestrator_config")
data class OrchestratorConfigEntity(
    @PrimaryKey val id: Int = 1,
    val discordWebhookUrl: String = "",
    val maxParallelWorkers: Int = 4,
    val headlessMode: Boolean = true,
    val randomizeUserAgent: Boolean = true,
    val uAStringOverride: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/123.0.0.0",
    val heuristicTimeoutMs: Long = 45000L,
    val autoCheckIntervalMinutes: Int = 5,
    val autoCheckEnabled: Boolean = true
)

object CanadianIdentityGenerator {
    private val firstNames = listOf(
        "Liam", "Noah", "Olivia", "Emma", "Lucas", "Sophie", "Benjamin", "Chloe",
        "Ethan", "Maya", "William", "Élodie", "Alexander", "Hannah", "Gabriel", "Amelia",
        "Jean", "Mathieu", "Antoine", "Camille", "Félix", "Florence", "Julien", "Rosalie"
    )
    private val lastNames = listOf(
        "Tremblay", "Roy", "Gagnon", "Bouchard", "Smith", "Côté", "MacDonald", "Fortin",
        "Gauthier", "Morin", "Lavoie", "Campbell", "Leblanc", "Paquette", "Wong", "Singh",
        "Miller", "Desjardins", "Belanger", "Caron", "Pelletier", "Levesque", "Bérubé"
    )

    private data class CaAddress(val city: String, val province: String, val zip: String, val street: String, val areaCode: String)
    private val addresses = listOf(
        CaAddress("Toronto", "ON", "M5X 1A9", "100 King St W", "416"),
        CaAddress("Toronto", "ON", "M5B 2L7", "250 Yonge St", "647"),
        CaAddress("Montreal", "QC", "H3B 4W5", "1000 Rue de la Gauchetière", "514"),
        CaAddress("Montreal", "QC", "H2Z 1W7", "500 Boulevard René-Lévesque", "438"),
        CaAddress("Vancouver", "BC", "V7X 1C4", "701 W Georgia St", "604"),
        CaAddress("Vancouver", "BC", "V6C 3L6", "200 Burrard St", "778"),
        CaAddress("Calgary", "AB", "T2P 1N2", "225 6 Ave SW", "403"),
        CaAddress("Calgary", "AB", "T2P 4H2", "400 3 Ave SW", "587"),
        CaAddress("Ottawa", "ON", "K1P 6B9", "99 Bank St", "613"),
        CaAddress("Quebec City", "QC", "G1R 5A7", "900 Boulevard René-Lévesque E", "418"),
        CaAddress("Halifax", "NS", "B3J 3N4", "1801 Hollis St", "902"),
        CaAddress("Edmonton", "AB", "T5J 3A3", "10180 101 St NW", "780"),
        CaAddress("Winnipeg", "MB", "R3C 4T3", "201 Portage Ave", "204")
    )

    private val cardBins = listOf(
        "54421294", "51051056", "45327182", "54241801", "40001234", "37144963"
    )

    fun generate(targetUrl: String = "https://www.myprepaidcenter.com/login/card"): CardProfileEntity {
        val fn = firstNames.random()
        val ln = lastNames.random()
        val addr = addresses.random()
        val bin = cardBins.random()
        val cardSuffix = (10000000..99999999).random().toString()
        val fullCard = "$bin$cardSuffix"
        val expM = String.format("%02d", (1..12).random())
        val expY = (2026..2031).random().toString()
        val cvc = (100..999).random().toString()
        val phoneNum = "+1 ${addr.areaCode}-${(100..999).random()}-${(1000..9999).random()}"
        val emailDomain = listOf("gmail.com", "yahoo.ca", "outlook.com", "rogers.com", "bell.net").random()
        val email = "${fn.lowercase()}.${ln.lowercase()}${(10..99).random()}@$emailDomain"
        val profileId = "PRF-CA-${java.util.UUID.randomUUID().toString().take(4).uppercase()}"

        return CardProfileEntity(
            id = profileId,
            targetPortalUrl = targetUrl,
            balanceSelector = ".card-balance, .account-balance, #balance",
            holderFirstName = fn,
            holderLastName = ln,
            holderEmail = email,
            holderPhone = phoneNum,
            billingStreet = addr.street,
            billingUnit = if ((0..1).random() == 1) "Apt ${(10..999).random()}" else "",
            billingCity = addr.city,
            billingState = addr.province,
            billingZip = addr.zip,
            billingCountry = "Canada",
            cardNumber = "",
            cardExpMonth = "",
            cardExpYear = "",
            cardCvc = ""
        )
    }
}
