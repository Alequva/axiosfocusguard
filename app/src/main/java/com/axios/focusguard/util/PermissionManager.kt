package com.axios.focusguard.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.axios.focusguard.service.FocusAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appLaunchTime = System.currentTimeMillis()

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${context.packageName}/${FocusAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        var isEnabled = false
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                isEnabled = true
                break
            }
        }

        // Clear heartbeat if explicitly disabled
        if (!isEnabled) {
            context.getSharedPreferences("service_health", Context.MODE_PRIVATE)
                .edit().remove("last_heartbeat").apply()
        }

        return isEnabled
    }

    /**
     * Checks if the accessibility service process is actually connected and receiving events.
     * Use this to check if the timer is allowed to start. No grace period here.
     */
    fun isServiceTrulyActive(): Boolean {
        if (!isAccessibilityServiceEnabled()) return false
        if (!FocusAccessibilityService.isServiceRunning) return false
        
        val prefs = context.getSharedPreferences("service_health", Context.MODE_PRIVATE)
        val lastHeartbeat = prefs.getLong("last_heartbeat", 0)
        val currentTime = System.currentTimeMillis()
        
        // If last heartbeat was more than 10 seconds ago, service is likely dead
        return (currentTime - lastHeartbeat) < 10000
    }

    /**
     * Checks if the service is stalled (enabled in settings but process not active).
     * Includes a 15s grace period from app launch to hide the warning during startup.
     */
    fun isServiceStalled(): Boolean {
        if (!isAccessibilityServiceEnabled()) return false
        
        // During grace period (15s from app launch), we assume it's starting
        if (System.currentTimeMillis() - appLaunchTime < 15000) {
            return false
        }

        return !isServiceTrulyActive()
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
