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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsertAlertFragment : Fragment() {
    private lateinit var binding: FragmentInsertAlertBinding
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private var imageUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Usa il binding per associare la vista
        binding = FragmentInsertAlertBinding.inflate(inflater, container, false)
        // Configura il menu a tendina per le categorie
        setupCategoryDropdown()
        // Configura il selettore di data e ora
        setupDateTimePicker()
        // Imposta il listener sul pulsante
        binding.InsertAlertButton.setOnClickListener {
            Toast.makeText(requireContext(), "Insert", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, HomeFragment())
                .commit()
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
        cameraAcrivityResultLauncher.launch(intent)
    }

    private val cameraAcrivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOGIN", "Image Uri: $imageUri")
            try {
                Glide.with(this.activity).load(imageUri).into(binding.AlertImage)
            }
            catch (e: Exception){
                Log.d("LOGIN", "Error: $e")
            }
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
            try {
                Glide.with(this.activity).load(imageUri).into(binding.AlertImage)
            } catch (e: Exception) {
                Log.d("LOGIN", "Error: $e")
            }
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