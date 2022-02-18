package com.example.mapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var newPolilineLats: ArrayList<LatLng> = arrayListOf()

    private var currentLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    companion object {
        const val REQUEST_CODE_LOCATION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Construct a fusedLocationClient.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createMapFragment()
    }

    private fun createMapFragment() {


        val mapFragment  = supportFragmentManager
            .findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestLocationPermission()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        enableMyLocation()
        addMarkerOnLongPress()


        val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        FindCurrentPlaceRequest.newInstance(placeFields)

    }

    private fun enableMyLocation() {

        if (!::map.isInitialized) return
        if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestLocationPermission()
            }
            else {
                map.isMyLocationEnabled = true
                setRequestPositionListener()
            }
    }




    private fun createStaticMarker() {
        val favoritePlace = LatLng(28.044195,-16.5363842)
        map.addMarker(MarkerOptions().position(favoritePlace).title("Mi playa favorita!"))
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(favoritePlace, 18f),
            4000,
            null
        )
    }

    private fun addMarkerOnLongPress(){

        map.setOnMapLongClickListener(OnMapLongClickListener { latLng ->
            newPolilineLats.add( latLng )
            createPolylines()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Your marker title")
                    .snippet("Your marker snippet")
            )
        })

    }

    private fun createPolylines(){
        val polylineOptions = PolylineOptions()
        val polygonOptions = PolygonOptions()

        for ( lat in newPolilineLats ){
            polylineOptions.add( lat )
            polygonOptions.add( lat )
        }

        val polyline = map.addPolyline(polylineOptions)
        val polygon = map.addPolygon( polygonOptions )
        polygon.fillColor = 0x7F00FF00
    }




    private fun setRequestPositionListener(){

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val task = fusedLocationClient.lastLocation
        task.addOnSuccessListener { location ->


            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            var locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
//                Log.d(LOG_TAG, "Se recibió una actualización")
                    for (location in locationResult.locations) {
                        onMyLocationChange(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            this.currentLocation = location

        }
    }

    private fun onMyLocationChange(location: Location) {
        val target = Location("target")
        
        for (point in newPolilineLats) {
            target.setLatitude(point.latitude)
            target.setLongitude(point.longitude)
            if (location.distanceTo(target) < 2) {
                Toast.makeText(this, "Estas cerca de un marcador", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            REQUEST_CODE_LOCATION ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText( this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT ).show()
                        return
                    }

                    map.isMyLocationEnabled = true
                    setRequestPositionListener()

                }
            else -> {}
        }


    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            
            map.isMyLocationEnabled = false
            Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        }


    }


}