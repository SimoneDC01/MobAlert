package com.example.mobalert

import AdAdapter
import AdAdapterMy
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.example.mobalert.databinding.FragmentAlertsBinding
import com.example.mobalert.ListHomeFragment
import com.example.mobalert.databinding.FragmentListHomeBinding
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
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
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class AlertsFragment : Fragment() {

    private lateinit var binding: FragmentAlertsBinding
    private lateinit var adapter: AdAdapterMy
    private lateinit var auth: FirebaseAuth

    private var filters= mutableMapOf(
        "title" to "",
        "description" to "",
        "dateHour" to "",
        "category" to ""
    )
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LOGIN", "onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.d("LOGIN", "onStart")
    }
    override fun onResume() {
        super.onResume()
        Log.d("LOGIN", "onResume")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance()

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

        binding.MyOrderBy.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, binding.MyOrderBy)
            popupMenu.menu.add(Menu.NONE, 1, 1, "Recent Date")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Old Date")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        Log.d("LOGIN", "Recent Date")
                        adapter.sortByDateDesc()
                    }
                    2 -> {
                        Log.d("LOGIN", "Old Date")
                        adapter.sortByDate()
                    }
                }
                true
            }

            // Mostra il menu popup
            popupMenu.show()
        }

        binding.MyFilter.setOnClickListener {
            val window = PopupWindow(requireContext())
            val view = layoutInflater.inflate(R.layout.filter_layout, null)
            window.contentView = view
            window.isFocusable = true
            window.isOutsideTouchable = true
            window.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.white))
            val button = view.findViewById<Button>(R.id.submitFilter)
            val editText = view.findViewById<EditText>(R.id.title)
            val username=view.findViewById<EditText>(R.id.username)
            username.visibility=View.GONE
            val usernameEdit=view.findViewById<TextView>(R.id.usernameDesc)
            usernameEdit.visibility=View.GONE
            val elemFrom=view.findViewById<EditText>(R.id.dateFrom)
            val reset=view.findViewById<Button>(R.id.Reset)
            val elemTo=view.findViewById<EditText>(R.id.dateTo)
            setupDateTimePicker(elemFrom)
            setupDateTimePicker(elemTo)
            val cat1=view.findViewById<CheckBox>(R.id.Cat1)
            cat1.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat1.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Info"
                    }
                    else{
                        filters["category"]+=",Info"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Info", "")
                    filters["category"] = filters["category"]!!.replace("Info", "")
                    adapter.filter(filters)

                }
            }

            val cat2=view.findViewById<CheckBox>(R.id.Cat2)
            cat2.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat2.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Warning"
                    }
                    else{
                        filters["category"]+=",Warning"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {

                    filters["category"] = filters["category"]!!.replace(",Warning", "")
                    filters["category"] = filters["category"]!!.replace("Warning", "")
                    adapter.filter(filters)

                }
            }

            val cat3=view.findViewById<CheckBox>(R.id.Cat3)
            cat3.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat3.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Emergency"
                    }
                    else{
                        filters["category"]+=",Emergency"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {

                    filters["category"] = filters["category"]!!.replace(",Emergency", "")
                    filters["category"] = filters["category"]!!.replace("Emergency", "")
                    adapter.filter(filters)

                }
            }

            val cat4=view.findViewById<CheckBox>(R.id.Cat4)
            cat4.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat4.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Critical"
                    }
                    else{
                        filters["category"]+=",Critical"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Critical", "")
                    filters["category"] = filters["category"]!!.replace("Critical", "")
                    adapter.filter(filters)

                }
            }

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // Azioni prima che il testo cambi
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Azioni mentre il testo sta cambiando
                    filters["title"] = s.toString()
                    adapter.filter(filters)
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
                    filters["description"] = s.toString()
                    adapter.filter(filters)
                }

                override fun afterTextChanged(s: Editable?) {
                    // Azioni dopo che il testo è cambiato
                    if (s != null) {
                        println("Il testo è ora: $s")
                    }
                }
            })

            button.setOnClickListener {
                val datefrom = view.findViewById<EditText>(R.id.dateFrom)
                val dateto = view.findViewById<EditText>(R.id.dateTo)
                filters["dateHour"] = "${datefrom.text},${dateto.text}"
                adapter.filter(filters)
                window.dismiss()
            }

            reset.setOnClickListener {
                filters["title"] = ""
                filters["description"] = ""
                filters["dateHour"] = ""
                filters["category"] = ""
                adapter.filter(filters)
            }

            window.showAsDropDown(binding.MyFilter)
        }



        return binding.root
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
                        "",
                        alert.title,
                        alert.datehour,
                        alert.type,
                        alert.position,
                        my_images
                    )
                    homealerts.add(payload)
                }
                withContext(Dispatchers.Main) {
                    Log.d("LOGIN", "homealerts: $homealerts")
                    adapter = AdAdapterMy(requireContext(),homealerts,this@AlertsFragment)
                    binding.MyAlertsRv.adapter = adapter
                }

                Log.d("LOGIN", "myalerts: $alerts")
            } else {
                Log.e("LOGIN", "Errore nella richiesta: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "Errore durante la richiesta: $e")
        }
    }

    private fun setupDateTimePicker(elem:EditText) {
        val calendar = Calendar.getInstance()

        // Listener per clic su data/ora
        elem.setOnClickListener {
            // Mostra DatePickerDialog
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Aggiorna la data nel calendario
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Mostra TimePickerDialog
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            // Aggiorna l'ora nel calendario
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)

                            // Formatta data e ora
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val formattedDateTime = sdf.format(calendar.time)

                            // Imposta il valore nel campo di testo
                            elem.setText(formattedDateTime)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true // Usa formato 24 ore
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
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