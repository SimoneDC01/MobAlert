package com.example.mobalert

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.databinding.FragmentProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.lang.Thread.sleep
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = Firebase.auth
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

    override fun onStart() {
        super.onStart()
        Log.d("LOGIN", "onStart")
        reference.child(auth.uid.toString()).get().addOnSuccessListener {
            binding.fullNameTv.text = it.child("name").value.toString()
            binding.emailTv.text = it.child("email").value.toString()
            val phone = it.child("phoneNumber").value.toString()
            if(phone == "") binding.phoneTv.text = "Not Set"
            else binding.phoneTv.text = phone
            val dob = it.child("dob").value.toString()
            if(dob == "") binding.dobTv.text = "Not Set"
            else binding.dobTv.text = dob
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = getImage(auth.uid.toString())
                withContext(Dispatchers.Main) {
                    Glide.with(requireContext())
                        .load(image)
                        .into(binding.profileIv)
                }
            } catch (e: Exception) {
                // Handle exceptions if necessary
                e.printStackTrace()
            }
        }
        binding.logoutCv.setOnClickListener {
            Log.d("LOGIN", "Logout button clicked")
            auth.signOut()
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.changePasswordCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.editCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.deleteCv.setOnClickListener {
            reference.child(auth.uid.toString()).removeValue().addOnSuccessListener {
                Log.d("LOGIN", "User deleted from database")
            }.addOnFailureListener {
                Log.d("LOGIN", "User not deleted from database")
            }
            auth.currentUser!!.delete().addOnSuccessListener {
                Log.d("LOGIN", "User deleted from firebase")
            }.addOnFailureListener {e->
                Log.d("LOGIN", "User not deleted from firebase $e")
            }

            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        return binding.root
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}