package com.example.mobalert

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.example.mobalert.databinding.FragmentEditProfileBinding
import com.yalantis.ucrop.UCrop
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar


class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private var ImageProfile: Uri?=null
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private var imageUri: Uri? = null
    private var image: Bitmap?=null
    private var delete: Boolean = false

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("LOGIN", "onStart")
        reference.child(auth.uid.toString()).get().addOnSuccessListener {
            binding.profileTv.text = it.child("name").value.toString()
            binding.editName.setText(it.child("name").value.toString())
            val phone = it.child("phoneNumber").value.toString()
            if(phone != "") binding.editPhone.setText(phone)
            val dob = it.child("dob").value.toString()
            if(dob != "") binding.editDob.setText(dob)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        CoroutineScope(Dispatchers.IO).launch {
            try {
                image = getImage(auth.uid.toString())
                if(image==null){
                    binding.profileIv.setImageResource(R.drawable.person_black)
                }
                else{
                withContext(Dispatchers.Main) {
                    Glide.with(requireContext())
                        .load(image)
                        .into(binding.profileIv)
                }
                }
            } catch (e: Exception) {
                // Handle exceptions if necessary
                e.printStackTrace()
            }
        }




        binding.updateProfileButton.setOnClickListener {
            if (binding.editName.text.toString() == "") {
                binding.editName.error = "Please enter a name"
                binding.editName.requestFocus()
            }
            else{
            val hashMap = HashMap<String, Any>()
            hashMap["name"] = binding.editName.text.toString()
            hashMap["phoneNumber"] = binding.editPhone.text.toString()
            hashMap["dob"] = binding.editDob.text.toString()

            reference.child(auth.uid.toString()).updateChildren(hashMap).addOnSuccessListener {
                Log.d("LOGIN", "updateUserInfo: Info saved")
            }
                .addOnFailureListener { e ->
                    Log.e("LOGIN", "updateUserInfo: ", e)
                }
            if (ImageProfile != null) {
                val imageBytes = requireContext().contentResolver.openInputStream(ImageProfile!!)
                    ?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: throw IllegalArgumentException("Unable to read bytes from input stream ")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response: io.ktor.client.statement.HttpResponse =
                            client.post("${MainActivity.url}/InsertProfileImage") {
                                contentType(ContentType.MultiPart.FormData)
                                setBody(
                                    MultiPartFormDataContent(
                                        formData {
                                            append("file", imageBytes, Headers.build {
                                                append(
                                                    HttpHeaders.ContentDisposition,
                                                    "filename=${auth.uid}.jpg"
                                                )
                                            })
                                        }
                                    )
                                )
                            }

                        if (response.status == HttpStatusCode.Created) {
                            val result = response.bodyAsText()
                            Log.d("LOGIN", "Server response: $result")
                        } else {
                            Log.e("LOGIN", "Request error: ${response.status}")
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN", "Error during request: $e")
                    }
                }
            } else if (imageUri == null && delete == true) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        DeleteProfileImage(auth.uid.toString())
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.d("LOGIN", "Error: $e")
                        }
                    }
                }
            }
            runBlocking { // Avvia una coroutine di base
                delay(200)
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        }

        binding.editDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            Log.d("LOGIN", "editDob clicked")
            val datePickerDialog = DatePickerDialog(
                this.requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    binding.editDob.setText(selectedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        binding.imageButton.setOnClickListener {
            val popupMenu = PopupMenu(this.context, binding.imageButton)
            popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
            popupMenu.menu.add(Menu.NONE, 3, 3, "Edit")
            popupMenu.menu.add(Menu.NONE, 4, 4, "Delete")
            if(image == null){
                popupMenu.menu.removeItem(4)
                popupMenu.menu.removeItem(3)
            }
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        delete=false
                        Log.d("LOGIN", "imagePickDialog: Camera Clicked")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestCameraPermission.launch(arrayOf(
                                android.Manifest.permission.CAMERA
                            ))
                        } else {
                            requestCameraPermission.launch(arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ))
                        }
                    }
                    2 -> {
                        Log.d("LOGIN", "imagePickDialog: Gallery Clicked")
                        pickImageGallery()
                        delete=false
                    }
                    3 -> {
                        Log.d("LOGIN", "imagePickDialog: Edit Clicked")
                        startCrop(bitmapToUri(requireContext(), image!!))
                        delete=false
                    }
                    4 -> {
                        Log.d("LOGIN", "imagePickDialog: Delete Clicked")
                        binding.profileIv.setImageResource(R.drawable.person_black)
                        delete=true
                    }
                }
                true
            }
        }


        return binding.root
    }


    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        // Creare un file temporaneo nella cache directory
        val file = File(context.cacheDir, "temp_image.png") // Nome del file
        return try {
            // Scrivere il bitmap nel file
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Restituire l'URI del file
            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allAreGranted = result.values.all { it }

        if (allAreGranted) {
            Log.d("LOGIN", "Permission granted")
            pickImageCamera()
        } else {
            Log.d("LOGIN", "Permission denied")
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Temp Image")
            put(MediaStore.Images.Media.DESCRIPTION, "Temp Image Description")
        }

        imageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }

        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOGIN", "Image Uri: $imageUri")
            startCrop(imageUri)
        } else {
            Log.d("LOGIN", "Image Pick Cancelled")
            Toast.makeText(this.activity, "Image Pick Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageGallery() {

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }

        galleryActivityResultLauncher.launch(intent)

    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            if (imageUri != null) {
                startCrop(imageUri)
            }
        }
    }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                ImageProfile = resultUri
                Log.d("LOGIN", "Cropped Image URI: $resultUri")
                Glide.with(requireContext())
                    .load(resultUri)
                    .into(binding.profileIv)

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



    private suspend fun getImage(image: String): Bitmap? {
        var url : String
        url = "${MainActivity.url}/images/$image.jpg"
        try {
            val response: io.ktor.client.statement.HttpResponse = client.get(url)
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

    suspend fun DeleteProfileImage(image: String) {
        Log.d("LOGIN", "DeleteProfileImage: $image")
        val url = "${MainActivity.url}/deleteimages/$image.jpg"
        try {
            val response: io.ktor.client.statement.HttpResponse = client.delete(url)
            when (response.status) {
                HttpStatusCode.OK -> Log.d("LOGIN", "Image con ID $image eliminato con successo.")
                HttpStatusCode.NotFound -> Log.e("LOGIN", "Image con ID $image non trovato.")
                else -> Log.e("LOGIN", "Errore nell'eliminazione: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.profileTv.text = auth.currentUser?.displayName
        super.onViewCreated(view, savedInstanceState)
        if(auth.currentUser?.displayName != "") binding.nameTil.editText?.setText(auth.currentUser?.displayName)
        if(auth.currentUser?.phoneNumber != "") binding.phoneTil.editText?.setText(auth.currentUser?.phoneNumber)
    }

}