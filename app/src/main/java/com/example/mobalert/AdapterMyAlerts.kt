import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobalert.EditAlertFragment
import com.example.mobalert.HomeFragment
import com.example.mobalert.InsertAlertFragment
import com.example.mobalert.R

class AdAdapterMy(private val ads: List<HomeFragment.HomeAlters>,
                  private val parentFragment: Fragment) : RecyclerView.Adapter<AdAdapterMy.AdViewHolder>() {

    // ViewHolder per gli elementi della RecyclerView
    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIv: ShapeableImageView = itemView.findViewById(R.id.myimageIv)
        val titleTv: TextView = itemView.findViewById(R.id.mytitleTv)
        val descriptionTv: TextView = itemView.findViewById(R.id.mydescriptionTv)
        val dateTv: TextView = itemView.findViewById(R.id.mydateTv)
        val optionsButton: ImageButton=itemView.findViewById(R.id.myoptions_button)
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
        holder.imageIv.setImageBitmap(ad.image)
        holder.titleTv.text = ad.title
        holder.descriptionTv.text = ad.description
        holder.dateTv.text = ad.datehour
        holder.optionsButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, holder.optionsButton)
            popupMenu.menu.add(Menu.NONE, 1, 1, "Edit Alert")
            popupMenu.menu.add(Menu.NONE, 2, 2, "Delete Alert")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        Log.d("AdAdapter", "Edit Alert")

                        // Naviga al nuovo Fragment
                        val fragmentManager = parentFragment.parentFragmentManager
                        val transaction = fragmentManager.beginTransaction()
                        val fragment = EditAlertFragment() // Crea una nuova istanza del Fragment di destinazione

                        // Passa dati se necessario (ad esempio l'ID dell'alert)
                        val bundle = Bundle()
                        bundle.putString("alertTitle", ad.title)
                        bundle.putString("alertDescription", ad.description)
                        fragment.arguments = bundle

                        // Sostituisci il Fragment corrente con quello nuovo
                        transaction.replace(R.id.Fragment, fragment)
                        transaction.addToBackStack(null) // Aggiungi alla backstack per consentire il "torna indietro"
                        transaction.commit()
                    }
                    2 -> {
                        Log.d("AdAdapter", "Delete Alert")
                        // Aggiungi qui il codice per eliminare l'alert
                    }
                }
                true
            }

            // Mostra il menu popup
            popupMenu.show()
        }
    }
    override fun getItemCount(): Int = ads.size
}
