package com.example.mobalert

import AdAdapter
import AdAdapterMy
import android.app.DatePickerDialog
import android.app.Dialog
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
import android.widget.DatePicker
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.TimePicker
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
    private lateinit var rootLayout: FrameLayout
    private var loadingSpinner: LoadingSpinner? = null
    private lateinit var dialog: Dialog

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
        dialog = Dialog(requireContext())
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance()
        rootLayout = binding.MyAlerts
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getAlerts()
                withContext(Dispatchers.Main) {
                    showLoading(false)
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
            val editText = view.findViewById<EditText>(R.id.title)
            val username=view.findViewById<EditText>(R.id.username)
            username.visibility=View.GONE
            val usernameEdit=view.findViewById<TextView>(R.id.usernameDesc)
            usernameEdit.visibility=View.GONE
            val elemFrom=view.findViewById<EditText>(R.id.dateFrom)
            val reset=view.findViewById<Button>(R.id.Reset)
            val elemTo=view.findViewById<EditText>(R.id.dateTo)
            val description = view.findViewById<EditText>(R.id.description)

            setupDateTimePicker(elemFrom)
            setupDateTimePicker(elemTo)

            editText.setText(filters["title"])
            description.setText(filters["description"])
            if(filters["dateHour"]!="") {
                elemFrom.setText(filters["dateHour"]!!.split(",")[0])
                elemTo.setText(filters["dateHour"]!!.split(",")[1])
            }
            else{
                elemFrom.setText("")
                elemTo.setText("")
            }

            val cat1=view.findViewById<CheckBox>(R.id.Cat1)
            cat1.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat1.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Natural environmental accident"
                    }
                    else{
                        filters["category"]+=",Natural environmental accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Natural environmental accident", "")
                    filters["category"] = filters["category"]!!.replace("Natural environmental accident", "")
                    adapter.filter(filters)

                }
            }

            val cat2=view.findViewById<CheckBox>(R.id.Cat2)
            cat2.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat2.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Anthropic environmental accident"
                    }
                    else{
                        filters["category"]+=",Anthropic environmental accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {

                    filters["category"] = filters["category"]!!.replace(",Anthropic environmental accident", "")
                    filters["category"] = filters["category"]!!.replace("Anthropic environmental accident", "")
                    adapter.filter(filters)

                }
            }

            val cat3=view.findViewById<CheckBox>(R.id.Cat3)
            cat3.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat3.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Health and biological accident"
                    }
                    else{
                        filters["category"]+=",Health and biological accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {

                    filters["category"] = filters["category"]!!.replace(",Health and biological accident", "")
                    filters["category"] = filters["category"]!!.replace("Health and biological accident", "")
                    adapter.filter(filters)

                }
            }

            val cat4=view.findViewById<CheckBox>(R.id.Cat4)
            cat4.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat4.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Technological accident"
                    }
                    else{
                        filters["category"]+=",Technological accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Technological accident", "")
                    filters["category"] = filters["category"]!!.replace("Technological accident", "")
                    adapter.filter(filters)

                }
            }
            val cat5=view.findViewById<CheckBox>(R.id.Cat5)
            cat5.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat5.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Urban and social accident"
                    }
                    else{
                        filters["category"]+=",Urban and social accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Urban and social accident", "")
                    filters["category"] = filters["category"]!!.replace("Urban and social accident", "")
                    adapter.filter(filters)

                }
            }
            val cat6=view.findViewById<CheckBox>(R.id.Cat6)
            cat6.setOnClickListener {
                // Puoi controllare lo stato della checkbox manualmente
                if (cat6.isChecked) {
                    if(filters["category"]==""){
                        filters["category"]="Marine and aquatic accident"
                    }
                    else{
                        filters["category"]+=",Marine and aquatic accident"
                    }
                    // Azione quando è selezionata
                    adapter.filter(filters)
                } else {


                    filters["category"] = filters["category"]!!.replace(",Marine and aquatic accident", "")
                    filters["category"] = filters["category"]!!.replace("Marine and aquatic accident", "")
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

            elemFrom.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filters["dateHour"] = "${s.toString()},${elemTo.text}"
                    adapter.filter(filters)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

            elemTo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filters["dateHour"] = "${elemFrom.text},${s.toString()}"
                    adapter.filter(filters)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

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

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingSpinner == null) {
                loadingSpinner = LoadingSpinner(requireContext())
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                rootLayout.addView(loadingSpinner, params)
            }
            loadingSpinner?.visibility = View.VISIBLE
        } else {
            loadingSpinner?.let {
                rootLayout.removeView(it) // Rimuovi dal layout
                loadingSpinner = null
            }
        }
    }

    private fun setupDateTimePicker(elem : EditText) {
        elem.setOnClickListener {
            dialog.setContentView(R.layout.date_hour)
            dialog.setCancelable(false)
            dialog.show()
            val datePicker = dialog.findViewById<DatePicker>(R.id.datepicker)
            val deleteDate = dialog.findViewById<TextView>(R.id.deleteDate)
            val okDate = dialog.findViewById<TextView>(R.id.okDate)
            val timePicker = dialog.findViewById<TimePicker>(R.id.timepicker)
            val deleteTime = dialog.findViewById<TextView>(R.id.deleteTime)
            val okTime = dialog.findViewById<TextView>(R.id.okTime)

            timePicker.setIs24HourView(true)

            deleteDate.setOnClickListener {
                elem.setText("")
                dialog.hide()
            }
            deleteTime.setOnClickListener {
                elem.setText("")
                dialog.hide()
            }

            okDate.setOnClickListener {
                datePicker.visibility = View.GONE
                timePicker.visibility = View.VISIBLE
            }

            okTime.setOnClickListener {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR,datePicker.year)
                calendar.set(Calendar.MONTH,datePicker.month)
                calendar.set(Calendar.DAY_OF_MONTH,datePicker.dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY,timePicker.hour)
                calendar.set(Calendar.MINUTE,timePicker.minute)
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val parsedDate = sdf.format(calendar.time)
                elem.setText(parsedDate)
                dialog.hide()
            }
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