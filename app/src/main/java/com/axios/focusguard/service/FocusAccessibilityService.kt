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

    // M5: Debounce tracking to prevent multiple logs for a single app opening
    private var lastLoggedPackage: String? = null
    private var lastLogTime: Long = 0

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
                        val currentTime = System.currentTimeMillis()
                        
                        // Only log if it's a different app or enough time has passed (2 seconds)
                        if (packageName != lastLoggedPackage || (currentTime - lastLogTime) > 2000) {
                            Log.w("FocusGuard", "BLOCKED: $packageName. Redirecting to Axios...")
                            
                            focusManager.currentSessionId.value?.let { sessionId ->
                                repository.logSessionEvent(
                                    sessionId = sessionId,
                                    packageName = packageName,
                                    appName = blockedApp.appName
                                )
                            }
                            
                            lastLoggedPackage = packageName
                            lastLogTime = currentTime
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
