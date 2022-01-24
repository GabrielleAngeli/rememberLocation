package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // provide testing to the RemindersListViewModel and its live data objects
    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Rule related to livedata test
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing
    // reference https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-survey#3
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setViewModel() {
        fakeDataSource = FakeDataSource()
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun loadReminders_success() {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderDTO3 = ReminderDTO("title3", "des3", "loc3", 3.00, 3.00, "3")
        val dataListDTO = mutableListOf(reminderDTO1, reminderDTO2, reminderDTO3)

        val reminderReminderDataItem1 = ReminderDataItem("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderReminderDataItem2 = ReminderDataItem("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderReminderDataItem3 = ReminderDataItem("title3", "des3", "loc3", 3.00, 3.00, "3")
        val dataListDataItem =
            listOf(reminderReminderDataItem1, reminderReminderDataItem2, reminderReminderDataItem3)
        fakeDataSource.reminders = dataListDTO

        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.remindersList.value, IsEqual(dataListDataItem))
    }

    @Test
    fun loadReminders_error() {
        fakeDataSource.shouldReturnError(true)
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Set result error"))
    }

    @Test
    fun loadReminders_fail() {
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}