package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.android.synthetic.main.activity_reminders.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

import android.R
import androidx.test.espresso.Espresso

import androidx.test.espresso.Espresso.onView





@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
/*
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(ViewMatchers.withId(com.udacity.project4.R.id.addReminderFAB)).perform(ViewActions.click())
        activityScenario.close()
*/
    }

    //    TODO: add End to End testing to the app
    @Test
    fun snackBarTest() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(ViewMatchers.withId(com.udacity.project4.R.id.addReminderFAB)).perform(ViewActions.click())
        onView(isRoot()).perform(waitFor(5000))
        onView(ViewMatchers.withId(com.udacity.project4.R.id.saveReminder)).perform(ViewActions.click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Please enter title")))
        activityScenario.close()
    }

    @Test
    fun toastTest() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(ViewMatchers.withId(com.udacity.project4.R.id.addReminderFAB)).perform(ViewActions.click())
        onView(ViewMatchers.withId(com.udacity.project4.R.id.reminderTitle)).perform(ViewActions.typeText("Test"))
        onView(ViewMatchers.withId(com.udacity.project4.R.id.reminderDescription)).perform(ViewActions.typeText("Description"))
        onView(ViewMatchers.withId(com.udacity.project4.R.id.selectLocation)).perform(ViewActions.click())
        onView(isRoot()).perform(waitFor(5000))
        onView(ViewMatchers.withId(com.udacity.project4.R.id.proceed)).perform(ViewActions.click())
        onView(isRoot()).perform(waitFor(2000))
        onView(withText("Reminder Location"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })
        onView(ViewMatchers.withId(com.udacity.project4.R.id.saveReminder)).perform(ViewActions.click())
        onView(isRoot()).perform(waitFor(2000))

//        onView(withText(R.string.reminder_location)).inRoot(ToastMatcher())
//            .check(matches(isDisplayed()))
//        onView(withId(com.google.android.material.R.id.snackbar_text))
//            .check(ViewAssertions.matches(withText(R.string.err_select_location)))
        activityScenario.close()
    }

    // get activity context
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }


    @Test
    fun shouldReturnError() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(ViewMatchers.withId(com.udacity.project4.R.id.addReminderFAB)).perform(ViewActions.click())
        onView(isRoot()).perform(waitFor(5000))
        onView(withId(com.udacity.project4.R.id.saveReminder)).perform(ViewActions.click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(
            ViewAssertions.matches(
                ViewMatchers.withText("Please enter title")
            )
        )
        onView(ViewMatchers.withId(com.udacity.project4.R.id.reminderTitle)).perform(
            ViewActions.typeText(
                "Test"
            )
        )
        onView(ViewMatchers.isRoot()).perform(ViewActions.closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(8000))
        onView(withId(com.udacity.project4.R.id.saveReminder)).perform(ViewActions.click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(
            ViewAssertions.matches(
                ViewMatchers.withText("Please select location")
            )
        )
        activityScenario.close()
    }

    fun waitFor(delay: Long): ViewAction? {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "wait for $delay milliseconds"
            override fun perform(uiController: UiController, v: View?) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

}
