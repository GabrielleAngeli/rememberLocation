package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.ServiceLocator
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest()  {
    //    test the navigation of the fragments.
//    test the displayed data on the UI.
//    add testing for the error messages.
    private val dataSource: ReminderDataSource by inject()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun startKoinForTestAndInitRepository() {
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module {
                    viewModel {
                        RemindersListViewModel(
                            ApplicationProvider.getApplicationContext(),
                            get()
                        )
                    }
                    single {
                        SaveReminderViewModel(
                            ApplicationProvider.getApplicationContext(),
                            get()
                        )
                    }
                    single {
                        Room.inMemoryDatabaseBuilder(
                            ApplicationProvider.getApplicationContext(),
                            RemindersDatabase::class.java).allowMainThreadQueries()
                            .build() as RemindersDao
                    }

                    single {
                        RemindersLocalRepository(get())
                    }

                    single {
                        FakeAndroidTestDataSource() as ReminderDataSource
                    }
                }
            )
        }
    }

    @After
    fun cleanupDb() = runBlocking {
        ServiceLocator.resetRepository()
        stopKoin()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun reminderList_displayedInUi() = runBlockingTest {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        dataSource.saveReminder(reminderDTO1)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withText("title1")).check(matches(isDisplayed()))
    }

    @Test
    fun reminderListEmpty_displayedInUi() = runBlockingTest {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun addReminderFABClick_displaySaveReminderFragment() = runBlockingTest {
        val scenario = launchFragmentInContainer<ReminderListFragment> (Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
}