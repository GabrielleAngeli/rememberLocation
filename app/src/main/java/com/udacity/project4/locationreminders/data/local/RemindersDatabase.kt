package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

/**
 * The Room Database that contains the reminders table.
 */
@Database(entities = [ReminderDTO::class], version = 1, exportSchema = false)
abstract class RemindersDatabase : RoomDatabase() {

    abstract fun reminderDao(): RemindersDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: RemindersDatabase? = null

        fun getDatabase(context: Context): RemindersDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemindersDatabase::class.java,
                    "locationReminders.db"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}