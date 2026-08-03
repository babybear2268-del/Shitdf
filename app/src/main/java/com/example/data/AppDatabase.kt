package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardProfileEntity::class,
        WorkerJobEntity::class,
        TelemetryLogEntity::class,
        OrchestratorConfigEntity::class,
        PaymentCardEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardProfileDao(): CardProfileDao
    abstract fun workerJobDao(): WorkerJobDao
    abstract fun telemetryLogDao(): TelemetryLogDao
    abstract fun configDao(): ConfigDao
    abstract fun paymentCardDao(): PaymentCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orchestrator_pro.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
