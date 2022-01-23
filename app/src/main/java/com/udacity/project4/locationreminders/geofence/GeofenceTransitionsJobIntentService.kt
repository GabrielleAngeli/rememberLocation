package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.lang.Exception
import kotlin.coroutines.CoroutineContext


class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMsg: String = getErrorString(geofencingEvent.errorCode)
            Log.e("TAG", errorMsg)
            return
        }
        val geoFenceTransition = geofencingEvent.geofenceTransition
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
            geoFenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails: String =
                getGeofenceTrasitionDetails(geoFenceTransition, triggeringGeofences)
            sendNotification(triggeringGeofences)
        }
    }

    private fun getGeofenceTrasitionDetails(
        geoFenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val triggeringGeofencesList: ArrayList<String?> = ArrayList()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesList.add(geofence.requestId)
        }

        var status: String? = null
        if (geoFenceTransition === Geofence.GEOFENCE_TRANSITION_ENTER) status =
            "Entering " else if (geoFenceTransition === Geofence.GEOFENCE_TRANSITION_EXIT) status =
            "Exiting "
        return status + TextUtils.join(", ", triggeringGeofencesList)

    }

    // get the request id of the current geofence
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        for(i in triggeringGeofences) {
            val requestId = i.requestId
            Log.d("TAG", "sendNotification loading: $requestId")
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                try {
                    val result = RemindersDatabase.getDatabase(applicationContext).reminderDao()
                        .getReminderById(requestId) //.getReminderById(requestId)
                    Log.d("TAG", "sendNotification success: ${result?.title}")
                    if (result is ReminderDTO) {
                        //send a notification to the user with the reminder details
                        Log.d("TAG", "sendNotification inside: $requestId")
                        sendNotification(
                            this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                                result.title,
                                result.description,
                                result.location,
                                result.latitude,
                                result.longitude,
                                result.id
                            )
                        )
                    } else {
                        sendNotification(
                            this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                                "No Data",
                                "No Data",
                                "No Data",
                                0.0,
                                0.0,
                                "No Data"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.d("TAG", "sendNotification failure: ${e.message}")
                }
            }
        }
    }


    private fun getErrorString(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "GeoFence not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many GeoFences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
            else -> "Unknown error."
        }
    }
}
