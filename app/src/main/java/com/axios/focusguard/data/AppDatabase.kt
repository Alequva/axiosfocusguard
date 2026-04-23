package com.axios.focusguard.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BlockedApp::class, SessionEvent::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun sessionEventDao(): SessionEventDao
}
