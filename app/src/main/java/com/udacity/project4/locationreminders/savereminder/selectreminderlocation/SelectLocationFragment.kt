package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.activity_reminders.*
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment() {

    var mLocationCallback: LocationCallback? = null
    private var googleApiClient: GoogleApiClient? = null


    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    override fun styleMap(mapType: String) {
        when (mapType) {
            "MAP_TYPE_NORMAL" -> {
                try {
                    val success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.normal
                        )
                    )

                    if (!success) {
                        Log.e("TAG", "Style parsing failed.")
                    }
                } catch (e: Resources.NotFoundException) {
                    Log.e("TAG", "Can't find style. Error: ", e)
                }
            }
            "MAP_TYPE_HYBRID" -> {
                try {
                    val success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.hybrid
                        )
                    )

                    if (!success) {
                        Log.e("TAG", "Style parsing failed.")
                    }
                } catch (e: Resources.NotFoundException) {
                    Log.e("TAG", "Can't find style. Error: ", e)
                }
            }
            "MAP_TYPE_SATELLITE" -> {
                try {
                    val success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.satelite
                        )
                    )

                    if (!success) {
                        Log.e("TAG", "Style parsing failed.")
                    }
                } catch (e: Resources.NotFoundException) {
                    Log.e("TAG", "Can't find style. Error: ", e)
                }
            }
            "MAP_TYPE_TERRAIN" -> {
                try {
                    val success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.terrian
                        )
                    )

                    if (!success) {
                        Log.e("TAG", "Style parsing failed.")
                    }
                } catch (e: Resources.NotFoundException) {
                    Log.e("TAG", "Can't find style. Error: ", e)
                }

            }
        }
    }

    override fun askUserForMarkerOrPOI(latLng: LatLng, zoom: Float, title: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Please choose loction adding method").setCancelable(false)
            .setPositiveButton("Marker", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if (statusCheck())
                        moveCamera(latLng, zoom, title)
                    else {
                        enableLoc()
                    }
                }
            }).setNegativeButton("POI", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if (statusCheck())
                        setPoiClick()
                    else {
                        enableLoc()
                    }
                }

            })
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    override fun addGeofenceForClue() {
        TODO("Not yet implemented")
    }

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location = Location("")
    private lateinit var latitude: String
    private lateinit var longitude: String
    private var mapMode: Int = -1
    private val REQUEST_CODE = 200
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    private val LOCATION_PERMISSION_INDEX = 0
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    companion object {
        private const val DEFAULT_ZOOM = 15f
        var isMyLocationSet = false
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback {
        mMap = it
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@OnMapReadyCallback
        } else {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
            fetchLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        /*//add the map setup implementation
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //zoom to the user location after taking his permission
        //add style to the map
        //put a marker to location that the user selected
        //call this function after the user confirms on the selected location
        //  getRuntimePermissions()
        checkPermissions()
        binding.proceed.setOnClickListener {
            if (getRuntimePermissions() && statusCheck())
                onLocationSelected()
            else {
                Snackbar.make(
                    binding.root,
                    "Please allow all the permissions",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun setPoiClick() {
        mMap.setOnPoiClickListener { poi ->
            val poiMarker = mMap.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            userLocation.latitude = poiMarker.position.latitude
            userLocation.longitude = poiMarker.position.longitude
        }
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
            if (statusCheck())
                fetchingUserLocation()
            else
                enableLoc()
            return true
        }
    }


    private fun onLocationSelected() {
        //        When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.latitude.value = userLocation.latitude
        _viewModel.longitude.value = userLocation.longitude
        findNavController().popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            styleMap("MAP_TYPE_NORMAL")
            // mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL)
            true
        }
        R.id.hybrid_map -> {
            styleMap("MAP_TYPE_HYBRID")
            //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID)
            true
        }
        R.id.satellite_map -> {
            styleMap("MAP_TYPE_SATELLITE")
            //  mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE)
            true
        }
        R.id.terrain_map -> {
            styleMap("MAP_TYPE_TERRAIN")
            //mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun fetchingUserLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    try {
                        userLocation = it
                        latitude = it.latitude.toString()
                        longitude = it.longitude.toString()
                        askUserForMarkerOrPOI(
                            LatLng(it.latitude, it.longitude),
                            DEFAULT_ZOOM,
                            "My Location"
                        )
                        //moveCamera(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM, "My Location")
                    } catch (e: Exception) {
                        userLocation = Location("")
                        Log.d("TAG", "Location error ${e.message}")
                    }
                } else {
                    Log.d("TAG", "onComplete: current location is null")
                    view?.let { it1 ->
                        Snackbar.make(
                            it1,
                            "Yours location is not retrieved! Try again.", Snackbar.LENGTH_LONG
                        ).setAction("Retry") {
                            getMyLocation()
                            fetchLocation()
                        }.show()
                    }
                }
            }
        }
    }

    private fun getMyLocation() {
        isMyLocationSet = false
        mMap.setOnMyLocationChangeListener {
            try {
                if (isMyLocationSet)
                    return@setOnMyLocationChangeListener
                isMyLocationSet = true
                userLocation = it
                Log.d("TAG", "current location ${it.latitude} and latitude ${it.longitude}")
                askUserForMarkerOrPOI(
                    LatLng(it.latitude, it.longitude),
                    DEFAULT_ZOOM,
                    "My Location"
                )
                //  moveCamera(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM, "Location")
            } catch (e: Exception) {
                Log.d("TAG", "Location: ${e.message}")
            }
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        mMap.clear()
        Log.d(
            "TAG",
            "moveCamera: moving the camera to: lat: $latLng.latitude, lng: $latLng.longitude"
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        // setPoiClick()
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                Log.d(
                    "TAG",
                    "onMarkerDragEnd: $marker.position.latitude,  $marker.position.longitude"
                )
                // setPoiClick()
                userLocation.latitude = marker.position.latitude
                userLocation.longitude = marker.position.longitude
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        /*if (requestCode == REQUEST_CODE && permissions.isNotEmpty()) {
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
        }*/
        if (grantResults.isEmpty() ||
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
                    //findNavController().popBackStack()
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

    private fun checkPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
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
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
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
                showMap()
            }
        }
    }

    private fun showMap() {
        //add the map setup implementation
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        }
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
            (requireActivity() as RemindersActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    //Fetching current user location.
    fun fetchLocation() {
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.apply {
            this.interval = 60000
            this.fastestInterval = 5000
            this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(
                    "TAG",
                    "Latitude: ${locationResult.lastLocation.latitude} and Longitude: ${locationResult.lastLocation.longitude}"
                )
                try {
                    setPoiClick()
                    isMyLocationSet = true
                    userLocation = locationResult.lastLocation
                    Log.d(
                        "TAG",
                        "current location ${locationResult.lastLocation.latitude} and latitude ${locationResult.lastLocation.longitude}"
                    )
/*
                    askUserForMarkerOrPOI(
                        LatLng(userLocation.latitude, userLocation.longitude),
                        DEFAULT_ZOOM,
                        "My Location"
                    )
*/

                    moveCamera(
                        LatLng(
                            locationResult.lastLocation.latitude,
                            locationResult.lastLocation.longitude
                        ), DEFAULT_ZOOM, "Location"
                    )
                } catch (e: Exception) {
                    Log.d("TAG", "Location: ${e.message}")
                }
                if (isVisible)
                    LocationServices.getFusedLocationProviderClient(requireActivity() as RemindersActivity)
                        .removeLocationUpdates(mLocationCallback)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getRuntimePermissions()
        } else {
            LocationServices.getFusedLocationProviderClient(this.requireActivity())
                .requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    override fun onResume() {
        super.onResume()
        /*if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        }*/
        showMap()
    }

    override fun onDestroyView() {
        mLocationCallback?.let {
            LocationServices.getFusedLocationProviderClient(requireActivity() as RemindersActivity)
                .removeLocationUpdates(mLocationCallback)
        }
        super.onDestroyView()
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
                    if (isVisible)
                        status.startResolutionForResult(
                            requireActivity(),
                            SaveReminderFragment.REQUESTLOCATION
                        )
                } catch (e: IntentSender.SendIntentException) {
                    Log.d("TAG", "error: ${e.message}")
                }
            }
        }
    }
}
