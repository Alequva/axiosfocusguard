package com.axios.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.axios.focusguard.data.AppRepository
import com.axios.focusguard.data.FocusManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var repository: AppRepository

    @Inject
    lateinit var focusManager: FocusManager

    companion object {
        var isServiceRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val packageName = event.packageName?.toString() ?: return
            
            // Log for debugging
            Log.v("FocusGuard", "Event from: $packageName")

            if (packageName == this.packageName || 
                packageName == "com.android.systemui" || 
                packageName == "android" ||
                packageName.contains("launcher")) {
                return
            }

            serviceScope.launch {
                val isFocusActive = focusManager.isFocusActive
                
                if (isFocusActive) {
                    val blockedApps = repository.getBlockedApps().first()
                    val blockedApp = blockedApps.find { it.packageName == packageName }
                    
                    if (blockedApp != null) {
                        Log.w("FocusGuard", "BLOCKED: $packageName")
                        
                        focusManager.currentSessionId.value?.let { sessionId ->
                            val timerState = focusManager.uiState.value
                            val offset = timerState.initialSessionSeconds - timerState.timeLeftSeconds
                            val category = repository.getCategory(packageName).name

                            repository.logSessionEvent(
                                sessionId = sessionId,
                                packageName = packageName,
                                appName = blockedApp.appName,
                                category = category,
                                offsetSeconds = offset
                            )
                        }
                        
                        blockApp()
                    }
                } else {
                    // Log why we are not blocking if it's a known distractor
                    if (repository.isPopularDistractor(packageName)) {
                        Log.d("FocusGuard", "App $packageName is a distractor but focus is not active.")
                    }
                }
            }
        }
    }

    private fun blockApp() {
        // First, return to home screen (Forceful close effect)
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Second, show our blocker activity after a slight delay to allow home transition
        serviceScope.launch {
            kotlinx.coroutines.delay(300)
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (intent != null) {
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
