package com.example.mobalert

import android.app.Activity
import android.app.DatePickerDialog
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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.mobalert.databinding.FragmentEditProfileBinding
import com.example.mobalert.databinding.FragmentProfileBinding
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.Calendar


class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")
    private var imageUri: Uri? = null

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


        binding.updateProfileButton.setOnClickListener {
            val hashMap = HashMap<String, Any>()
            hashMap["name"] = binding.editName.text.toString()
            hashMap["phoneNumber"] = binding.editPhone.text.toString()
            hashMap["dob"] = binding.editDob.text.toString()

            reference.child(auth.uid.toString()).updateChildren(hashMap).
            addOnSuccessListener {
                Log.d("LOGIN", "updateUserInfo: Info saved")
            }
            .addOnFailureListener { e ->
                Log.e("LOGIN", "updateUserInfo: ", e)
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
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
            //init popup menu param 1 is context and param 2 is the UI View (profileIma
            val popupMenu = PopupMenu( this.context, binding.imageButton)
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
                Glide.with(this.activity).load(imageUri).into(binding.profileIv)
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
                Glide.with(this.activity).load(imageUri).into(binding.profileIv)
            } catch (e: Exception) {
                Log.d("LOGIN", "Error: $e")
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.profileTv.text = auth.currentUser?.displayName
        super.onViewCreated(view, savedInstanceState)
        if(auth.currentUser?.displayName != "") binding.nameTil.editText?.setText(auth.currentUser?.displayName)
        if(auth.currentUser?.phoneNumber != "") binding.phoneTil.editText?.setText(auth.currentUser?.phoneNumber)
    }

}