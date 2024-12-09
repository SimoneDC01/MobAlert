package com.example.mobalert

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobalert.databinding.RowImageSliderBinding
import com.google.android.material.imageview.ShapeableImageView

class AdapterImageSlider : RecyclerView.Adapter<AdapterImageSlider.HolderImageSlider> {
    private lateinit var binding: RowImageSliderBinding

    private var context: Context
    private var imageArrayList = ArrayList<ModelImageSlider>()

    constructor(context: Context, imageCopyOnWriteArraySet: ArrayList<ModelImageSlider>) {
        this.context = context
        this.imageArrayList = imageCopyOnWriteArraySet
    }
    inner class HolderImageSlider(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageIv: ShapeableImageView = binding.imageIv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImageSlider {
        binding = RowImageSliderBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderImageSlider(binding.root)
    }

    override fun getItemCount(): Int {
        return imageArrayList.size
    }

    override fun onBindViewHolder(holder: HolderImageSlider, position: Int) {
        val modelImageSlider = imageArrayList[position]

        val imageUrl = modelImageSlider.imageUrl

        try {
            Glide.with(context)
                .load(imageUrl)
                .into(holder.imageIv)
        } catch (e: Exception) {
            Log.e("LOGIN", "onBindViewHolder: ", e)

        }

        holder.imageIv.setOnClickListener {
            Log.d("LOGIN", "onBindViewHolder: Clicked on image $position")
        }
    }
}