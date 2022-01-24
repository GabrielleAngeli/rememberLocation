package com.udacity.project4.locationreminders.data.local

import androidx.lifecycle.LiveData
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidTestDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    //    Create a fake data source to act as a double to the real data source
    var shouldReturnError = false

    fun shouldReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error("Set result error", 410)
        } else {
            Result.Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("Set result error", 410)
        } else {
            val reminder: ReminderDTO? = reminders.find { it.id == id }
            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Not Found!", 404)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    override suspend fun refreshReminders() {
        TODO("Not yet implemented")
    }

    override suspend fun refreshReminder(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        TODO("Not yet implemented")
    }

    override suspend fun observeTask(reminderId: String): LiveData<Result<ReminderDTO>> {
        TODO("Not yet implemented")
    }
}