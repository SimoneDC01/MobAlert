package com.example.mobalert

import AdAdapter
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobalert.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Calendar


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var referece: DatabaseReference


    @Serializable
    data class Alert(
        val datehour: String,
        val description: String,
        val id: Int,
        var iduser: String,
        val position: String,
        val title: String,
        val type: String,
        val image: String
    )

    @Serializable
    data class UpdateAlert(
        val description: String,
        val type: String,
        val title: String
    )

    data class HomeAlters(
        val id: Int,
        val description: String,
        val username: String="",
        val title: String,
        val datehour: String,
        val type: String,
        val position: String,
        val image: ArrayList<Bitmap?>,
        var visible: Boolean = true
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeBinding = binding
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        referece = database.reference.child("Users")

        var position = arguments?.getString("position").toString()
        if(position != "null"){
            val bundle = Bundle()
            bundle.putString("position", position)

            val fragment = ListHomeFragment()
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.alerts_fragment, fragment) // Sostituisci il fragment attuale
                .addToBackStack(null) // Opzionale, aggiunge il fragment alla back stack
                .commit() // Applica la transazione
        }


        binding.addAlert.setOnClickListener {
            goToFragment2(InsertAlertFragment())
        }

        goToFragment(ListHomeFragment())

        return binding.root
    }


    private fun goToFragment(fragment: Fragment) {
        Log.d("LOGIN", "goToFragment")
        parentFragmentManager.beginTransaction()
            .replace(R.id.alerts_fragment, fragment)
            .commit()

    }
    private fun goToFragment2(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
        .replace(R.id.Fragment, fragment) // Sostituisci il fragment attuale
        .addToBackStack(null) // Opzionale, aggiunge il fragment alla back stack
        .commit() // Applica la transazione
    }

    companion object{
        var homeBinding: FragmentHomeBinding? = null
    }
}