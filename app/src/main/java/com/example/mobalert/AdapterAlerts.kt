import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import com.example.mobalert.HomeFragment
import com.example.mobalert.R

class AdAdapter(private val ads: List<HomeFragment.HomeAlters>) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    // ViewHolder per gli elementi della RecyclerView
    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIv: ShapeableImageView = itemView.findViewById(R.id.imageIv)
        val titleTv: TextView = itemView.findViewById(R.id.titleTv)
        val descriptionTv: TextView = itemView.findViewById(R.id.descriptionTv)
        val dateTv: TextView = itemView.findViewById(R.id.dateTv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        // Infla il layout dell'elemento
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_el, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        // Popola i dati
        val ad = ads[position]
        holder.imageIv.setImageResource(R.drawable.image_white)
        holder.titleTv.text = ad.title
        holder.descriptionTv.text = ad.description
        holder.dateTv.text = ad.datehour
    }

    override fun getItemCount(): Int = ads.size
}
