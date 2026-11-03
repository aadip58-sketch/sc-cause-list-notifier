package gov.sci.causelist.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import gov.sci.causelist.MainActivity
import gov.sci.causelist.R
import gov.sci.causelist.model.CauseListItem

object NotificationHelper {

    const val CHANNEL_ID = "cause_list_alerts"
    private const val CHANNEL_NAME = "Cause List Alerts"
    private const val CHANNEL_DESC = "Notifications when new Supreme Court cause lists drop"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNewCauseListNotification(context: Context, newItems: List<CauseListItem>) {
        if (newItems.isEmpty()) return

        createNotificationChannel(context)

        val firstItem = newItems.first()
        val title = if (newItems.size == 1) {
            "🚨 Supreme Court: ${firstItem.category} List Dropped!"
        } else {
            "🚨 Supreme Court: ${newItems.size} New Cause Lists Dropped!"
        }

        val message = if (newItems.size == 1) {
            "${firstItem.title} for ${firstItem.date} is now available."
        } else {
            "Latest: ${firstItem.title} (${firstItem.date}) + ${newItems.size - 1} more lists."
        }

        // Tap opens MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button to open PDF directly
        val pdfIntent = Intent(Intent.ACTION_VIEW, Uri.parse(firstItem.pdfUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pdfPendingIntent = PendingIntent.getActivity(
            context,
            firstItem.id.hashCode(),
            pdfIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nTap to view all cause lists or tap 'View PDF' below.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_pdf, "View PDF", pdfPendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
