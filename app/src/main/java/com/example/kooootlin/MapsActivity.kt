package com.example.kooootlin

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kooootlin.model.PermissionFeature
import com.example.kooootlin.services.MarkerService
import com.example.kooootlin.services.PermissionService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.GeofencingClient

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private val positionsList = ArrayList<MarkerOptions>()

    val REQUEST_CODE_PERMISSION = 101
    val REQUEST_TURN_DEVICE_LOCATION_ON = 111
    var type = GoogleMap.MAP_TYPE_HYBRID
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geolocPermissionFeature: PermissionFeature
    private lateinit var geofencingClient: GeofencingClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Prepare geoloc feature permission
        val permissions = arrayListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        geolocPermissionFeature = PermissionFeature("GEOLOC_PERMISSION_FEATURE",
            400,
            permissions,
            getString(R.string.feature_geo)
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        createChannel(this)
        val buttonTypeMap: View = findViewById(R.id.typeMap)
        buttonTypeMap.setOnClickListener { view ->
            changeType()
        }

        val buttonCenterMap: View = findViewById(R.id.centerMap)
        buttonCenterMap.setOnClickListener { view ->
            centerPosition()
        }

        val buttonDeleteMarker: View = findViewById(R.id.deleteMarker)
        buttonDeleteMarker.setOnClickListener { view ->
            deleteMarker()
        }
        geofencingClient = LocationServices.getGeofencingClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.e("cc", location.longitude.toString())
                }

            }

    }

    fun append(arr: Array<String>, element: String): Array<String> {
        val list: MutableList<String> = arr.toMutableList()
        list.add(element)
        return list.toTypedArray()
    }

    private fun initPosition (code: Boolean) {

        if (code) {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.e("test", location.latitude.toString())
                        val currentPosition = LatLng(location.latitude, location.longitude)
                        changePosition(currentPosition)
                    }

                }

        } else {
            val positionNice = LatLng(43.7101728, 7.2619532)
            changePosition(positionNice)
        }

    }

    private fun changeType () {
        type = if (type == GoogleMap.MAP_TYPE_HYBRID) GoogleMap.MAP_TYPE_NORMAL else GoogleMap.MAP_TYPE_HYBRID
        mMap.mapType = type
    }

    private fun centerPosition () {
        initPosition(true)

    }

    private fun deleteMarker () {
        mMap.clear()
        addMarkerNagui()

        positionsList.clear().apply {
            MarkerService.setMarkers(applicationContext, positionsList)
        }

    }

    private fun changePosition (position: LatLng) {

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 18F));

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_TURN_DEVICE_LOCATION_ON -> {
                checkDeviceLocationSettingsAndStartGeofence()
            }
            REQUEST_CODE_PERMISSION -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initPosition(true)
                    checkDeviceLocationSettingsAndStartGeofence()
                }
            }

        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(this,
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("TAG", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                checkDeviceLocationSettingsAndStartGeofence()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForClue()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if (resultCode == Activity.RESULT_OK) {
                    initPosition(true)
                    checkDeviceLocationSettingsAndStartGeofence()
                }
            }
            REQUEST_TURN_DEVICE_LOCATION_ON -> {
                checkDeviceLocationSettingsAndStartGeofence(false)
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = "ACTION_GEOFENCE_EVENT"
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun addMarkerNagui () {

        mMap.addMarker(
            MarkerOptions().position(LatLng(31.2156400, 29.9552700)).title("Lieu de naissance de Nagui, Alexandrie, Egypte")).apply {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.common_full_open_on_phone))
        }

    }

    private fun showDialog(permission: String, requestCode: Int) {

        val builder= AlertDialog.Builder(this)

        builder.apply {
            setMessage("Etes-vous sûr ?")
            setTitle("Permission requise")
            setPositiveButton("Oui") { dialog, which ->
                ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(permission), requestCode)
            }
            setNegativeButton("Non merci") { dialog, which ->
                Toast.makeText(applicationContext, "Tu vas à Nice", Toast.LENGTH_SHORT).show()
                initPosition(false)
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    private fun addGeofenceForClue() {

        val currentGeofenceData = GeofencingConstants.LANDMARK_DATA[0]

        // Add geofence
        val geofence = Geofence.Builder()
            .setRequestId(currentGeofenceData.id)
            .setCircularRegion(currentGeofenceData.latLong.latitude,
                currentGeofenceData.latLong.longitude,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    baseContext, "success Add Geofence",
                    Toast.LENGTH_LONG
                )
                    .show()
                Log.e("Add Geofence", geofence.requestId)
            }
            addOnFailureListener {
                Toast.makeText(
                    baseContext, "failed",
                    Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w("TAG", it.message!!)
                }
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.positionsList.addAll(MarkerService.getMarkers(this))
        mMap = googleMap
        addMarkerNagui()
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        positionsList.forEach{ positions ->
            mMap.addMarker(positions)
        }

        mMap.setOnMapClickListener { lL ->
            val newMarker = MarkerOptions().position(lL)
            mMap.addMarker(newMarker)
            positionsList.add(newMarker).apply {
                MarkerService.setMarkers(applicationContext, positionsList)
            }
        }
        checkDeviceLocationSettingsAndStartGeofence()
        checkPermission()

    }

    private fun checkPermission() {
        if (isPermissionFeatureGranted(geolocPermissionFeature)) {
            initPosition(true)
        }else {

            startActivityForResult(askPermissions(geolocPermissionFeature), REQUEST_CODE_PERMISSION)
        }
    }

    private fun isPermissionFeatureGranted(permissionFeature: PermissionFeature): Boolean {
        val prefs = getSharedPreferences("CODE_PERMISSION", Context.MODE_PRIVATE);
        return prefs.getBoolean(permissionFeature.featureKey, false)
    }

    private fun askPermissions(permissionFeature: PermissionFeature): Intent {
        return Intent(this, PermissionService::class.java).apply {
            putExtra("INTENT_EXTRA_FEATURE_PERMISSION", permissionFeature)
        }
    }


}