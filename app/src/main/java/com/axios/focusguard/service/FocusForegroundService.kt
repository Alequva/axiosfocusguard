package com.axios.focusguard.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.axios.focusguard.MainActivity
import com.axios.focusguard.R
import com.axios.focusguard.data.AppRepository
import com.axios.focusguard.data.FocusManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class FocusForegroundService : Service() {

    @Inject
    lateinit var repository: AppRepository

    @Inject
    lateinit var focusManager: FocusManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var pollingJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "focus_guard_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure service stays alive
        return START_STICKY
    }

    private var blockedAppsCache: Set<String> = emptySet()

    private fun startPolling() {
        // Cache blocked apps periodically
        serviceScope.launch {
            repository.getBlockedApps().collect { apps ->
                blockedAppsCache = apps.map { it.packageName }.toSet()
            }
        }

        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                if (focusManager.isFocusActive) {
                    checkForegroundApp()
                    delay(200) // Poll every 200ms during focus for more aggressive blocking
                } else {
                    delay(2000) // Lower frequency when idle
                }
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val packageName = getTopPackageName() ?: return
        
        if (packageName == this.packageName || 
            packageName == "com.android.systemui" || 
            packageName == "android" ||
            packageName.contains("launcher") ||
            packageName.contains("settings")) { // Don't block settings so user can't lock themselves out
            return
        }

        if (blockedAppsCache.contains(packageName)) {
            Log.w("FocusGuard", "BLOCKED via UsageStats: $packageName")
            
            focusManager.currentSessionId.value?.let { sessionId ->
                val timerState = focusManager.uiState.value
                val offset = timerState.initialSessionSeconds - timerState.timeLeftSeconds
                val category = repository.getCategory(packageName).name
                val appName = repository.getInstalledApps().find { it.packageName == packageName }?.name ?: packageName

                repository.logSessionEvent(
                    sessionId = sessionId,
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    offsetSeconds = offset
                )
            }
            
            redirectToApp()
        }
    }

    private fun getTopPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        
        // Use UsageEvents for higher precision than queryUsageStats
        val events = usageStatsManager.queryEvents(time - 2000, time)
        val event = android.app.usage.UsageEvents.Event()
        var lastPackageName: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackageName = event.packageName
            }
        }
        
        return lastPackageName ?: usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 5,
            time
        )?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun redirectToApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Guard")
            .setContentText("Focus session is active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Guard Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Used to keep Focus Guard active during sessions"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
