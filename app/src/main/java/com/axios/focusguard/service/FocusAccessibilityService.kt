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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            
            val packageName = event.packageName?.toString() ?: return
            
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
                        Log.w("FocusGuard", "BLOCKED: $packageName. Redirecting to Axios...")
                        
                        // M5: Track the violation
                        focusManager.currentSessionId.value?.let { sessionId ->
                            repository.logSessionEvent(
                                sessionId = sessionId,
                                packageName = packageName,
                                appName = blockedApp.appName
                            )
                        }
                        
                        blockApp()
                    }
                }
            }
        }
    }

    private fun blockApp() {
        val intent = packageManager.getLaunchIntentForPackage(this.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (intent != null) {
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}
