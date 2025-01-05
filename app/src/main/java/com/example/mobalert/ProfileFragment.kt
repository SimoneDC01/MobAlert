package com.example.mobalert

import android.content.Context
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        val user=auth.currentUser
        if(user!=null && user.providerData[1]?.providerId=="google.com"){
            binding.changePasswordCv.visibility = View.GONE
        }
        else{
            binding.changePasswordCv.visibility = View.VISIBLE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = getImage(auth.uid.toString())
                    withContext(Dispatchers.Main) {


                        if (image == null) {
                            binding.profileIv.setImageResource(R.drawable.person_black)
                        } else {
                            Glide.with(requireContext())
                                .load(image)
                                .into(binding.profileIv)
                        }

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
            /*
            auth.currentUser!!.delete().addOnSuccessListener {
                Log.d("LOGIN", "User deleted from firebase")
            }.addOnFailureListener {e->
                Log.d("LOGIN", "User not deleted from firebase $e")
            }
             */

            val user = auth.currentUser

            if (user != null) {
                // Controlla il metodo di accesso attuale
                val providerId = user.providerData[1]?.providerId

                when (providerId) {
                    "password" -> { // Utente autenticato con email e password
                        deleteUserWithEmailPassword(user)
                    }
                    "google.com" -> { // Utente autenticato con Google
                        //deleteUserWithGoogle(auth, requireContext(), user)
                        renewGoogleTokenAndDeleteUser(auth, requireContext(), user)
                    }
                    else -> { // Altri metodi di autenticazione
                        Log.e("LOGIN", "Unknown provider: $providerId")
                    }
                }
            } else {
                Log.e("LOGIN", "User is null")
            }

            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }


    private fun renewGoogleTokenAndDeleteUser(auth: FirebaseAuth, context: Context, user: FirebaseUser) {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Sostituisci con il tuo client ID di Firebase
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

        googleSignInClient.silentSignIn()
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                // Ottieni il nuovo ID Token
                val idToken = googleAccount.idToken
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // Ri-autentica l'utente
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        Log.d("DELETE_USER", "User re-authenticated successfully")

                        // Ora elimina l'utente
                        user.delete()
                            .addOnSuccessListener {
                                Log.d("DELETE_USER", "User deleted successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("DELETE_USER", "Failed to delete user", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DELETE_USER", "Re-authentication failed", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DELETE_USER", "Silent sign-in failed", e)
            }
    }

    private fun deleteUserWithEmailPassword(user: FirebaseUser) {
        user.delete()
            .addOnSuccessListener {
                Log.d("LOGIN", "User with email/password deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("LOGIN", "Failed to delete user with email/password", e)
            }
    }

    // Elimina utenti autenticati con Google
    private fun deleteUserWithGoogle(auth: FirebaseAuth, context: Context, user: FirebaseUser) {
        // Ottieni l'account Google collegato
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

        if (googleAccount != null) {
            val googleCredential = GoogleAuthProvider.getCredential(googleAccount.idToken, null)

            // Ri-autentica l'utente con le credenziali Google
            user.reauthenticate(googleCredential)
                .addOnSuccessListener {
                    Log.d("LOGIN", "User re-authenticated with Google")

                    // Ora elimina l'utente
                    user.delete()
                        .addOnSuccessListener {
                            Log.d("LOGIN", "User with Google deleted successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("LOGIN", "Failed to delete Google user", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("DELETE_USER", "Re-authentication with Google failed", e)
                }
        } else {
            Log.e("DELETE_USER", "No Google account found for re-authentication")
        }
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