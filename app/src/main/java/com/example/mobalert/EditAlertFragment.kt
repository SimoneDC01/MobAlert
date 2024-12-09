package com.example.mobalert

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.mobalert.databinding.FragmentAlertsBinding
import com.example.mobalert.databinding.FragmentEditAlertBinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class EditAlertFragment : Fragment() {

    private lateinit var binding: FragmentEditAlertBinding

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
        binding = FragmentEditAlertBinding.inflate(inflater, container, false);


        val alertId = arguments?.getInt("alertId") // Assicurati di avere un campo "alertId" nel bundle
        val alertTitle = arguments?.getString("alertTitle") // Altro campo, se passato
        val alertDescription = arguments?.getString("alertDescription")
        val alertCategory = arguments?.getString("alertCategory")
        setupCategoryDropdown(alertCategory!!)
        binding.editTitle.setText(alertTitle)
        binding.editDescription.setText(alertDescription)
        binding.editAlertButton.setOnClickListener {

            val updatedAlert = HomeFragment.UpdateAlert(
                binding.editDescription.text.toString(),
                binding.editCategory.text.toString(),
                binding.editTitle.text.toString()
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    updateAlert(alertId!!, updatedAlert)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Edited", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.Fragment, AlertsFragment())
                            .commit()
                    }
                }
                catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                    }
                }
            }
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun setupCategoryDropdown(alertCategory: String) {
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
        when(alertCategory){
            "Emergency" -> binding.editCategory.setText(categories[0], false)
            "Warning" -> binding.editCategory.setText(categories[1], false)
            "Info" -> binding.editCategory.setText(categories[2], false)
            "Critical" -> binding.editCategory.setText(categories[3], false)
        }

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

    suspend fun updateAlert(itemId: Int, update: HomeFragment.UpdateAlert) {
        val url = "${MainActivity.url}/alerts/$itemId"
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
    }
}