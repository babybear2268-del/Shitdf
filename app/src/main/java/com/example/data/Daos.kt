package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardProfileDao {
    @Query("SELECT * FROM card_profiles")
    fun getAllProfiles(): Flow<List<CardProfileEntity>>

    @Query("SELECT * FROM card_profiles")
    suspend fun getAllProfilesList(): List<CardProfileEntity>

    @Query("SELECT COUNT(*) FROM card_profiles")
    suspend fun getProfileCount(): Int

    @Query("SELECT * FROM card_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): CardProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CardProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<CardProfileEntity>)

    @Delete
    suspend fun deleteProfile(profile: CardProfileEntity)

    @Query("DELETE FROM card_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)
}

@Dao
interface WorkerJobDao {
    @Query("SELECT * FROM worker_jobs ORDER BY updatedAt DESC")
    fun getAllWorkerJobs(): Flow<List<WorkerJobEntity>>

    @Query("SELECT * FROM worker_jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): WorkerJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateJob(job: WorkerJobEntity)

    @Query("DELETE FROM worker_jobs WHERE jobId = :jobId")
    suspend fun deleteJobById(jobId: String)

    @Query("DELETE FROM worker_jobs")
    suspend fun clearAllJobs()
}

@Dao
interface TelemetryLogDao {
    @Query("SELECT * FROM telemetry_logs ORDER BY id DESC LIMIT 500")
    fun getRecentLogs(): Flow<List<TelemetryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLogEntity)

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearLogs()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM orchestrator_config WHERE id = 1")
    fun getConfigFlow(): Flow<OrchestratorConfigEntity?>

    @Query("SELECT * FROM orchestrator_config WHERE id = 1")
    suspend fun getConfig(): OrchestratorConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: OrchestratorConfigEntity)
}

@Dao
interface PaymentCardDao {
    @Query("SELECT * FROM payment_cards WHERE profileId = :profileId")
    fun getCardsForProfile(profileId: String): Flow<List<PaymentCardEntity>>

    @Query("SELECT * FROM payment_cards WHERE profileId = :profileId")
    suspend fun getCardsForProfileList(profileId: String): List<PaymentCardEntity>

    @Query("SELECT * FROM payment_cards")
    fun getAllCards(): Flow<List<PaymentCardEntity>>

    @Query("SELECT * FROM payment_cards")
    suspend fun getAllCardsList(): List<PaymentCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: PaymentCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<PaymentCardEntity>)

    @Delete
    suspend fun deleteCard(card: PaymentCardEntity)

    @Query("DELETE FROM payment_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: String)

    @Query("DELETE FROM payment_cards WHERE profileId = :profileId")
    suspend fun deleteCardsForProfile(profileId: String)
}
