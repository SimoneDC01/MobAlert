package com.example.mobalert

import AdAdapter
import android.annotation.SuppressLint
import android.app.Dialog
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
import androidx.fragment.app.FragmentManager
import com.example.mobalert.HomeFragment.Alert
import com.example.mobalert.HomeFragment.HomeAlters
import com.example.mobalert.databinding.FragmentListHomeBinding
import com.google.firebase.database.FirebaseDatabase
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class ListHomeFragment : Fragment() {
    private lateinit var binding: FragmentListHomeBinding

    private lateinit var adapter: AdAdapter
    private lateinit var dialog: Dialog
    private var alerts = mutableListOf<Alert>()
    private lateinit var rootLayout: FrameLayout
    private var loadingSpinner: LoadingSpinner? = null
    private lateinit var mapFragmentManager: FragmentManager
    private lateinit var position: String

    private var filters= mutableMapOf(
        "title" to "",
        "description" to "",
        "username" to "",
        "dateHour" to "",
        "category" to ""
    )

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

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog = Dialog(requireContext())
        binding = FragmentListHomeBinding.inflate(inflater, container, false);
        HomeFragment.homeBinding?.addAlert?.visibility = View.VISIBLE
        rootLayout = binding.loadingSpinner
        showLoading(true)

        position = arguments?.getString("position").toString()
        Log.d("LOGIN", "position list home $position")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                getAlerts()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    binding.orderBy.setOnClickListener { view ->
                        val popupMenu = PopupMenu(view.context, binding.orderBy)
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
                        popupMenu.show()
                    }


                    binding.filter.setOnClickListener {
                        val window = PopupWindow(requireContext())
                        val view = layoutInflater.inflate(R.layout.filter_layout, null)
                        window.contentView = view
                        window.isFocusable = true
                        window.isOutsideTouchable = true
                        window.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.white))

                        val title = view.findViewById<EditText>(R.id.editTitle)
                        val username=view.findViewById<EditText>(R.id.editUsername)
                        val description = view.findViewById<EditText>(R.id.editDescription)
                        val elemFrom=view.findViewById<EditText>(R.id.dateFrom)
                        val reset=view.findViewById<Button>(R.id.Reset)
                        val elemTo=view.findViewById<EditText>(R.id.dateTo)
                        setupDateTimePicker(elemFrom)
                        setupDateTimePicker(elemTo)

                        title.setText(filters["title"])
                        username.setText(filters["username"])
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
                        setUpCategory(cat1,"Natural environmental accident")

                        val cat2=view.findViewById<CheckBox>(R.id.Cat2)
                        setUpCategory(cat2,"Anthropic environmental accident")

                        val cat3=view.findViewById<CheckBox>(R.id.Cat3)
                        setUpCategory(cat3,"Health and biological accident")

                        val cat4=view.findViewById<CheckBox>(R.id.Cat4)
                        setUpCategory(cat4,"Technological accident")

                        val cat5=view.findViewById<CheckBox>(R.id.Cat5)
                        setUpCategory(cat5,"Urban and social accident")

                        val cat6=view.findViewById<CheckBox>(R.id.Cat6)
                        setUpCategory(cat6,"Marine and aquatic accident")


                        setUpEditText(title, "title")

                        setUpEditText(username, "username")

                        setUpEditText(description, "description")

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
                            //TO-DO: RESETTARE LA UI
                            filters["title"] = ""
                            filters["description"] = ""
                            filters["username"] = ""
                            filters["dateHour"] = ""
                            filters["category"] = ""
                            adapter.filter(filters)
                            window.dismiss()
                        }

                        window.showAsDropDown(binding.filter)
                    }

                    binding.mapView.setOnClickListener{

                        val bundle = Bundle()
                        bundle.putSerializable("alerts", ArrayList(alerts))

                        val fragment = MapFragment()
                        fragment.arguments = bundle

                        val transaction = mapFragmentManager.beginTransaction()
                        transaction.replace(R.id.alerts_fragment, fragment)
                        transaction.addToBackStack(null)
                        transaction.commit()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }

        mapFragmentManager = requireActivity().supportFragmentManager





        return binding.root
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

    private fun setUpCategory(elem: CheckBox, category: String){
        elem.isChecked = filters["category"]!!.contains(category)
        elem.setOnClickListener {
            // Puoi controllare lo stato della checkbox manualmente
            if (elem.isChecked) {
                if(filters["category"]==""){
                    filters["category"]=category
                }
                else{
                    filters["category"]+=",$category"
                }
                // Azione quando è selezionata
                adapter.filter(filters)
            } else {


                filters["category"] = filters["category"]!!.replace(",$category", "")
                filters["category"] = filters["category"]!!.replace(category, "")
                adapter.filter(filters)

            }
        }
    }

    private fun setUpEditText(elem: EditText, field: String){
        elem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filters[field] = s.toString()
                adapter.filter(filters)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private suspend fun getAlerts() {
        val url = "${MainActivity.url}/alerts"
        try {
            val response: HttpResponse = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                alerts = Json.decodeFromString(response.bodyAsText())
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

                    reference.child(alert.iduser.toString()).get().addOnSuccessListener {
                        Log.d("LOGIN","${it.child("name").value.toString()}")
                        val payload = HomeAlters(
                            alert.id,
                            alert.description,
                            it.child("name").value.toString(),
                            alert.title,
                            alert.datehour,
                            alert.type,
                            alert.position,
                            my_images
                        )
                        homealerts.add(payload)
                    }
                    //val image = getImage(alert.id,"0")

                }
                withContext(Dispatchers.Main) {
                    if(isAdded){
                        while(homealerts.size<alerts.size) {delay(100)}

                        adapter = AdAdapter(requireContext(),mapFragmentManager, homealerts, alerts)
                        binding.alertsRv.adapter = adapter
                    }
                    else{
                        Log.d("LOGIN", "Fragment is not attached to the activity")
                    }
                }

                if(position != "null") {
                    val bundle = Bundle()
                    bundle.putSerializable("alerts", ArrayList(alerts))
                    bundle.putString("position", position)
                    val fragment = MapFragment()
                    fragment.arguments = bundle
                    val transaction = mapFragmentManager.beginTransaction()
                    transaction.replace(R.id.alerts_fragment, fragment)
                    transaction.commit()
                }
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



}