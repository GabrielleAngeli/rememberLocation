package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    //    Add testing implementation to the RemindersDao.kt
    private lateinit var db: RemindersDatabase

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun saveReminderAndGetReminderById() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        db.reminderDao().saveReminder(reminderDTO1)
        val retrieveReminder = db.reminderDao().getReminderById("1")

        assertThat(retrieveReminder?.title == reminderDTO1.title, `is`(true))
        assertThat(retrieveReminder?.description == reminderDTO1.description, `is`(true))
        assertThat(retrieveReminder?.latitude == reminderDTO1.latitude, `is`(true))
        assertThat(retrieveReminder?.longitude == reminderDTO1.longitude, `is`(true))
        assertThat(retrieveReminder?.id == reminderDTO1.id, `is`(true))
    }

    @Test
    fun getRemindersAndDeleteAllReminders() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        db.reminderDao().saveReminder(reminderDTO1)
        db.reminderDao().saveReminder(reminderDTO2)
        val reminders = db.reminderDao().getReminders()
        assertThat(reminders[0] == reminderDTO1, `is`(true))
        assertThat(reminders[1] == reminderDTO2, `is`(true))

        db.reminderDao().deleteAllReminders()
        val emptyDatabase = db.reminderDao().getReminders()
        assertThat(emptyDatabase, `is`(emptyList()))
    }
}