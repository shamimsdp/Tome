package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.engine.AppReleaseInfo
import com.example.engine.AppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks GitHub Releases once per day.
 * If a newer version is published for "shamimsdp/Tome", it posts a system notification
 * so the user can easily review and update the app.
 */
class AppUpdateCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "tome_daily_update_check"
        const val CHANNEL_ID = "tome_app_updates_channel"
        const val NOTIFICATION_ID = 4040

        /**
         * Enqueues a periodic background check running once every 24 hours.
         */
        fun scheduleDailyCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateWorkRequest = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
                24, TimeUnit.HOURS,
                2, TimeUnit.HOURS // flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateWorkRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val updateManager = AppUpdateManager(appContext)
            val result = updateManager.checkForUpdates(forceCheck = false)

            result.getOrNull()?.let { release ->
                if (release.isNewer) {
                    showUpdateNotification(release)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showUpdateNotification(release: AppReleaseInfo) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tome App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a new Tome version is published on GitHub"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_open_update", true)
            putExtra("extra_release_tag", release.tagName)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New Tome Update: ${release.tagName}")
            .setContentText("Version ${release.versionName} is available on GitHub.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${release.releaseTitle}\n\n${release.releaseNotes.take(300)}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
