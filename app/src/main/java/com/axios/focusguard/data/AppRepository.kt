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
    val isBlocked: Boolean = false
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedAppDao: BlockedAppDao
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
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps for now
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    name = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app)
                )
            }
            .sortedBy { it.name }
    }
}
