package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsNull
import org.hamcrest.text.IsEmptyString
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    //provide testing to the SaveReminderView and its live data objects
    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    //Rule related to livedata test
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing
    //reference https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-survey#3
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setViewModel() {
        fakeDataSource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(getApplicationContext(), fakeDataSource)
    }

    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun onClear_clearReminder() {
        saveReminderViewModel.apply {
            reminderTitle.value = "title"
            reminderDescription.value = "description"
            reminderSelectedLocationStr.value = "location"
            latitude.value = 100.123
            longitude.value = 200.123
            selectedPOI.value =
                PointOfInterest(LatLng(latitude.value!!, longitude.value!!), "id", "name")
        }
        saveReminderViewModel.onClear()

        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(),
            IsEmptyString.isEmptyOrNullString())
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(),
            IsEmptyString.isEmptyOrNullString())
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            IsEmptyString.isEmptyOrNullString())
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(IsNull.nullValue()))
    }

    @Test
    fun validateEnteredData_titleIsNullOrEmpty() {
        val reminderTitleIsNull = ReminderDataItem(
            null,
            description = "description",
            location = "location",
            latitude = 100.123,
            longitude = 200.123)

        val reminderWithoutTitle = ReminderDataItem(
            "",
            description = "description",
            location = "location",
            latitude = 100.123,
            longitude = 200.123)

        saveReminderViewModel.validateEnteredData(reminderTitleIsNull)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title))

        saveReminderViewModel.validateEnteredData(reminderWithoutTitle)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_locationIsNullOrEmpty() {
        val reminderLocationIsNull = ReminderDataItem(
            title = "title",
            description = "description",
            location = null,
            latitude = 100.123,
            longitude = 200.123)

        val reminderLocationIsEmpty = ReminderDataItem(
            title = "title",
            description = "description",
            location = "",
            latitude = 100.123,
            longitude = 200.123)

        saveReminderViewModel.validateEnteredData(reminderLocationIsNull)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location))

        saveReminderViewModel.validateEnteredData(reminderLocationIsEmpty)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_saveReminderToDataSource() {
        // reference https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-survey#4
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()
        val reminder = ReminderDataItem(
            title = "title",
            description = "description",
            location = "location",
            latitude = 100.123,
            longitude = 200.123)
        saveReminderViewModel.saveReminder(reminder)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(saveReminderViewModel.app.getString(R.string.reminder_location)))
    }
}

