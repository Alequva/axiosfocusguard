package com.axios.focusguard.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BlockedApp::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
}
