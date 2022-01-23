package com.udacity.project4.locationreminders.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.delay

class ReminderLocalDataSource internal constructor(
    private val remindersDao: RemindersDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReminderDataSource {

    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    override suspend fun getReminders(): Result<List<ReminderDTO>> = withContext(ioDispatcher) {
        return@withContext try {
            Result.Success(remindersDao.getReminders())
        } catch (e: Exception) {
            Result.Error(e.toString())
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersDao.saveReminder(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> = withContext(ioDispatcher) {
        try {
            val task = remindersDao.getReminderById(id)
            if (task != null) {
                return@withContext Result.Success(task)
            } else {
                return@withContext Result.Error("Task not found!")
            }
        } catch (e: Exception) {
            return@withContext Result.Error(e.toString())
        }
    }

    override suspend fun deleteAllReminders() {
        remindersDao.deleteAllReminders()
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