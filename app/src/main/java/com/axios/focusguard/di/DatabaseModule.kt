package com.axios.focusguard.di

import android.content.Context
import androidx.room.Room
import com.axios.focusguard.data.AppDatabase
import com.axios.focusguard.data.BlockedAppDao
import com.axios.focusguard.data.SessionEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "focus_guard_db"
        ).fallbackToDestructiveMigration() // Useful during development
        .build()
    }

    @Provides
    fun provideBlockedAppDao(database: AppDatabase): BlockedAppDao {
        return database.blockedAppDao()
    }

    @Provides
    fun provideSessionEventDao(database: AppDatabase): SessionEventDao {
        return database.sessionEventDao()
    }
}
