package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.Event
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.FakeReminderTestRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.EspressoIdlingResource
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock

@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {
    // provide testing to the SaveReminderView and its live data objects
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var tasksRepository: FakeReminderTestRepository

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        // We initialise the repository with no tasks
        tasksRepository = FakeReminderTestRepository()
        val task1 = ReminderDTO("Title1", "Description1", "22.741, 88.697", 22.741, 88.697)
        val task2 = ReminderDTO("Title2", "Description2", "22.78945, 88.14263", 22.78945, 88.14263)
        val task3 = ReminderDTO("Title3", "Description3", "22.3145, 88.6412", 22.3145, 88.6412)
        tasksRepository.addReminders(task1, task2, task3)
        saveReminderViewModel = SaveReminderViewModel(Application(), tasksRepository)
    }

    @Test
    fun addNewReminder_setsNewReminderEvent() {
        saveReminderViewModel.addNewTask()
        val value = saveReminderViewModel.newTaskEvent.getOrAwaitValue()
        MatcherAssert.assertThat(
            value.getContentIfNotHandled(),
            CoreMatchers.not(CoreMatchers.nullValue())
        )
    }

    @Test
    fun loadReminderWhenRemindersAreUnavailable_callErrorToDisplay() {
        tasksRepository.setReturnError(true)
        saveReminderViewModel.refresh()
        assertTrue(saveReminderViewModel.empty.getOrAwaitValue())
        assertTrue(saveReminderViewModel.error.getOrAwaitValue())
    }

    @Test
    fun loadReminders_loading() {
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.refresh()
        assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue() == false)
    }

}