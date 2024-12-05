package com.example.mobalert

import AdAdapter
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
        val iduser: String,
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

    @Serializable
    data class HomeAlters(
        val description: String,
        val title: String,
        val datehour: String,
        @Contextual
        val image: Bitmap?
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        referece = database.reference.child("Users")

        goToFragment(ListHomeFragment())


        /*
        binding.insert.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val payload = Alert(
                        "2024-12-03T12:00:00",
                        "Descrizione di esempio",
                        1,
                        "Titolo di esempio",
                        "Posizione di esempio",
                        "123456789",
                        "Tipo di esempio"
                    )
                    insertAlert(payload)
                    withContext(Dispatchers.Main) {
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                    }
                }
            }
        }

        binding.delete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    deleteAlert(1)
                    withContext(Dispatchers.Main) {
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                    }
                }
            }
        }

        binding.update.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val payload = UpdateAlert(
                        "Descrizione aggiornata",
                        "Tipo aggiornato",
                        "Titolo aggiornato"
                    )
                    updateAlert(2, payload)
                    withContext(Dispatchers.Main) {
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                    }
                }
            }
        }

         */
        return binding.root
    }


    private fun goToFragment(fragment: Fragment) {
        Log.d("LOGIN", "goToFragment")
        parentFragmentManager.beginTransaction()
            .replace(R.id.alerts_fragment, fragment)
            .commit()

    }


/*
    suspend fun insertAlert(alert: Alert) {
        val url = "https://deep-jaybird-exotic.ngrok-free.app/alerts"
        try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(alert)
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

    suspend fun deleteAlert(itemId: Int) {
        val url = "https://deep-jaybird-exotic.ngrok-free.app/alerts/$itemId"
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

    suspend fun updateAlert(itemId: Int, update: UpdateAlert) {
        val url = "https://deep-jaybird-exotic.ngrok-free.app/alerts/$itemId"
        try {
            val response: HttpResponse = client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(update)
            }
            when (response.status) {
                HttpStatusCode.OK -> Log.d("LOGIN", "Alert con ID $itemId aggiornato con successo.")
                HttpStatusCode.NotFound -> Log.e("LOGIN", "Alert con ID $itemId non trovato.")
                else -> Log.e("LOGIN", "Errore nell'aggiornamento: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }*/


}