package com.example.mobalert

import AdAdapter
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
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
        dialog = Dialog(requireContext())
        HomeFragment.homeBinding?.addAlert?.visibility = View.GONE
        binding.listView.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.alerts_fragment, ListHomeFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        val alerts = arguments?.getSerializable("alerts") as ArrayList<HomeFragment.Alert>


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
                    convertAddressToCoordinates(alert.position, R.drawable.location_red, alert)
                }
                "Marine and aquatic accident" -> {
                    convertAddressToCoordinates(alert.position, R.drawable.location_red, alert)
                }
            }
        }

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

        binding.filter.setOnClickListener {
            val window = PopupWindow(requireContext())
            val view = layoutInflater.inflate(R.layout.filter_layout, null)
            window.contentView = view
            window.isFocusable = true
            window.isOutsideTouchable = true
            window.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.white))

            val title = view.findViewById<EditText>(R.id.title)
            val username=view.findViewById<EditText>(R.id.username)
            val description = view.findViewById<EditText>(R.id.description)
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
                    "Urban and social accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_red, ad)
                    "Marine and aquatic accident" -> convertAddressToCoordinates(ad.position, R.drawable.location_red, ad)
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
        /*
        val calendar = Calendar.getInstance()

        // Listener per clic su data/ora
        elem.setOnClickListener {
            // Mostra DatePickerDialog
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Aggiorna la data nel calendario
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Mostra TimePickerDialog
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            // Aggiorna l'ora nel calendario
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)

                            // Formatta data e ora
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val formattedDateTime = sdf.format(calendar.time)

                            // Imposta il valore nel campo di testo
                            elem.setText(formattedDateTime)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true // Usa formato 24 ore
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
         */
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
                Toast.makeText(requireContext(), "Errore nell'aggiunta del marker", Toast.LENGTH_SHORT).show()
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
            val adapter = AdAdapter(requireContext(), mutableListOf(payload))
            binding.alertsRv.adapter = adapter
            /*
            binding.include.usernameTv.text = alert?.iduser
            binding.include.titleTv.text = alert?.title
            binding.include.categoryTv.text = alert?.type
            binding.include.descriptionTv.text = alert?.description
            binding.include.dateTv.text = alert?.datehour
            */
            Toast.makeText(requireContext(), "Marker cliccato con ID: ${alert?.id}", Toast.LENGTH_SHORT).show()
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