package com.example.mobalert

import AdAdapter
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.mobalert.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


class MapFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var binding: FragmentMapBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dialog: Dialog

    private var pointAnnotationManager: PointAnnotationManager ?= null

    private val markerMap = mutableMapOf<PointAnnotation, HomeFragment.Alert>()
    private val imagesAlert = mutableMapOf<Int, ArrayList<Bitmap?>>()

    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private lateinit var position: String

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private var filters= mutableMapOf(
        "title" to "",
        "description" to "",
        "username" to "",
        "dateHour" to "",
        "category" to ""
    )

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVectorSensor: Sensor

    private var currentAzimuth: Float = 0f
    private var rotate = false
    private val sensorEventListener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                currentAzimuth = (azimuth + 360) % 360

                updateMapBearing(currentAzimuth)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateMapBearing(bearing: Float) {
        if(rotate) {
            val mapboxMap = binding.mapView.getMapboxMap()
            val cameraOptions = CameraOptions.Builder()
                .bearing(bearing.toDouble())
                .build()

            mapboxMap.setCamera(cameraOptions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            sensorEventListener,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        dialog = Dialog(requireContext())
        HomeFragment.homeBinding?.addAlert?.visibility = View.GONE

        binding.rotateCamera.setOnClickListener {
            rotate = !rotate
        }

        binding.listView.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.alerts_fragment, ListHomeFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        val alerts = arguments?.getSerializable("alerts") as? ArrayList<HomeFragment.Alert>
        if (alerts == null) {
            Log.e("LOGIN", "L'argomento 'alerts' è nullo o mancante.")
            return null // Oppure gestisci il caso in modo appropriato
        }
        else{
            position = ""
        position = arguments?.getString("position").toString()
            }
        Log.d("LOGIN","position: $position")



        // Usa la lista come preferisci
        alerts.forEach { alert ->

            if (!imagesAlert.containsKey(alert.id)) {
                imagesAlert[alert.id] = ArrayList()
            }

            var my_images_name : List<String> = alert.image.split(",")
            for (image in my_images_name) {

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bitmap = getImage(alert.id, image)
                        if (bitmap != null) {
                            imagesAlert[alert.id]!!.add(bitmap)
                        }
                        withContext(Dispatchers.Main) {
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                        }
                    }
                }

            }

            reference.child(alert.iduser.toString()).get().addOnSuccessListener {
                alert.iduser = it.child("name").value.toString()
            }

            when (alert.type) {
                "Natural environmental accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_green, alert)
                }
                "Anthropic environmental accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_yellow, alert)
                }
                "Health and biological accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_orange, alert)
                }
                "Technological accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_red, alert)
                }
                "Urban and social accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_blue, alert)
                }
                "Marine and aquatic accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_water, alert)
                }
            }
        }

        // Controlla e richiedi i permessi
        checkAndRequestPermissions(position)


        // Carica lo stile della mappa
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            // Inizializza il PointAnnotationManager
            val annotationPlugin = binding.mapView.annotations
            pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
        }

        binding.myPosition.setOnClickListener {
            checkAndRequestPermissions("null")
        }

        binding.mapView.getMapboxMap().addOnMapClickListener { point ->
            onMapClick(point)
            true
        }

        binding.filter.setOnClickListener {
            val window = PopupWindow(requireContext())
            val view = layoutInflater.inflate(R.layout.filter_layout, null)
            window.contentView = view
            window.isFocusable = true
            window.isOutsideTouchable = true
            window.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.white))

            val title = view.findViewById<EditText>(R.id.editTitle)
            val username=view.findViewById<EditText>(R.id.editUsername)
            val description = view.findViewById<EditText>(R.id.editDescription)
            val elemFrom=view.findViewById<EditText>(R.id.dateFrom)
            val reset=view.findViewById<Button>(R.id.Reset)
            val elemTo=view.findViewById<EditText>(R.id.dateTo)
            setupDateTimePicker(elemFrom)
            setupDateTimePicker(elemTo)

            title.setText(filters["title"])
            username.setText(filters["username"])
            description.setText(filters["description"])
            if(filters["dateHour"]!="") {

                elemFrom.setText(filters["dateHour"]!!.split(",")[0])
                elemTo.setText(filters["dateHour"]!!.split(",")[1])
            }
            else{
                elemFrom.setText("")
                elemTo.setText("")
            }

            val cat1=view.findViewById<CheckBox>(R.id.Cat1)
            setUpCategory(cat1,"Natural environmental accident",alerts)

            val cat2=view.findViewById<CheckBox>(R.id.Cat2)
            setUpCategory(cat2,"Anthropic environmental accident",alerts)

            val cat3=view.findViewById<CheckBox>(R.id.Cat3)
            setUpCategory(cat3,"Health and biological accident",alerts)

            val cat4=view.findViewById<CheckBox>(R.id.Cat4)
            setUpCategory(cat4,"Technological accident",alerts)

            val cat5=view.findViewById<CheckBox>(R.id.Cat5)
            setUpCategory(cat5,"Urban and social accident",alerts)

            val cat6=view.findViewById<CheckBox>(R.id.Cat6)
            setUpCategory(cat6,"Marine and aquatic accident",alerts)

            setUpEditText(title, "title", alerts)

            setUpEditText(username, "username", alerts)

            setUpEditText(description, "description", alerts)

            elemFrom.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filters["dateHour"] = "${s.toString()},${elemTo.text}"
                    filter(filters, alerts)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

            elemTo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filters["dateHour"] = "${elemFrom.text},${s.toString()}"
                    filter(filters, alerts)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })




            reset.setOnClickListener {
                //TO-DO: RESETTARE LA UI
                filters["title"] = ""
                filters["description"] = ""
                filters["username"] = ""
                filters["dateHour"] = ""
                filters["category"] = ""
                filter(filters, alerts)
                window.dismiss()
            }

            window.showAsDropDown(binding.filter)
        }

        return binding.root
    }

    private fun setUpEditText(elem: EditText, field: String, alerts: ArrayList<HomeFragment.Alert>){
        elem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Azioni prima che il testo cambi
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Azioni mentre il testo sta cambiando
                filters[field] = s.toString()
                filter(filters, alerts)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun setUpCategory(elem: CheckBox, category: String,alerts: ArrayList<HomeFragment.Alert>){
        elem.isChecked = filters["category"]!!.contains(category)

        elem.setOnClickListener {
            // Puoi controllare lo stato della checkbox manualmente
            if (elem.isChecked) {
                if(filters["category"]==""){
                    filters["category"]=category
                }
                else{
                    filters["category"]+=",$category"
                }
                // Azione quando è selezionata
                filter(filters, alerts)
            } else {


                filters["category"] = filters["category"]!!.replace(",$category", "")
                filters["category"] = filters["category"]!!.replace(category, "")
                filter(filters, alerts)

            }
        }
    }

    private fun clearMarkers() {
        pointAnnotationManager?.deleteAll()
        markerMap.clear()
    }
    
    private fun filter(filters: MutableMap<String, String>, alerts: ArrayList<HomeFragment.Alert>) {
        Log.d("LOGIN", "$filters")
        clearMarkers()

        for (ad in alerts) {
            var visible=true

            if (!ad.title.contains(filters["title"]!!, ignoreCase = true)) {
                visible = false
            }

            if (!ad.description.contains(filters["description"]!!, ignoreCase = true)) {
                visible = false
            }
            if(filters["category"]!="") {
                if (!filters["category"]!!.contains(ad.type, ignoreCase = true)) {
                    visible = false
                }
            }


            if (!ad.iduser.contains(filters["username"]!!, ignoreCase = true)) {
                visible = false
            }

            if(filters["dateHour"]!="") {
                val dates = filters["dateHour"]!!.split(",")
                val dateFrom = dates[0]
                val dateTo = dates[1]
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                val dateFromParsed: LocalDateTime
                val dateToParsed: LocalDateTime
                if (dateFrom.isNotEmpty()) {
                    dateFromParsed = LocalDateTime.parse(dateFrom, formatter)
                    if (dateTo.isNotEmpty()) {
                        dateToParsed = LocalDateTime.parse(dateTo, formatter)
                        if (LocalDateTime.parse(ad.datehour, formatter)
                                .isBefore(dateFromParsed) || LocalDateTime.parse(
                                ad.datehour,
                                formatter
                            )
                                .isAfter(dateToParsed)
                        ) {
                            visible = false
                        }
                    } else {
                        if (LocalDateTime.parse(ad.datehour, formatter)
                                .isBefore(dateFromParsed)
                        ) {
                            visible = false
                        }
                    }
                } else if (dateTo.isNotEmpty()) {
                    dateToParsed = LocalDateTime.parse(dateTo, formatter)
                    if (LocalDateTime.parse(ad.datehour, formatter)
                            .isAfter(dateToParsed)
                    ) {
                        visible = false
                    }
                }
            }

            if(visible){
                when (ad.type) {
                    "Natural environmental accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_green, ad)
                    "Anthropic environmental accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_yellow, ad)
                    "Health and biological accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_orange, ad)
                    "Technological accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_red, ad)
                    "Urban and social accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_blue, ad)
                    "Marine and aquatic accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_water, ad)
                }
            }
        }
    }

    private fun setupDateTimePicker(elem:EditText) {
        elem.setOnClickListener {
            dialog.setContentView(R.layout.date_hour)
            dialog.setCancelable(false)
            dialog.show()
            val datePicker = dialog.findViewById<DatePicker>(R.id.datepicker)
            val deleteDate = dialog.findViewById<TextView>(R.id.deleteDate)
            val okDate = dialog.findViewById<TextView>(R.id.okDate)
            val timePicker = dialog.findViewById<TimePicker>(R.id.timepicker)
            val deleteTime = dialog.findViewById<TextView>(R.id.deleteTime)
            val okTime = dialog.findViewById<TextView>(R.id.okTime)

            timePicker.setIs24HourView(true)

            deleteDate.setOnClickListener {
                elem.setText("")
                dialog.hide()
            }
            deleteTime.setOnClickListener {
                elem.setText("")
                dialog.hide()
            }

            okDate.setOnClickListener {
                datePicker.visibility = View.GONE
                timePicker.visibility = View.VISIBLE
            }

            okTime.setOnClickListener {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR,datePicker.year)
                calendar.set(Calendar.MONTH,datePicker.month)
                calendar.set(Calendar.DAY_OF_MONTH,datePicker.dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY,timePicker.hour)
                calendar.set(Calendar.MINUTE,timePicker.minute)
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val parsedDate = sdf.format(calendar.time)
                elem.setText(parsedDate)
                dialog.hide()
            }
        }
    }

    private suspend fun getImage(id : Int, image: String): Bitmap? {
        var url : String
        if (".jpg" in image) url = "${MainActivity.url}/images/${id.toString()}_$image"
        else url = "${MainActivity.url}/images/${id.toString()}_$image.jpg"
        try {
            val response: HttpResponse = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                val bytes = response.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                return bitmap
            } else {
                Log.e("LOGIN", "Errore nella richiesta img: ${response.status}")
                return null
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta img: $e")
            return null
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Attivare la posizione")
            .setMessage("La posizione è disattivata.")
            .setNegativeButton("Ok") { dialog, _ ->
                dialog.dismiss() // Chiudi il dialogo
            }
            .show()
    }

    private fun checkAndRequestPermissions(position: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permissions", "Permessi non concessi, li richiedo...")
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        else {
            Log.d("Permissions", "Permessi già concessi, avvio gli aggiornamenti.")
            getLastKnownLocation(position)
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
                getLastKnownLocation(position)
            } else {
                // Permesso negato
                Toast.makeText(requireContext(), "Permesso negato!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun getLastKnownLocation(position: String) {
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
        val mapboxMap = binding.mapView.getMapboxMap()
        if (position != "null"){
            Log.d("LOGIN", "sono qua: $position")
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(GeocodingService::class.java)
            val call = service.getCoordinates(position, "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw")

            call.enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val features = (body?.get("features") as? List<*>)?.filterIsInstance<Map<String, Any>>()
                        if (!features.isNullOrEmpty()) {
                            val geometry = features[0]["geometry"] as? Map<String, Any>
                            val coordinates = geometry?.get("coordinates") as? List<Double>
                            if (coordinates != null && coordinates.size >= 2) {
                                val cameraOptions = CameraOptions.Builder()
                                    .center(
                                        com.mapbox.geojson.Point.fromLngLat(
                                            coordinates[0],
                                            coordinates[1]
                                        )
                                    )
                                    .zoom(18.0)
                                    .build()

                                mapboxMap.flyTo(cameraOptions)
                                Log.d("LOGIN", "Coordinate trovate: Lat: ${coordinates[0]}, Lon: ${coordinates[1]}")
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
        else {
            if (!isLocationEnabled(requireContext())) {
                // Mostra il dialogo se la posizione è disabilitata
                showLocationDialog()
            }
            else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            Log.d(
                                "LocationSuccess",
                                "Lat: ${location.latitude}, Lon: ${location.longitude}"
                            )

                            val cameraOptions = CameraOptions.Builder()
                                .center(
                                    com.mapbox.geojson.Point.fromLngLat(
                                        location.longitude,
                                        location.latitude
                                    )
                                )
                                .zoom(18.0)
                                .build()

                            mapboxMap.flyTo(cameraOptions)
                        } else {
                            Log.e("LocationError", "Posizione non disponibile")
                            Toast.makeText(
                                requireContext(),
                                "Impossibile ottenere la posizione.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "LocationError",
                            "Errore durante il rilevamento della posizione: ${exception.message}"
                        )
                        Toast.makeText(
                            requireContext(),
                            "Errore nel rilevamento della posizione: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

    }

    private fun addMarker(longitude: Double, latitude: Double, drawable: Int, alert: HomeFragment.Alert) {
        val point = Point.fromLngLat(longitude, latitude)

        // Converti il drawable in bitmap
        val drawable = ContextCompat.getDrawable(requireContext(), drawable)
        val bitmap = drawable?.toBitmap()

        // Converte il layout in Bitmap
        //val customMarkerView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_el, null)
        //val bitmap = convertViewToBitmap(customMarkerView)

        // Registra il bitmap come icona nel gestore annotazioni
        bitmap?.let {
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(it)
            )

            try {
                markerMap[pointAnnotationManager?.annotations!!.last()] = alert
            }
            catch (e: Exception){
                Log.e("LOGIN", "Errore nell'aggiunta del marker $e")
            }
        }
    }

    private fun setupMarkerClickListener() {
        pointAnnotationManager?.addClickListener { annotation ->
            val alert = markerMap[annotation]

            Log.d("LOGIN", "Marker cliccato con alert: $alert")
            // Gestisci l'azione per il marker cliccat
            val include = view?.findViewById<View>(R.id.include)
            //include?.visibility = View.VISIBLE
            val payload = HomeFragment.HomeAlters(
                alert!!.id,
                alert!!.description,
                alert!!.iduser,
                alert!!.title,
                alert!!.datehour,
                alert!!.type,
                alert!!.position,
                imagesAlert[alert.id]!!,
                true
            )
            val adapter = AdAdapter(requireContext(),null,  mutableListOf(payload),null)
            binding.alertsRv.adapter = adapter
            /*
            binding.include.usernameTv.text = alert?.iduser
            binding.include.titleTv.text = alert?.title
            binding.include.categoryTv.text = alert?.type
            binding.include.descriptionTv.text = alert?.description
            binding.include.dateTv.text = alert?.datehour
            */
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
        //view?.findViewById<View>(R.id.include)?.visibility = View.GONE
        binding.alertsRv.adapter = null

        Log.d("MAP_CLICK", "Lat: $latitude, Lon: $longitude")

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
                        //convertAddressToCoordinates(address!!)
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

    fun convertAddressToCoordinates(address: String, drawable: Int, alert: HomeFragment.Alert) {
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
                            addMarker(longitude, latitude, drawable,alert)
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