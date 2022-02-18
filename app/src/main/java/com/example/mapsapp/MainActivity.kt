package com.example.mapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
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


        Toast.makeText(this, "AAAA", Toast.LENGTH_SHORT).show()
        createMapFragment()

        // Construct a PlacesClient
        /*Places.initialize(applicationContext, getString(R.string.api_maps_key))
        placesClient = Places.createClient(this)*/


    }

    private fun createMapFragment() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestLocationPermission()
            return
        }
        else {

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
                            onMyLocationChange( location )
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )



                this.currentLocation = location
                val mapFragment  = supportFragmentManager
                    .findFragmentById(R.id.fragmentMap) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }


    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!

        map.animateCamera(
            CameraUpdateFactory.newLatLng(
                LatLng(
                    this.currentLocation!!.latitude,
                    this.currentLocation!!.longitude
                )
            )
        )

        map.setOnMyLocationClickListener {

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


            onMyLocationChange(this.currentLocation!!)
        }
        enableMyLocation()
        // createMarker()
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
            return
        } else {
            map.isMyLocationEnabled = true
            Toast.makeText(this, "Los tienes", Toast.LENGTH_SHORT).show()
        }

    }

    private fun createMarker() {
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

    fun onMyLocationChange(location: Location) {
        val target = Location("target")
        val test: List<Location>
        
        for (point in newPolilineLats) {
            target.setLatitude(point.latitude)
            target.setLongitude(point.longitude)
            if (location.distanceTo(target) < 50) {
                Toast.makeText(this, "holaaaa", Toast.LENGTH_SHORT).show()
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
        when(requestCode){
            REQUEST_CODE_LOCATION -> if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                map.isMyLocationEnabled = true
            }else{
                Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
                requestLocationPermission()
                return
        }
        else {
            map.isMyLocationEnabled = true
        }
    }

    override fun onLocationChanged(p0: Location) {
        TODO("Not yet implemented")
        Toast.makeText(this, "ufff", Toast.LENGTH_SHORT).show()
    }


}