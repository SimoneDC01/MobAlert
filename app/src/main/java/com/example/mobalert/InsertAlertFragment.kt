package com.example.mobalert

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mobalert.MapFragment.GeocodingService
import com.example.mobalert.databinding.FragmentInsertAlertBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.primitives.Bytes
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.location
import com.yalantis.ucrop.UCrop
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class InsertAlertFragment : Fragment() {
    private lateinit var binding: FragmentInsertAlertBinding
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private var imageUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private lateinit var dialog: Dialog

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private lateinit var imagesPickedArrayList: ArrayList<ModelImagePicked>

    private lateinit var adapterImagePicked: AdapterImagePicked


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentInsertAlertBinding.inflate(inflater, container, false)

        dialog = Dialog(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupCategoryDropdown()

        setupDateTimePicker(binding.editDateHour)

        imagesPickedArrayList = ArrayList()

        binding.pickPosition.setOnClickListener{
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
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val retrofit = Retrofit.Builder()
                            .baseUrl("https://api.mapbox.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val service = retrofit.create(com.example.mobalert.MapFragment.GeocodingService::class.java)
                        val call = service.getAddress(longitude, latitude, "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw")

                        call.enqueue(object : Callback<Map<String, Any>> {
                            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    val features = (body?.get("features") as? List<*>)?.filterIsInstance<Map<String, Any>>()
                                    if (!features.isNullOrEmpty()) {
                                        val address = features[0]["place_name"] as? String
                                        //convertAddressToCoordinates(address!!)
                                        binding.editPosition.setText(address)
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
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Impossibile ottenere la posizione.",
                            Toast.LENGTH_SHORT
                        ).show()
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

        binding.InsertAlertButton.setOnClickListener {

            var images =""
            for (imagePicked in imagesPickedArrayList) {
                images += imagePicked.id.toString()+".jpg,"
            }

            val title = binding.editTitle.text.toString()
            val category= binding.editCategory.text.toString()
            val description = binding.editDescription.text.toString()
            val date = binding.editDateHour.text.toString()
            val position = binding.editPosition.text.toString()

            if (title.isEmpty()) {
                binding.editTitle.error = "Enter Title"
                binding.editTitle.requestFocus()
            }
            else if (category.isEmpty()) {
                binding.editCategory.error = "Enter Category"
                binding.editCategory.requestFocus()
            }
            else if (description.isEmpty()) {
                binding.editDescription.error = "Enter Description"
                binding.editDescription.requestFocus()
            }
            else if (date.isEmpty()) {
                binding.editDateHour.error = "Enter Date"
                binding.editDateHour.requestFocus()
            }
            else if (position.isEmpty()) {
                binding.editPosition.error = "Enter Position"
                binding.editPosition.requestFocus()
            }
            else if(images==""){
                Toast.makeText(requireContext(), "Insert Images", Toast.LENGTH_SHORT).show()
            }
            else {
                images = images.substring(0, images.length - 1)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alert = HomeFragment.Alert(
                            binding.editDateHour.text.toString(),
                            binding.editDescription.text.toString(),
                            0,
                            auth.currentUser!!.uid,
                            binding.editPosition.text.toString(),
                            binding.editTitle.text.toString(),
                            binding.editCategory.text.toString(),
                            images
                        )

                        insertAlert(alert, imagesPickedArrayList[0])
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Insert", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.Fragment, HomeFragment())
                                .commit()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.d("LOGIN", "Error: $e")
                        }
                    }
                }
            }



        }

        binding.imageButtonAlert.setOnClickListener {
            //init popup menu param 1 is context and param 2 is the UI View (profileIma
            val popupMenu = PopupMenu( this.context, binding.imageButtonAlert)
            //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, P
            popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
            //Show Popup Menu
            popupMenu.show()
            //handle popup menu item click
            popupMenu.setOnMenuItemClickListener { item ->
                //get the id of the menu item clicked
                val itemId = item.itemId
                //check which menu item is clicked based on itemId we got
                if (itemId == 1) {
                    //Camera is clicked
                    Log.d("LOGIN", "imagePickDialog: Camera Clicked")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestCameraPermission.launch(arrayOf(
                            android.Manifest.permission.CAMERA))
                    }
                    else{
                        requestCameraPermission.launch(arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    }
                } else {
                    //Device version is below TIRAMISU. We need Camera & Storage permis
                    Log.d("LOGIN", "imagePickDialog: Gallery Clicked")
                    pickImageGallery()
                }
                true
            }
        }

        setupEditText()

        // Restituisci la vista associata al binding
        return binding.root
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ result ->
        var allAreGranted = true
        for (isGranted in result.values){
            allAreGranted = allAreGranted && isGranted
        }

        if (allAreGranted){
            Log.d("LOGIN", "Permission granted")
            pickImageCamera()
        }
        else {
            Log.d("LOGIN", "Permission denied")
        }
    }

    private fun pickImageCamera(){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Image Description")

        imageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startCrop(imageUri)

        }
        else{
            Log.d("LOGIN", "Image Pick Cancelled")
            Toast.makeText(this.activity, "Image Pick Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            startCrop(imageUri)


        }
        else{
            Log.d("LOGIN", "Image Pick Cancelled")
            Toast.makeText(this.activity, "Image Pick Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                val modelImagePicked = ModelImagePicked(
                    id = 0,
                    imageUri = resultUri
                )
                Log.d("LOGIN", "Cropped Image URI: $resultUri")
                imagesPickedArrayList.add(modelImagePicked)
                loadImages()
            } else {
                Log.d("LOGIN", "Cropped image URI is null")
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.d("LOGIN", "Crop error: $cropError")
        }
    }


    private fun startCrop(uri: Uri?) {
        Log.d("LOGIN", "startCrop called with URI: $uri")
        uri?.let {
            try {
                val uniqueFileName = "croppedImage_${System.currentTimeMillis()}.jpg"
                val destinationUri = Uri.fromFile(File(requireContext().cacheDir, uniqueFileName))
                val options = UCrop.Options().apply {
                    setCompressionQuality(80)
                    setFreeStyleCropEnabled(true)
                    setHideBottomControls(false)
                }
                val uCropIntent = UCrop.of(it, destinationUri)
                    .withOptions(options)
                    .withAspectRatio(1f, 1f)
                    .getIntent(requireContext())

                cropImageLauncher.launch(uCropIntent)
            } catch (e: Exception) {
                Log.e("LOGIN", "Error starting UCrop: ${e.message}")
            }
        } ?: Log.e("LOGIN", "URI is null")
    }


    private fun loadImages() {
        Log.d("LOGIN", "loadImages: ")
        adapterImagePicked = AdapterImagePicked(this.requireContext(), imagesPickedArrayList)
        binding.imagesRv.adapter = adapterImagePicked
    }

    @OptIn(InternalAPI::class)
    suspend fun insertAlert(alert: HomeFragment.Alert, file: ModelImagePicked) {
        val url = "${MainActivity.url}/alerts"
        var listImages = arrayListOf<ByteArray>()

        for (imagePicked in imagesPickedArrayList) {
            val imageBytes = requireContext().contentResolver.openInputStream(imagePicked.imageUri!!)?.use { inputStream ->
                inputStream.readBytes()
            }?: throw IllegalArgumentException("unable to read bytes from input stream ${imagePicked.imageUri!!}")
            listImages.add(imageBytes)
        }
        val fileBytes = requireContext().contentResolver.openInputStream(file.imageUri!!)?.use { inputStream ->
            inputStream.readBytes()
        }?: throw IllegalArgumentException("unable to read bytes from input stream ${file.imageUri!!}")
        try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.MultiPart.FormData)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            // Aggiungi il corpo dell'alert come parte della richiesta
                            append("data", Json.encodeToString(alert))

                            // Aggiungi il file come parte della richiesta
                            for (i in 0 until listImages.size) {
                                append("file$i", listImages[i],
                                    Headers.build {
                                        append(HttpHeaders.ContentDisposition, "filename=$i.jpg")
                                    })
                            }

                        }
                    )
                )
            }
            if (response.status == HttpStatusCode.Created) {
                val result = response.bodyAsText()
                Log.d("LOGIN", "Risposta dal server: $result")
            } else {
                Log.e("LOGIN", "Errore nella richiesta: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }

    private fun setupCategoryDropdown() {
        // Lista di categorie predefinite
        val categories = listOf("Natural environmental accident", "Anthropic environmental accident", "Health and biological accident", "Technological accident","Urban and social accident","Marine and aquatic accident")

        // Crea un adattatore per l'AutoCompleteTextView
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        // Imposta l'adattatore nell'AutoCompleteTextView
        binding.editCategory.setAdapter(adapter)

        // Apri sempre il dropdown quando l'utente clicca sul campo
        binding.editCategory.setOnClickListener {
            binding.editCategory.showDropDown()
        }

        // Garantire che si apra anche al focus
        binding.editCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.editCategory.showDropDown()
            }
        }
    }



    private fun setupDateTimePicker(elem : EditText) {
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

    private fun fetchAddressSuggestions(query: String) {
        Log.d("LOGIN", "fetchAddressSuggestions: $query")
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mapbox.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        Log.d("LOGIN", "fetchAddressSuggestions: 1")

        val service = retrofit.create(GeocodingService::class.java)
        val call = service.getSuggestions(query, "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw")


        Log.d("LOGIN", "fetchAddressSuggestions: 2")

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Log.d("LOGIN", "fetchAddressSuggestions: 3")

                    val features = (response.body()?.get("features") as? List<*>)?.filterIsInstance<Map<String, Any>>()
                    val suggestions = features?.mapNotNull { it["place_name"] as? String } ?: emptyList()
                    Log.d("LOGIN", "fetchAddressSuggestions: $suggestions")

                    // Aggiorna i suggerimenti nel RecyclerView
                    updateSuggestions(suggestions)
                } else {
                    Log.e("LOGIN", "Errore nella risposta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("LOGIN", "Errore: ${t.message}")
            }
        })
    }

    interface GeocodingService {
        @GET("geocoding/v5/mapbox.places/{query}.json")
        fun getSuggestions(
            @Path("query") query: String,
            @Query("access_token") accessToken: String,
            @Query("autocomplete") autocomplete: Boolean = true,
            @Query("limit") limit: Int = 5
        ): Call<Map<String, Any>>
    }

    private fun setupEditText() {
        val editText = binding.editPosition
        val recyclerView = binding.recyclerViewSuggestions

        Log.d("LOGIN", "setupEditText: 1")
        val suggestionsAdapter = SuggestionsAdapter(emptyList()) { selectedAddress ->
            editText.setText(selectedAddress)
            recyclerView.visibility = View.GONE
        }
        Log.d("LOGIN", "setupEditText: 2")

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = suggestionsAdapter

        Log.d("LOGIN", "setupEditText: 3")

        editText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("LOGIN", "onTextChanged: $s")
                val query = s.toString()
                if (query.isNotEmpty()) {
                    recyclerView.visibility = View.VISIBLE
                    fetchAddressSuggestions(query)
                } else {
                    recyclerView.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateSuggestions(suggestions: List<String>) {
        val recyclerView = binding.recyclerViewSuggestions
        val onItemClick: (String) -> Unit = { suggestion ->
           Log.d("LOGIN", "hai cliccato: $suggestion")
            binding.editPosition.setText(suggestion)
            binding.recyclerViewSuggestions.visibility = View.GONE
        }
        val adapter = SuggestionsAdapter(suggestions, onItemClick)
        Log.d("LOGIN", "updateSuggestions: $suggestions")

        recyclerView.adapter = adapter

    }


}