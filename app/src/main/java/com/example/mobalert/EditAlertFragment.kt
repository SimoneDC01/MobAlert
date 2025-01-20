package com.example.mobalert

import android.app.Activity
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
import com.example.mobalert.databinding.FragmentEditAlertBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yalantis.ucrop.UCrop
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Thread.sleep


class EditAlertFragment : Fragment() {

    private lateinit var binding: FragmentEditAlertBinding
    private var imageUri: Uri? = null
    private val auth = Firebase.auth
    private lateinit var imagesPickedArrayList: ArrayList<ModelImagePicked>

    private lateinit var adapterImagePicked: AdapterImagePicked

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditAlertBinding.inflate(inflater, container, false);

        imagesPickedArrayList = ArrayList()

        val alertId = arguments?.getInt("alertId")
        val alerturistring= arguments?.getStringArrayList("alertImages")
        if (alerturistring != null) {
            for (uri in alerturistring) {
                val modelImagePicked = ModelImagePicked(
                    id = 0,
                    imageUri = Uri.parse(uri)
                )
                imagesPickedArrayList.add(modelImagePicked)
            }
            loadImages()
        }

        val alertPosition = arguments?.getString("alertPosition")
        val alertDate = arguments?.getString("alertDate")
        val alertTitle = arguments?.getString("alertTitle") // Altro campo, se passato
        val alertDescription = arguments?.getString("alertDescription")
        val alertCategory = arguments?.getString("alertCategory")
        setupCategoryDropdown(alertCategory!!)
        binding.editTitle.setText(alertTitle)
        binding.editDescription.setText(alertDescription)


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


        binding.editAlertButton.setOnClickListener {
            var images =""
            for (imagePicked in imagesPickedArrayList) {
                images += imagePicked.id.toString()+".jpg,"
            }

            if (binding.editTitle.text.toString().isEmpty()) {
            binding.editTitle.error = "Enter Title"
            binding.editTitle.requestFocus()
        }   else if (binding.editCategory.text.toString().isEmpty()) {
                binding.editCategory.error = "Enter Category"
                binding.editCategory.requestFocus()
            }
            else if (binding.editDescription.text.toString().isEmpty()) {
                binding.editDescription.error = "Enter Description"
                binding.editDescription.requestFocus()
            }
            else if(images==""){
                Toast.makeText(requireContext(), "Insert Images", Toast.LENGTH_SHORT).show()
            }

            else {
                images = images.substring(0, images.length - 1)
                val updatedAlert = HomeFragment.Alert(
                    alertDate!!,
                    binding.editDescription.text.toString(),
                    0,
                    auth.currentUser!!.uid,
                    alertPosition!!,
                    binding.editTitle.text.toString(),
                    binding.editCategory.text.toString(),
                    images
                )
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        deleteAlert(alertId!!)
                        insertAlert(updatedAlert, imagesPickedArrayList[0])
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Edited", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.Fragment, AlertsFragment())
                                .commit()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                        }
                    }
                }
            }
        }

        // Inflate the layout for this fragment
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
            if (imageUri != null) {
                sleep(700)
                startCrop(imageUri)
            }
            else {
                Toast.makeText(requireContext(), "Please, retake image", Toast.LENGTH_SHORT).show()
            }

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
            if (imageUri != null) {
                sleep(700)
                startCrop(imageUri)
            }
            else {
                Toast.makeText(requireContext(), "Please, retake image", Toast.LENGTH_SHORT).show()
            }


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



    private fun setupCategoryDropdown(alertCategory: String) {
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
        when(alertCategory){
            "Natural environmental accident" -> binding.editCategory.setText(categories[0], false)
            "Anthropic environmental accident" -> binding.editCategory.setText(categories[1], false)
            "Health and biological accident" -> binding.editCategory.setText(categories[2], false)
            "Technological accident" -> binding.editCategory.setText(categories[3], false)
            "Urban and social accident" -> binding.editCategory.setText(categories[4], false)
            "Marine and aquatic accident" -> binding.editCategory.setText(categories[5], false)
        }

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


    suspend fun deleteAlert(itemId: Int) {
        val url = "${MainActivity.url}/alerts/$itemId"
        try {
            val response: HttpResponse = client.delete(url)
            when (response.status) {
                HttpStatusCode.OK -> Log.d("LOGIN", "Alert con ID $itemId eliminato con successo.")
                HttpStatusCode.NotFound -> Log.e("LOGIN", "Alert con ID $itemId non trovato.")
                else -> Log.e("LOGIN", "Errore nell'eliminazione: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }


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

}