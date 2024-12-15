import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.widget.ViewPager2
import com.example.mobalert.AdapterImageSlider
import com.example.mobalert.HomeFragment
import com.example.mobalert.ModelImageSlider
import com.example.mobalert.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class AdAdapter(private val context: Context, private val ads: MutableList<HomeFragment.HomeAlters>) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    // ViewHolder per gli elementi della RecyclerView
    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.root)
        val titleTv: TextView = itemView.findViewById(R.id.titleTv)
        val descriptionTv: TextView = itemView.findViewById(R.id.descriptionTv)
        val iduser: TextView= itemView.findViewById(R.id.usernameTv)
        val dateTv: TextView = itemView.findViewById(R.id.dateTv)
        val categoryTv: TextView = itemView.findViewById(R.id.categoryTv)
        val positionTv: TextView = itemView.findViewById(R.id.positionTv)
        val imageIv: ViewPager2 = itemView.findViewById(R.id.imageIv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        // Infla il layout dell'elemento
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_el, parent, false)
        return AdViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val ad = ads[position]
        //holder.imageIv.setImageBitmap(ad.image[0])
        holder.titleTv.text = ad.title
        holder.descriptionTv.text = ad.description
        holder.iduser.text=ad.username
        holder.dateTv.text = ad.datehour
        holder.categoryTv.text = ad.type

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

        holder.root.setOnClickListener {
            Log.d("LOGIN", "CLICK ${holder.imageIv.layoutParams.width}")
            if(holder.imageIv.layoutParams.width == 360) {
                holder.imageIv.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                holder.imageIv.layoutParams.height = 1000

                var params = holder.titleTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.END_OF)
                params.addRule(RelativeLayout.BELOW, R.id.imageIv)
                holder.titleTv.layoutParams = params

                params = holder.iduser.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.BELOW, R.id.imageIv)
                holder.iduser.layoutParams = params

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
            }
            else{
                holder.imageIv.layoutParams.width = 360
                holder.imageIv.layoutParams.height = 360

                var params = holder.titleTv.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.BELOW)
                params.addRule(RelativeLayout.END_OF, R.id.imageIv)
                holder.titleTv.layoutParams = params

                params = holder.iduser.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.BELOW)
                holder.iduser.layoutParams = params

                params = holder.descriptionTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.imageIv)
                holder.descriptionTv.layoutParams = params

                params = holder.categoryTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.imageIv)
                holder.categoryTv.layoutParams = params

                params = holder.dateTv.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.END_OF, R.id.imageIv)
                holder.dateTv.layoutParams = params

            }

            holder.imageIv.requestLayout()
            holder.titleTv.requestLayout()
            holder.iduser.requestLayout()
            holder.descriptionTv.requestLayout()
            holder.categoryTv.requestLayout()
            holder.dateTv.requestLayout()
        }


    }

    override fun getItemCount(): Int = ads.size

    fun filter(filters: MutableMap<String, String>){
        Log.d("LOGIN", "${filters}")
        val newList = ads.map { it.copy() }.toMutableList()
        for (ad in newList) {
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


            if (!ad.username.contains(filters["username"]!!, ignoreCase = true)) {
                ad.visible = false
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
