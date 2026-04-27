package com.axios.focusguard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val isBlocked: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory {
    SOCIAL, VIDEO, GAME, OTHER
}

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedAppDao: BlockedAppDao,
    private val sessionEventDao: SessionEventDao,
    private val focusSessionDao: FocusSessionDao
) {
    fun getBlockedApps(): Flow<List<BlockedApp>> = blockedAppDao.getAllBlockedApps()

    suspend fun toggleAppBlock(packageName: String, appName: String, shouldBlock: Boolean) {
        if (shouldBlock) {
            blockedAppDao.insert(BlockedApp(packageName, appName))
        } else {
            blockedAppDao.delete(BlockedApp(packageName, appName))
        }
    }

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isPopularDistractor(it.packageName) }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    name = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app),
                    category = getCategory(app)
                )
            }
            .sortedWith(compareBy({ it.category }, { it.name }))
    }

    private fun getCategory(app: ApplicationInfo): AppCategory {
        return when {
            app.category == ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
            app.category == ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            socialPackages.contains(app.packageName) -> AppCategory.SOCIAL
            videoPackages.contains(app.packageName) -> AppCategory.VIDEO
            else -> AppCategory.OTHER
        }
    }

    fun isPopularDistractor(packageName: String): Boolean {
        return socialPackages.contains(packageName) || videoPackages.contains(packageName)
    }

    fun getCategory(packageName: String): AppCategory {
        return when {
            socialPackages.contains(packageName) -> AppCategory.SOCIAL
            videoPackages.contains(packageName) -> AppCategory.VIDEO
            else -> AppCategory.OTHER
        }
    }

    private val socialPackages = setOf(
        "com.zhiliaoapp.musically", "com.instagram.android", "com.facebook.katana",
        "com.twitter.android", "com.snapchat.android", "com.whatsapp", "com.reddit.frontpage"
    )

    private val videoPackages = setOf(
        "com.google.android.youtube", "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
        "com.hulu.plus", "com.disney.disneyplus", "org.videolan.vlc"
    )

    suspend fun logSessionEvent(
        sessionId: String, 
        packageName: String, 
        appName: String, 
        category: String, 
        offsetSeconds: Int
    ) {
        val lastEvent = sessionEventDao.getLastEventForApp(sessionId, packageName)
        val currentTime = System.currentTimeMillis()
        
        // Anti-transition filter (800ms):
        // Logs every individual tap as a raw attempt, while filtering internal
        // app transitions. Burst grouping is handled at the UI/AI level.
        if (lastEvent == null || (currentTime - lastEvent.timestamp) > 800) {
            sessionEventDao.insertEvent(
                SessionEvent(
                    sessionId = sessionId, 
                    packageName = packageName, 
                    appName = appName,
                    category = category,
                    sessionOffsetSeconds = offsetSeconds
                )
            )
        }
    }

    fun getEventsForSession(sessionId: String): Flow<List<SessionEvent>> = sessionEventDao.getEventsForSession(sessionId)

    suspend fun getSessionById(sessionId: String): FocusSession? = focusSessionDao.getSessionById(sessionId)

    suspend fun saveFocusSession(session: FocusSession) {
        focusSessionDao.insertSession(session)
    }
}
