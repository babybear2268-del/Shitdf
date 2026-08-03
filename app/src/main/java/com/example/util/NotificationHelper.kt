package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orchestrator Worker Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts and updates for active portal worker jobs"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showJobCompletedNotification(jobId: String, profileName: String, balance: Double) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pipeline Job Completed")
            .setContentText("Worker $jobId ($profileName) extracted balance: $$balance")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(jobId.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "orchestrator_worker_channel"
    }
}
