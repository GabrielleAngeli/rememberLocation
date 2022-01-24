package com.udacity.project4

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.runBlocking

object ServiceLocator {
    private var database: RemindersDatabase? = null
    private val lock = Any()

    @Volatile
    var remindersLocalRepository: ReminderDataSource? = null
        @VisibleForTesting set

    fun provideRemindersLocalRepository(context: Context): ReminderDataSource {
        synchronized(this) {
            return remindersLocalRepository ?: createRemindersLocalRepository(context)
        }
    }

    private fun createRemindersLocalRepository(context: Context): RemindersLocalRepository {
        createDatabase(context)
        val newRepo = RemindersLocalRepository(database!!.reminderDao())
        remindersLocalRepository = newRepo
        return newRepo
    }

    private fun createDatabase(context: Context): RemindersDatabase {
        val newDatabase = Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java, "locationReminders.db"
        ).build()
        database = newDatabase
        return newDatabase
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            runBlocking {
                remindersLocalRepository?.deleteAllReminders()
            }
            // Clear all data to avoid test pollution.
            database?.apply {
                clearAllTables()
                close()
            }
            database = null
            remindersLocalRepository = null
        }
    }
}