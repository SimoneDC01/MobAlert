package com.example.mobalert

import AdAdapter
import android.net.http.HttpResponseCache.install
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.example.mobalert.databinding.FragmentListHomeBinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class ListHomeFragment : Fragment() {
    private lateinit var binding: FragmentListHomeBinding

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
        binding = FragmentListHomeBinding.inflate(inflater, container, false);

        CoroutineScope(Dispatchers.IO).launch {
            try {
                getAlerts()
                withContext(Dispatchers.Main) {
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }

        return binding.root
    }

    private suspend fun getAlerts() {
        val url = "https://deep-jaybird-exotic.ngrok-free.app/alerts"
        try {
            val response: HttpResponse = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                val alerts: List<Alert> = Json.decodeFromString(response.bodyAsText())
                var homealerts = mutableListOf<HomeAlters>()
                for (alert in alerts) {
                    val payload = HomeAlters(
                        alert.description,
                        alert.title,
                        alert.datehour,
                        R.drawable.image_white
                    )
                    homealerts.add(payload)
                }
                withContext(Dispatchers.Main) {
                    val adapter = AdAdapter(homealerts)
                    binding.alertsRv.adapter = adapter
                }

                Log.d("LOGIN", "alerts: $alerts")
            } else {
                Log.e("LOGIN", "Errore nella richiesta: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }

}