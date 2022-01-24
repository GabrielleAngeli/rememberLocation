package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    //data held by SaveReminderViewModel is synced across SaveReminderFragment and SelectLocationFragment
    override val _viewModel: SaveReminderViewModel by inject()
    override fun styleMap(mapTypeNormal: String) {
        TODO("Not yet implemented")
    }

    override fun askUserForMarkerOrPOI(latLng: LatLng, zoom: Float, title: String) {
        TODO("Not yet implemented")
    }

    override fun addGeofenceForClue() {
        TODO("Not yet implemented")
    }

    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var contxt: Context

    private lateinit var map: GoogleMap
    private var marker: Marker? = null
    private val zoomLevel = 15f
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(100.0, 30.0)

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        //The FusedLocationProviderClient provides several methods to retrieve device location information.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(contxt)

        setHasOptionsMenu(true)

//       add the map setup implementation
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkPermissions()

//       call this function after the user confirms on the selected location
        binding.proceed.setOnClickListener {
            onLocationSelected()
            Toast.makeText(
                context,
                getString(R.string.reminder_location),
                Toast.LENGTH_SHORT
            ).show()
        }
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableCurrentLocation()

        setMapStyle(map)
        setMapLongClick(map)
        setPoiClick(map)
        Toast.makeText(context, R.string.choose_a_place, Toast.LENGTH_SHORT).show()
    }

    //        add style to the map
    private fun setMapStyle(map: GoogleMap) {
        try {
            val success =
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    //        put a marker to location that the user selected
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat:%1$.5f, Long:%2$5f",
                latLng.latitude,
                latLng.longitude
            )
            map.clear()
            val randomMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )
            marker = randomMarker
        }
    }

    //put a marker to point of interest that the user selected
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            marker = poiMarker
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableCurrentLocation() {
        if (foregroundLocationPermissionApproved()) {
            map.isMyLocationEnabled = true
            zoomCurrentLocation()
        }
    }

    //reference https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial#get-the-location-of-the-android-device-and-position-the-map
    //        zoom to the user location after taking his permission
    @SuppressLint("MissingPermission")
    private fun zoomCurrentLocation() {
        if (foregroundLocationPermissionApproved()) {
            val locationResult = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        map?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude
                                ), zoomLevel
                            )
                        )
                    }
                } else {
                    Log.d(TAG, "Current location is null. Using defaults.")
                    Log.e(TAG, "Exception: %s", task.exception)
                    map?.moveCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, zoomLevel)
                    )
                    map?.uiSettings?.isMyLocationButtonEnabled = true
                }
            }
        }
    }

    private fun onLocationSelected() {
        marker?.let {
            _viewModel.reminderSelectedLocationStr.value = it.title
            _viewModel.latitude.value = it.position.latitude
            _viewModel.longitude.value = it.position.longitude
        }
        findNavController().popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /*
    *  Determines whether the app has the appropriate permissions across Android 10+ and all other
    *  Android versions.This app need foreground and background location permission to work.
    */
    @TargetApi(29)
    private fun foregroundLocationPermissionApproved(): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            contxt,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    /*
     *  Requests ACCESS_FINE_LOCATION.
     */
    @TargetApi(29)
    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved()) {
            return
        }
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = 33
        Log.i(
            TAG,
            "Request foreground location permission"
        )
        //Request permissions passing in the current activity, the permissions array and the result code.
        requestPermissions(permissionsArray, resultCode)

    }

    // Check if location permissions are granted and if so enable the current location
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE &&
                    grantResults[FOREGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // This app has very little use when permissions are not granted so present a snackbar explaining
            // that the user needs location permissions in order to play.
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    requestForegroundLocationPermissions()
                }.show()
        } else {
            enableCurrentLocation()
            checkDeviceLocationSettings()
        }
    }

    //Separate check device location settings and request location permissions
    private fun checkPermissions() {
        if (foregroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundLocationPermissions()
        }
    }

    /*
    *  Uses the Location Client to check the current state of location settings, and gives the user
    *  the opportunity to turn on location services within our app.
    */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(contxt)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE, null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(
                        TAG,
                        "Error getting location settings resolution: " + sendEx.message
                    )
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    // After the user chooses whether to accept or deny device location permissions,
    // this checks if the user has chosen to accept the permissions. If not, it will ask again.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettings(false)
        }
    }
}

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "SelectLocationFragment"
private const val LOCATION_PERMISSION_INDEX = 0
private const val FOREGROUND_LOCATION_PERMISSION_INDEX = 1
