package in.gov.sci.causelist.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import in.gov.sci.causelist.MainActivity
import in.gov.sci.causelist.R
import in.gov.sci.causelist.parser.CauseListParser
import in.gov.sci.causelist.util.NotificationHelper
import in.gov.sci.causelist.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CauseListForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var prefs: PreferencesManager

    companion object {
        const val SERVICE_CHANNEL_ID = "sc_monitor_service_channel"
        const val NOTIFICATION_ID = 1001
        const val CHECK_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes

        const val ACTION_START = "ACTION_START_MONITORING"
        const val ACTION_STOP = "ACTION_STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, CauseListForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CauseListForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createServiceNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification("Starting 2-min check loop..."))
        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val causeLists = CauseListParser.fetchCauseLists()
                    if (causeLists.isNotEmpty()) {
                        val seenIds = prefs.getSeenIds()
                        val isFirstRun = prefs.isFirstRun()

                        if (isFirstRun) {
                            prefs.addSeenIds(causeLists.map { it.id })
                        } else {
                            val newLists = causeLists.filter { it.id !in seenIds }
                            if (newLists.isNotEmpty()) {
                                // Trigger high-priority alert notification
                                NotificationHelper.sendNewCauseListNotification(
                                    applicationContext,
                                    newLists
                                )
                                prefs.addSeenIds(newLists.map { it.id })
                            }
                        }

                        val now = System.currentTimeMillis()
                        prefs.setLastChecked(now)
                        val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(now))
                        updateForegroundNotification("Active • Last checked: $timeStr (Next in 2m)")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    updateForegroundNotification("Retrying connection in 2m...")
                }

                // Wait exactly 2 minutes
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Monitoring Service Status",
                NotificationManager.IMPORTANCE_LOW // Silent ongoing notification
            ).apply {
                description = "Shows active background cause list monitoring state"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Supreme Court Cause List Monitor")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val notification = buildForegroundNotification(contentText)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
