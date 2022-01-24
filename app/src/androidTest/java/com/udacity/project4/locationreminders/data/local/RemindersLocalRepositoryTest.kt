package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.matchers.Null
import org.junit.rules.ExpectedException

import org.junit.Rule
import android.content.res.Resources.NotFoundException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    // Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun createRepository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        database.clearAllTables()
        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Unconfined)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    // runBlocking is used to wait for all suspend functions to finish before continuing with the execution in the block.
    // Note that we're using runBlocking instead of runBlockingTest because of a bug(https://github.com/Kotlin/kotlinx.coroutines/issues/1204).
    fun getReminder_requestReminderFromDatabaseSuccess() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        remindersLocalRepository.saveReminder(reminderDTO1)
        val reminder1 = remindersLocalRepository.getReminder("1") as Result.Success<ReminderDTO>

        assertThat(reminder1.data == reminderDTO1, `is`(true))
    }

    @Test
    // runBlocking is used to wait for all suspend functions to finish before continuing with the execution in the block.
    // Note that we're using runBlocking instead of runBlockingTest because of a bug(https://github.com/Kotlin/kotlinx.coroutines/issues/1204).
    fun getReminders_requestRemindersFromDatabaseSuccess() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderDTO3 = ReminderDTO("title3", "des3", "loc3", 3.00, 3.00, "3")

        remindersLocalRepository.saveReminder(reminderDTO1)
        remindersLocalRepository.saveReminder(reminderDTO2)
        remindersLocalRepository.saveReminder(reminderDTO3)

        val reminders = remindersLocalRepository.getReminders() as Result.Success

        assertThat(reminders.data[0] == reminderDTO1, `is`(true))
        assertThat(reminders.data[1] == reminderDTO2, `is`(true))
        assertThat(reminders.data[2] == reminderDTO3, `is`(true))
    }

    @Test
    fun getReminder_requestReminderFromDatabaseException() = runBlocking {
        val reminder = remindersLocalRepository.getReminder("1") as Result.Error
        assertThat(reminder, `is`(Result.Error("Reminder not found!")))
    }

    @Test
    fun getReminders_requestRemindersFromDatabaseException() = runBlocking {
        val reminder = remindersLocalRepository.getReminder("1") as Result.Error
        assertThat(reminder, `is`(Result.Error("Reminder not found!")))
    }

    @Test
    fun saveReminder_reminderSaved() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        remindersLocalRepository.saveReminder(reminderDTO1)
        val reminder = remindersLocalRepository.getReminder("1") as Result.Success<ReminderDTO>
        assertThat(reminder.data == reminderDTO1, `is`(true))
    }

    @Test
    // runBlocking is used to wait for all suspend functions to finish before continuing with the execution in the block.
    // Note that we're using runBlocking instead of runBlockingTest because of a bug(https://github.com/Kotlin/kotlinx.coroutines/issues/1204).
    fun deleteAllReminders_noRemindersLeft() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderDTO3 = ReminderDTO("title3", "des3", "loc3", 3.00, 3.00, "3")

        remindersLocalRepository.saveReminder(reminderDTO1)
        remindersLocalRepository.saveReminder(reminderDTO2)
        remindersLocalRepository.saveReminder(reminderDTO3)

        remindersLocalRepository.deleteAllReminders()

        val reminder = remindersLocalRepository.getReminder("1") as Result.Error
        assertThat(reminder, `is`(Result.Error("Reminder not found!")))
    }
}