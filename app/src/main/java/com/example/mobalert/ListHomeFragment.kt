package com.example.mobalert

import AdAdapter
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.http.HttpResponseCache.install
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.example.mobalert.databinding.FragmentListHomeBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class ListHomeFragment : Fragment() {
    private lateinit var binding: FragmentListHomeBinding

    private lateinit var adapter: AdAdapter

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    @SuppressLint("MissingInflatedId")
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

        binding.orderBy.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, binding.orderBy)
            popupMenu.menu.add(Menu.NONE, 1, 1, "Recent Date")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Old Date")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        Log.d("LOGIN", "Recent Date")
                        adapter.sortByDate()
                    }
                    2 -> {
                        Log.d("LOGIN", "Old Date")
                        adapter.sortByDateDesc()
                    }
                }
                true
            }

            // Mostra il menu popup
            popupMenu.show()
        }


        binding.filter.setOnClickListener {
            val window = PopupWindow(requireContext())
            val view = layoutInflater.inflate(R.layout.filter_layout, null)
            window.contentView = view
            window.isFocusable = true
            window.isOutsideTouchable = true
            window.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.white))
            val button = view.findViewById<Button>(R.id.submitFilter)
            val editText = view.findViewById<EditText>(R.id.title)

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // Azioni prima che il testo cambi
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Azioni mentre il testo sta cambiando
                    println("Testo cambiato: $s")
                    adapter.filter("title", s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    // Azioni dopo che il testo è cambiato
                    if (s != null) {
                        println("Il testo è ora: $s")
                    }
                }
            })

            val description = view.findViewById<EditText>(R.id.description)

            description.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // Azioni prima che il testo cambi
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Azioni mentre il testo sta cambiando
                    println("Testo cambiato: $s")
                    adapter.filter("description", s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    // Azioni dopo che il testo è cambiato
                    if (s != null) {
                        println("Il testo è ora: $s")
                    }
                }
            })

            button.setOnClickListener {
                val editText = view.findViewById<EditText>(R.id.title)
                adapter.filter("title", editText.text.toString())
                window.dismiss()
            }
            window.showAsDropDown(binding.filter)
        }

/*
        binding.filter.setOnClickListener{
            adapter.filter("title", "w")
        }
*/
        binding.mapView.setOnClickListener{
            adapter.noFilter()
        }
/*
        binding.order.setOnClickListener {
            adapter.sortByDate()
        }

        binding.orderdesc.setOnClickListener {
            adapter.sortByDateDesc()
        }
*/
        return binding.root
    }

    private suspend fun getAlerts() {
        val url = "${MainActivity.url}/alerts"
        try {
            val response: HttpResponse = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                val alerts: List<Alert> = Json.decodeFromString(response.bodyAsText())
                var homealerts = mutableListOf<HomeAlters>()
                for (alert in alerts) {
                    var my_images: ArrayList<Bitmap?> = ArrayList()
                    var my_images_name : List<String> = alert.image.split(",")
                    for (image in my_images_name) {
                        val bitmap = getImage(alert.id, image)
                        if (bitmap != null) {
                            my_images.add(bitmap)
                        }
                    }
                    //val image = getImage(alert.id,"0")
                    val payload = HomeAlters(
                        alert.id,
                        alert.description,
                        alert.title,
                        alert.datehour,
                        alert.type,
                        alert.position,
                        my_images
                    )
                    homealerts.add(payload)
                }
                withContext(Dispatchers.Main) {
                    if(isAdded){
                        adapter = AdAdapter(requireContext(),homealerts)
                        binding.alertsRv.adapter = adapter
                    }
                    else{
                        Log.d("LOGIN", "Fragment is not attached to the activity")
                    }
                }

                Log.d("LOGIN", "alerts: $alerts")
            } else {
                Log.e("LOGIN", "Errore nella richiesta: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }

    private suspend fun getImage(id : Int, image: String): Bitmap? {
        var url : String
        if (".jpg" in image) url = "${MainActivity.url}/images/${id.toString()}_$image"
        else url = "${MainActivity.url}/images/${id.toString()}_$image.jpg"
        try {
            val response: HttpResponse = client.get(url)
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