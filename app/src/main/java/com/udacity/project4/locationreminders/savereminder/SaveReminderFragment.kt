package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context.LOCATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.TimeUnit


class SaveReminderFragment : BaseFragment() {
    private val REQUEST_CODE = 200
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    private val LOCATION_PERMISSION_INDEX = 0
    private var googleApiClient: GoogleApiClient? = null
    private val REQUEST_CODE_LOCATION_SETTING = 152
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)


    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    override fun styleMap(mapTypeNormal: String) {
        TODO("Not yet implemented")
    }

    override fun askUserForMarkerOrPOI(latLng: LatLng, zoom: Float, title: String) {
        TODO("Not yet implemented")
    }

    override fun addGeofenceForClue() {
        val title =
            if (_viewModel.reminderTitle.value.isNullOrEmpty()) binding.reminderTitle.text.toString()
                .trim() else _viewModel.reminderTitle.value
        val description =
            if (_viewModel.reminderDescription.value.isNullOrEmpty()) binding.reminderDescription.text.toString()
                .trim() else _viewModel.reminderDescription.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val location = if (latitude != null && longitude != null) "$latitude:$longitude" else ""
        dataItem = ReminderDataItem(
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude
        )
        setUpGeofence(dataItem)
        _viewModel.validateAndSaveReminder(dataItem)
    }


    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var geofencingClient: GeofencingClient
    val geofenceList = arrayListOf<Geofence>()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        lateinit var dataItem: ReminderDataItem
        val REQUESTLOCATION = 199
        var gpsStatus = MutableLiveData<Boolean>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        //setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            /*if (!statusCheck()) {
                enableLoc()
            } else {*/
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
            //  }
        }

        binding.saveReminder.setOnClickListener {

//            use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            checkPermissionsAndStartGeofencing()
            /*setUpGeofence(dataItem)
            _viewModel.validateAndSaveReminder(dataItem)*/
        }
        _viewModel.latitude.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (getRuntimePermissions())
                    binding.selectedLocation.append(" Latitude: $it ")
                else {
                    Snackbar.make(binding.root, "Please check your location", Snackbar.LENGTH_SHORT)
                        .show()
//                    buildAlertMessageNoGps()
                }
            }
        })
        _viewModel.longitude.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (getRuntimePermissions())
                    binding.selectedLocation.append(" Longitude: $it ")
                else {
                    Snackbar.make(binding.root, "Please check your location", Snackbar.LENGTH_SHORT)
                        .show()
//                    buildAlertMessageNoGps()
                }
            }
        })
    }

    private fun setUpGeofence(data: ReminderDataItem) {
        geofenceList.add(
            Geofence.Builder()
                .setRequestId(data.id)
                .setCircularRegion(data.latitude ?: 0.0, data.longitude ?: 0.0, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
                .build()
        )
        // checkPermissionsAndStartGeofencing()
        initiateGeofenceRequest()
    }

    @SuppressLint("MissingPermission")
    private fun initiateGeofenceRequest() {
        if (!binding.reminderTitle.text.isNullOrEmpty() && !binding.reminderDescription.text.isNullOrEmpty()) {
            geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("TAG", "initiateGeofenceRequest: ")
                    navigationCommand.value = NavigationCommand.Back
                }
                .addOnFailureListener {
                    Log.d("TAG", "initiateGeofenceRequest: ${it.message}")
                    navigationCommand.value = NavigationCommand.Back
                }
        } else {
            Log.d("TAG", "title and description are empty")
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        //  removeGeofences()
    }

    private fun getRuntimePermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ), REQUEST_CODE
                )
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQUEST_CODE
                )
            }
            return false
        } else {
            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        /* if (requestCode == REQUEST_CODE && permissions.isNotEmpty()) {
             if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                 val rational =
                     shouldShowRequestPermissionRationale(permissions[0]) && shouldShowRequestPermissionRationale(
                         permissions[1]
                     )
                 if (!rational) {
                     AlertDialog.Builder(requireActivity())
                         .setTitle("Permission required!")
                         .setMessage("This permission is essential to proceed further.")
                         .setPositiveButton("OK", object : DialogInterface.OnClickListener {
                             override fun onClick(dialog: DialogInterface?, which: Int) {
                                 val intent = Intent()
                                 intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                 val uri: Uri =
                                     Uri.fromParts("package", requireActivity().packageName, null)
                                 intent.data = uri
                                 startActivity(intent)
                             }
                         }).setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                             override fun onClick(dialog: DialogInterface?, which: Int) {
                                 dialog?.dismiss()
                             }

                         }).show()
                 } else {
                     getRuntimePermissions()
                 }
             }
         } else*/
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // Permission denied.
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }

    }

    fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, id: Int) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
            .setNegativeButton("No", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.cancel()
                }
            })
        val alert: AlertDialog = builder.create()
        alert.show()
    }

/*
    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofencing()
    }
*/

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d("TAG", "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    fun statusCheck(): Boolean {
        val manager: LocationManager =
            (requireActivity() as RemindersActivity).getSystemService(LOCATION_SERVICE) as LocationManager
        gpsStatus.value = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    /* exception.startResolutionForResult(
                         requireActivity(),
                         REQUEST_TURN_DEVICE_LOCATION_ON
                     )*/

                    startIntentSenderForResult(
                        geofencePendingIntent.intentSender,
                        REQUEST_CODE_LOCATION_SETTING,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("TAG", "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForClue()
            }
        }
    }

    private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                Log.d("TAG", getString(R.string.geofences_removed))
                Toast.makeText(requireContext(), R.string.geofences_removed, Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.d("TAG", getString(R.string.geofences_not_removed))
            }
        }
    }

    private fun enableLoc() {
        googleApiClient = GoogleApiClient.Builder(requireActivity())
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {}
                override fun onConnectionSuspended(i: Int) {
                    googleApiClient?.connect()
                }
            })
            .addOnConnectionFailedListener {
            }.build()
        googleApiClient?.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000.toLong()
        locationRequest.fastestInterval = 5 * 1000.toLong()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result: PendingResult<LocationSettingsResult> =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result ->
            val status: Status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    status.startResolutionForResult(
                        requireActivity(),
                        REQUESTLOCATION
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.d("TAG", "error: ${e.message}")
                }
            }
        }
    }
}
