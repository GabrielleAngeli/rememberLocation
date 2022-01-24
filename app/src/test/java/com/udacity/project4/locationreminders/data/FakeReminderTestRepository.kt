package com.udacity.project4.locationreminders.data

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.LinkedHashMap

class FakeReminderTestRepository : ReminderDataSource {
    var reminderData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private lateinit var tasksRemoteDataSource: ReminderDataSource
    private val task1 =
        ReminderDTO("Title1", "Description1", "location1", 22.578, 88.6048, "ID1")
    private val task2 =
        ReminderDTO("Title2", "Description2", "location2", 22.59878, 88.7848, "ID2")
    private val remoteReminder = listOf(task1, task2).sortedBy { it.id }
    private lateinit var viewModel: SaveReminderViewModel
    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    @Before
    fun createRepository() {
        tasksRemoteDataSource = FakeDataSource(remoteReminder.toMutableList())
        viewModel = SaveReminderViewModel(Application(), tasksRemoteDataSource)
    }

    private var shouldReturnError = false
    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        return Result.Success(reminderData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        reminderData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error("Could not find reminder")
    }

    override suspend fun deleteAllReminders() {
        reminderData.clear()
    }

    fun addReminders(vararg tasks: ReminderDTO) {
        for (task in tasks) {
            reminderData[task.id] = task
        }
        runBlocking { getReminders() }
    }

    @Test
    fun getReminder_requestsAllRemindersFromDataSource() = runBlockingTest {
        // When tasks are requested from the tasks repository
        val tasks = tasksRemoteDataSource.getReminders() as Result.Success
        // Then tasks are loaded from the remote data source
        Assert.assertThat(tasks.data, IsEqual(remoteReminder))
    }

    override suspend fun refreshReminders() {
        observableReminders.value = getReminders()
    }

    override suspend fun refreshReminder(id: String) {
        refreshReminders()
    }

    override suspend fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        return observableReminders
    }

    override suspend fun observeTask(reminderId: String): LiveData<Result<ReminderDTO>> {
        return observableReminders.map { result ->
            when (result) {
                is Result.Success -> {
                    Result.Success(result.data.first { it.id == reminderId })
                }
                is Result.Error -> {
                    Result.Error(result.message)
                }
                else -> Result.Error("")
            }
        }

    }
}