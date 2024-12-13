package com.example.mobalert

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.mobalert.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query




class MapFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var binding: FragmentMapBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var pointAnnotationManager: PointAnnotationManager

    private val markerMap = mutableMapOf<PointAnnotation, String>()
    private var markerIdCounter = 0

    lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Controlla e richiedi i permessi
        checkAndRequestPermissions()

        // Carica lo stile della mappa
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            // Inizializza il PointAnnotationManager
            val annotationPlugin = binding.mapView.annotations
            pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
        }

        binding.mapView.getMapboxMap().addOnMapClickListener { point ->
            onMapClick(point)
            true
        }

        return binding.root
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Richiedi i permessi
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permessi già concessi
            getLastKnownLocation()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("LOGIN", "onRequestPermissionsResult")
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso concesso
                Toast.makeText(requireContext(), "Permesso concesso!", Toast.LENGTH_SHORT).show()
                getLastKnownLocation()
            } else {
                // Permesso negato
                Toast.makeText(requireContext(), "Permesso negato!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permessi non concessi, non dovrebbe mai arrivare qui
            Toast.makeText(requireContext(), "Permessi non concessi!", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("LOGIN", "Lat: $latitude, Lon: $longitude")
                    Toast.makeText(
                        requireContext(),
                        "Lat: $latitude, Lon: $longitude",
                        Toast.LENGTH_SHORT
                    ).show()

                    val mapboxMap = binding.mapView.getMapboxMap()
                    val cameraOptions = CameraOptions.Builder()
                        .center(com.mapbox.geojson.Point.fromLngLat(longitude, latitude))
                        .zoom(14.0)
                        .build()

                    mapboxMap.flyTo(cameraOptions)
                    binding.mapView.location.pulsingEnabled = true
                    //binding.mapView.location.enabled = false
                    //addMarker(longitude, latitude)
                } else {
                    Toast.makeText(requireContext(), "Impossibile ottenere la posizione.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Errore nel rilevamento della posizione: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun addMarker(longitude: Double, latitude: Double) {
        val point = Point.fromLngLat(longitude, latitude)

        // Converti il drawable in bitmap
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.location_red)
        val bitmap = drawable?.toBitmap()

        // Converte il layout in Bitmap
        //val customMarkerView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_el, null)
        //val bitmap = convertViewToBitmap(customMarkerView)

        // Registra il bitmap come icona nel gestore annotazioni
        bitmap?.let {
            pointAnnotationManager.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(it)
            )

            markerMap[pointAnnotationManager.annotations.last()] = markerIdCounter.toString()
            markerIdCounter++
        }
    }

    private fun setupMarkerClickListener() {
        pointAnnotationManager.addClickListener { annotation ->
            val id = markerMap[annotation]
            if (id != null) {
                // Gestisci l'azione per il marker cliccato
                Toast.makeText(requireContext(), "Marker cliccato con ID: $id", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Marker cliccato!", Toast.LENGTH_SHORT).show()
            }
            true // Ritorna true per consumare l'evento
        }
    }

    private fun convertViewToBitmap(view: View): Bitmap {
        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(measureSpec, measureSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun onMapClick(point: Point) {
        val latitude = point.latitude()
        val longitude = point.longitude()

        Log.d("MAP_CLICK", "Lat: $latitude, Lon: $longitude")
        Toast.makeText(requireContext(), "Punto cliccato: Lat: $latitude, Lon: $longitude", Toast.LENGTH_SHORT).show()

        // Aggiungi un marker nel punto cliccato
        //addMarker(longitude, latitude)
        getAddressFromCoordinates(longitude, latitude)
    }

    // Modifica il GeocodingService per restituire un tipo JSON generico
    interface GeocodingService {
        @GET("geocoding/v5/mapbox.places/{longitude},{latitude}.json")
        fun getAddress(
            @Path("longitude") longitude: Double,
            @Path("latitude") latitude: Double,
            @Query("access_token") accessToken: String
        ): Call<Map<String, Any>>

        @GET("geocoding/v5/mapbox.places/{address}.json")
        fun getCoordinates(
            @Path("address") address: String,
            @Query("access_token") accessToken: String,
            @Query("limit") limit: Int = 1 // Limita il numero di risultati
        ): Call<Map<String, Any>>
    }

    private fun getAddressFromCoordinates(longitude: Double, latitude: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mapbox.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GeocodingService::class.java)
        val call = service.getAddress(longitude, latitude, "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw")

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val features = (body?.get("features") as? List<*>)?.filterIsInstance<Map<String, Any>>()
                    if (!features.isNullOrEmpty()) {
                        val address = features[0]["place_name"] as? String
                        convertAddressToCoordinates(address!!)
                        Log.d("LOGIN", "Indirizzo trovato: $address")
                    } else {
                        Log.d("LOGIN", "Nessun indirizzo trovato.")
                    }
                } else {
                    Log.e("LOGIN", "Errore nella risposta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("LOGIN", "Errore: ${t.message}")
            }
        })
    }



    fun convertAddressToCoordinates(address: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mapbox.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GeocodingService::class.java)
        val call = service.getCoordinates(address, "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw")

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val features = (body?.get("features") as? List<*>)?.filterIsInstance<Map<String, Any>>()
                    if (!features.isNullOrEmpty()) {
                        val geometry = features[0]["geometry"] as? Map<String, Any>
                        val coordinates = geometry?.get("coordinates") as? List<Double>
                        if (coordinates != null && coordinates.size >= 2) {
                            val longitude = coordinates[0]
                            val latitude = coordinates[1]
                            addMarker(longitude, latitude)
                            setupMarkerClickListener()
                            Log.d("LOGIN", "Coordinate trovate: Lat: $latitude, Lon: $longitude")
                        } else {
                            Log.d("LOGIN", "Impossibile trovare le coordinate.")
                        }
                    } else {
                        Log.d("LOGIN", "Nessun risultato trovato.")
                    }
                } else {
                    Log.e("LOGIN", "Errore nella risposta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("LOGIN", "Errore: ${t.message}")
            }
        })
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}