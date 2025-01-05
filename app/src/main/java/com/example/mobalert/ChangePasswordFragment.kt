package com.example.mobalert

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.mobalert.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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

class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private val auth = Firebase.auth
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

        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);

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
        binding.submitBtn.setOnClickListener{
            val currentPassword = binding. passwordEt.text.toString()
            val newPassword = binding. newPasswordEt.text. toString()
            val confirmNewPassword = binding. confirmPasswordEt.text. toString()

            //validate data
            if (currentPassword. isEmpty()){
            //Current Password Field (currentPasswordEt) is empty, show error
                binding.passwordEt.error = "Enter current password"
                binding. passwordEt.requestFocus()
            } else if (newPassword. isEmpty() ) {
                binding.newPasswordEt.error = "Enter new password"
                binding.newPasswordEt.requestFocus()
            }
            else if (confirmNewPassword. isEmpty() ){
                binding.confirmPasswordEt.error = "Enter confirm new password"
                binding.confirmPasswordEt.requestFocus()
            } else if (newPassword != confirmNewPassword) {
                binding.confirmPasswordEt.error = "Password doesn't match"
                binding. confirmPasswordEt.requestFocus()
            } else {
                //before changing password re-authenticate the user to check if the user has entered correct c
                val authCredential =
                    EmailAuthProvider.getCredential(auth.currentUser!!.email!!, currentPassword)
                auth.currentUser!!.reauthenticate(authCredential)
                    .addOnSuccessListener {
                        Log.d("LOGIN", "authenticateUserForUpdatePassword: Authentication success")

                        //begin update password, pass the new password as parameter
                        auth.currentUser!!.updatePassword(newPassword)
                            .addOnSuccessListener {
                                Log.d("LOGIN", "updatePassword: Password updated ... ")
                                Toast.makeText(this.activity, "Password updated", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.Fragment, ProfileFragment())
                                    .addToBackStack(null)
                                    .commit()
                            }
                            .addOnFailureListener { e ->
                                Log.e("LOGIN", "updatePassword: ", e)
                                Toast.makeText(this.activity, "Password update failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LOGIN", "authenticateUserForUpdatePassword: ", e)
                        Toast.makeText(this.activity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Inflate the layout for this fragment
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
}