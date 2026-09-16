package io.github.flyoer5.octopusapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 前台服务：让 octopus 进程在后台持续运行。
 */
class OctopusForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        OctopusEngine.stop()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Octopus 服务",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Octopus 运行中")
            .setContentText("LLM 聚合网关监听 127.0.0.1:${OctopusConfig.DEFAULT_PORT}")
            .setSmallIcon(android.R.drawable.stat_sys_data_connected)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val CHANNEL_ID = "octopus_service"
        private const val NOTIFICATION_ID = 18080

        fun start(context: Context) {
            val intent = Intent(context, OctopusForegroundService::class.java)
            ContextCompat_startForeground(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OctopusForegroundService::class.java))
        }

        private fun ContextCompat_startForeground(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
