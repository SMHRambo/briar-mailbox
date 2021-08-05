package org.briarproject.mailbox.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import org.briarproject.mailbox.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailboxNotificationManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    companion object {
        private const val CHANNEL_ID = "Briar Mailbox Service"

        const val NOTIFICATION_MAIN_ID = 1
    }

    private val nm = getSystemService(ctx, NotificationManager::class.java)!!

    init {
        if (Build.VERSION.SDK_INT >= 26) createNotificationChannels()
    }

    @RequiresApi(26)
    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.notification_channel_name),
                IMPORTANCE_LOW,
            )
        )
        nm.createNotificationChannels(channels)
    }

    val serviceNotification: Notification
        get() {
            val notificationIntent = Intent(ctx, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                ctx, 0, notificationIntent, 0
            )
            return NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setContentTitle(ctx.getString(R.string.notification_mailbox_title))
                .setContentText(ctx.getString(R.string.notification_mailbox_content))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_MIN)
                .build()
        }

}
