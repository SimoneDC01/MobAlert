package com.example.mobalert

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestionsAdapter(
    var suggestions: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.suggestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.suggestion_el, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.textView.text = suggestion
        holder.itemView.setOnClickListener { onItemClick(suggestion) }
    }

    override fun getItemCount() = suggestions.size
}
