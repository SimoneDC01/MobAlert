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
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
                if(image==null){
                    binding.profileIv.setImageResource(R.drawable.person_black)
                }
                else {
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

            //TO-DO ELIMINARE ANCHE GLI ALERT E L IMMAGINE PROFILO

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    DeleteProfileImage(auth.uid.toString())
                    getAlerts()
                } catch (e: Exception) {
                    // Handle exceptions if necessary
                    e.printStackTrace()
                }
            }

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

    private suspend fun getAlerts() {
        Log.d("LOGIN", "id: ${auth.uid}")
        val url = "${MainActivity.url}/myalerts/${auth.uid}"
        try {
            val response: HttpResponse = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                val alerts: List<Alert> = Json.decodeFromString(response.bodyAsText())
                var homealerts = mutableListOf<HomeAlters>()
                Log.d("LOGIN", "alerts: $alerts")
                for (alert in alerts) {
                    val alertId=alert.id
                    deleteAlert(alertId)
                }
                Log.d("LOGIN", "myalerts: $alerts")
            } else {
                Log.e("LOGIN", "Errore nella richiesta: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}