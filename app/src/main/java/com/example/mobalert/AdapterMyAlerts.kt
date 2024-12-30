import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.widget.ViewPager2
import com.example.mobalert.AdapterImageSlider
import com.example.mobalert.AlertsFragment
import com.example.mobalert.EditAlertFragment
import com.example.mobalert.HomeFragment
import com.example.mobalert.InsertAlertFragment
import com.example.mobalert.LoadingSpinner
import com.example.mobalert.MainActivity
import com.example.mobalert.ModelImageSlider
import com.example.mobalert.R
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdAdapterMy(private val context: Context, private val ads: MutableList<HomeFragment.HomeAlters>,
                  private val parentFragment: Fragment) : RecyclerView.Adapter<AdAdapterMy.AdViewHolder>() {


    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    // ViewHolder per gli elementi della RecyclerView
    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.myroot)
        val imageIv: ViewPager2  = itemView.findViewById(R.id.myimageIv)
        val titleTv: TextView = itemView.findViewById(R.id.mytitleTv)
        val descriptionTv: TextView = itemView.findViewById(R.id.mydescriptionTv)
        val dateTv: TextView = itemView.findViewById(R.id.mydateTv)
        val categoryTv: TextView = itemView.findViewById(R.id.mycategoryTv)
        val positionTv: TextView = itemView.findViewById(R.id.mypositionTv)
        val optionsButton: TextView =itemView.findViewById(R.id.myoptions_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        // Infla il layout dell'elemento
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.myalert_el, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {


        // Popola i dati
        val ad = ads[position]
        Log.d("LOGIN", "onBindViewHolder: $ad")
        //holder.imageIv.setImageBitmap(ad.image)
        holder.titleTv.text = ad.title
        holder.descriptionTv.text = ad.description
        holder.dateTv.text = ad.datehour
        holder.categoryTv.text = ad.type
        holder.positionTv.text = ad.position

        if (ad.visible) {
            holder.root.visibility = View.VISIBLE
        }
        else {
            Log.d("LOGIN", "GONE")
            holder.root.visibility = View.GONE
        }
        var images = ArrayList<ModelImageSlider>()
        for (image in ad.image) {
            images.add(ModelImageSlider(image!!))
        }
        var adapter = AdapterImageSlider(context, images)
        holder.imageIv.adapter = adapter

        holder.optionsButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, holder.optionsButton)
            popupMenu.menu.add(Menu.NONE, 1, 1, "Edit Alert")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Delete Alert")

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    1 -> {
                        Log.d("LOGIN", "Edit Alert")

                        // Naviga al nuovo Fragment
                        val fragmentManager = parentFragment.parentFragmentManager
                        val transaction = fragmentManager.beginTransaction()
                        val fragment = EditAlertFragment() // Crea una nuova istanza del Fragment di destinazione

                        // Passa dati se necessario (ad esempio l'ID dell'alert)
                        val bundle = Bundle()
                        bundle.putInt("alertId", ad.id)
                        bundle.putString("alertPosition", ad.position)
                        bundle.putString("alertDate", ad.datehour)
                        bundle.putString("alertTitle", ad.title)
                        bundle.putString("alertDescription", ad.description)
                        bundle.putString("alertCategory", ad.type)
                        var uriStringList = ArrayList<String>()
                        for (image in ad.image) {
                            uriStringList.add(bitmapToUri(context, image!!).toString())
                        }
                        bundle.putStringArrayList("alertImages", uriStringList);
                        fragment.arguments = bundle
                        // Sostituisci il Fragment corrente con quello nuovo
                        transaction.replace(R.id.Fragment, fragment)
                        transaction.addToBackStack(null) // Aggiungi alla backstack per consentire il "torna indietro"
                        transaction.commit()

                    }
                    2 -> {

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                deleteAlert(ad.id)
                                withContext(Dispatchers.Main) {

                                    Log.d("LOGIN", "Delete Alert")
                                    parentFragment.parentFragmentManager.beginTransaction()
                                        .replace(R.id.Fragment, AlertsFragment())
                                        .commit()
                                }
                            }
                            catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Log.d("LOGIN", "Error: $e")
                                }
                            }
                        }
                    }
                }
                true
            }

            // Mostra il menu popup
            popupMenu.show()
        }

        holder.root.setOnClickListener {
            Log.d("LOGIN", "CLICK ${holder.imageIv.layoutParams.width}")
            if(holder.imageIv.layoutParams.width == 360) {
                holder.imageIv.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                holder.imageIv.layoutParams.height = 1000

                var params = holder.titleTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.END_OF)
                params.addRule(RelativeLayout.BELOW, R.id.myimageIv)
                holder.titleTv.layoutParams = params


                params = holder.descriptionTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.END_OF)
                holder.descriptionTv.layoutParams = params

                params = holder.categoryTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.END_OF)
                holder.categoryTv.layoutParams = params
                holder.categoryTv.visibility = View.VISIBLE

                params = holder.dateTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.END_OF)
                holder.dateTv.layoutParams = params

                holder.imageIv.requestLayout()
                holder.titleTv.requestLayout()
                holder.descriptionTv.requestLayout()
                holder.categoryTv.requestLayout()
                holder.dateTv.requestLayout()
            }
            else{
                holder.imageIv.layoutParams.width = 360
                holder.imageIv.layoutParams.height = 360

                var params = holder.titleTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.BELOW)
                params.addRule(RelativeLayout.END_OF, R.id.myimageIv)
                holder.titleTv.layoutParams = params


                params = holder.descriptionTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.myimageIv)
                holder.descriptionTv.layoutParams = params

                params = holder.categoryTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.myimageIv)
                holder.categoryTv.layoutParams = params

                params = holder.dateTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.myimageIv)
                holder.dateTv.layoutParams = params

            }

            holder.imageIv.requestLayout()
            holder.titleTv.requestLayout()
            holder.descriptionTv.requestLayout()
            holder.categoryTv.requestLayout()
            holder.dateTv.requestLayout()
        }

    }
    override fun getItemCount(): Int = ads.size

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

    fun filter(filters: MutableMap<String, String>){
        Log.d("LOGIN", "${filters}")
        val newList = ads.map { it.copy() }.toMutableList()
        for (ad in newList) {
            Log.d("LOGIN","${ad}")
            ad.visible=true

            if (!ad.title.contains(filters["title"]!!, ignoreCase = true)) {
                ad.visible = false
            }

            if (!ad.description.contains(filters["description"]!!, ignoreCase = true)) {
                ad.visible = false
            }
            if(filters["category"]!="") {
                if (!filters["category"]!!.contains(ad.type, ignoreCase = true)) {
                    ad.visible = false
                }
            }


            if(filters["dateHour"]!="") {
                val dates = filters["dateHour"]!!.split(",")
                val dateFrom = dates[0]
                val dateTo = dates[1]
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                val dateFromParsed: LocalDateTime
                val dateToParsed: LocalDateTime
                if (dateFrom.isNotEmpty()) {
                    dateFromParsed = LocalDateTime.parse(dateFrom, formatter)
                    if (dateTo.isNotEmpty()) {
                        dateToParsed = LocalDateTime.parse(dateTo, formatter)
                        if (LocalDateTime.parse(ad.datehour, formatter)
                                .isBefore(dateFromParsed) || LocalDateTime.parse(
                                ad.datehour,
                                formatter
                            )
                                .isAfter(dateToParsed)
                        ) {
                            ad.visible = false
                        }
                    } else {
                        if (LocalDateTime.parse(ad.datehour, formatter)
                                .isBefore(dateFromParsed)
                        ) {
                            ad.visible = false
                        }
                    }
                } else if (dateTo.isNotEmpty()) {
                    dateToParsed = LocalDateTime.parse(dateTo, formatter)
                    if (LocalDateTime.parse(ad.datehour, formatter)
                            .isAfter(dateToParsed)
                    ) {
                        ad.visible = false
                    }
                }
            }


        }
        updateList(newList)
    }

    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        // Creare un file temporaneo nella cache directory
        val file = File(context.cacheDir, "image_" + System.currentTimeMillis() + ".png") // Nome del file
        return try {
            // Scrivere il bitmap nel file
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Restituire l'URI del file
            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    fun sortByDate() {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val sortedList = ads.sortedBy{ LocalDateTime.parse(it.datehour, formatter)}
        updateList(sortedList)
    }

    fun sortByDateDesc() {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val sortedList = ads.sortedByDescending{ LocalDateTime.parse(it.datehour, formatter)}
        updateList(sortedList)
    }

    fun updateList(newAds: List<HomeFragment.HomeAlters>) {
        val diffCallback = AdDiffCallback(ads, newAds)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        ads.clear()
        ads.addAll(newAds)
        diffResult.dispatchUpdatesTo(this)
    }

    // DiffUtil Callback per calcolare le differenze tra vecchia e nuova lista
    class AdDiffCallback(
        private val oldList: List<HomeFragment.HomeAlters>,
        private val newList: List<HomeFragment.HomeAlters>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id // Supponendo che HomeAlters abbia un campo 'id'
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }


}
