package com.example.mobalert

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.mobalert.databinding.FragmentInsertAlertBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.primitives.Bytes
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsertAlertFragment : Fragment() {
    private lateinit var binding: FragmentInsertAlertBinding
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private var imageUri: Uri? = null

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


        setupCategoryDropdown()

        setupDateTimePicker()

        imagesPickedArrayList = ArrayList()

        binding.InsertAlertButton.setOnClickListener {

            var images =""
            for (imagePicked in imagesPickedArrayList) {
                images += imagePicked.id.toString()+".jpg,"
            }
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
                }
                catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.d("LOGIN", "Error: $e")
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
            val modelImagePicked = ModelImagePicked(
                id = 0,
                imageUri = imageUri
            )
            imagesPickedArrayList.add(modelImagePicked)
            Log.d("LOGIN", "arraryList: $imagesPickedArrayList")
            loadImages()
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

            val modelImagePicked = ModelImagePicked(
                id = 0,
                imageUri = imageUri
            )

            imagesPickedArrayList.add(modelImagePicked)

            loadImages()
            /*try {
                Glide.with(this.activity).load(imageUri).into(binding.AlertImage)
            } catch (e: Exception) {
                Log.d("LOGIN", "Error: $e")
            }*/
        }
        else{
            Log.d("LOGIN", "Image Pick Cancelled")
            Toast.makeText(this.activity, "Image Pick Cancelled", Toast.LENGTH_SHORT).show()
        }
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
                            /*
                            listImages.forEachIndexed { index, imageBytes ->
                                append(
                                    "file",
                                    imageBytes,
                                    Headers.build {
                                        append(HttpHeaders.ContentDisposition, "filename=\"$index.jpg\"")
                                    }
                                )
                            }*/
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
        val categories = listOf("Emergency", "Warning", "Info", "Critical")

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



    private fun setupDateTimePicker() {
        val calendar = Calendar.getInstance()

        // Listener per clic su data/ora
        binding.editDateHour.setOnClickListener {
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
                            binding.editDateHour.setText(formattedDateTime)
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
    }



}