package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CardStatusDto(
    @Json(name = "card_id") val cardId: String,
    @Json(name = "profile_id") val profileId: String = "",
    @Json(name = "card_number_masked") val cardNumberMasked: String = "",
    @Json(name = "is_activated") val isActivated: Boolean = false,
    @Json(name = "activation_status") val activationStatus: String = "PENDING",
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "currency") val currency: String = "CAD",
    @Json(name = "last_checked_at") val lastCheckedAt: Long = System.currentTimeMillis(),
    @Json(name = "portal_url") val portalUrl: String = "",
    @Json(name = "status_message") val statusMessage: String = "OK"
)

@JsonClass(generateAdapter = true)
data class OrchestratedCardStatusSummaryDto(
    @Json(name = "total_cards") val totalCards: Int,
    @Json(name = "activated_count") val activatedCount: Int,
    @Json(name = "pending_count") val pendingCount: Int,
    @Json(name = "total_orchestrated_balance") val totalOrchestratedBalance: Double,
    @Json(name = "last_orchestration_run") val lastOrchestrationRun: Long,
    @Json(name = "cards") val cards: List<CardStatusDto>
)

@JsonClass(generateAdapter = true)
data class FetchCardStatusRequest(
    @Json(name = "card_id") val cardId: String,
    @Json(name = "profile_id") val profileId: String,
    @Json(name = "card_number") val cardNumber: String,
    @Json(name = "serial_ref") val serialRef: String = ""
)

interface CardStatusApiService {

    @GET("api/v1/cards/status/{cardId}")
    suspend fun getCardStatus(@Path("cardId") cardId: String): Response<CardStatusDto>

    @GET("api/v1/orchestrator/cards/status")
    suspend fun getOrchestratedCardStatusSummary(): Response<OrchestratedCardStatusSummaryDto>

    @POST("api/v1/cards/check-status")
    suspend fun fetchOrchestratedCardStatus(@Body request: FetchCardStatusRequest): Response<CardStatusDto>
}

object CardStatusApiClient {
    private const val DEFAULT_BASE_URL = "https://api.orchestrator.internal/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun create(baseUrl: String = DEFAULT_BASE_URL): CardStatusApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CardStatusApiService::class.java)
    }
}
