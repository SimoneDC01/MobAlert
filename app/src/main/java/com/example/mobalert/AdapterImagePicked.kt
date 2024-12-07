package com.example.mobalert

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.mobalert.databinding.RowImagesBinding

class AdapterImagePicked(
private val context: Context,
private val imagesPickedArrayList: ArrayList<ModelImagePicked>
) : Adapter<AdapterImagePicked. HolderImagePicked>() {

    private lateinit var binding: RowImagesBinding

    inner class HolderImagePicked(itemView: View) : ViewHolder(itemView) {

        var imageTy = binding.imageIv
        var closeBtn = binding.closeBtn
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagePicked {
        binding = RowImagesBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderImagePicked(binding.root)
    }

    override fun getItemCount(): Int {
        return imagesPickedArrayList.size
    }

    override fun onBindViewHolder(holder: HolderImagePicked, position: Int) {
        val model = imagesPickedArrayList[position]
        val imageUri = model.imageUri
        imagesPickedArrayList[position].id = position

        try {
            Glide.with(context)
                .load(imageUri)
                .into(holder.imageTy)
        } catch (e: Exception) {
            Log.d("LOGIN", "onBindViewHolder: ", e)
        }

        holder.closeBtn.setOnClickListener {

            for (i in 0 until imagesPickedArrayList.size) {
                if (imagesPickedArrayList[i].id == position) {

                    imagesPickedArrayList.removeAt(i)
                    notifyItemRemoved(i)
                    break
                }
            }
        }
    }
}