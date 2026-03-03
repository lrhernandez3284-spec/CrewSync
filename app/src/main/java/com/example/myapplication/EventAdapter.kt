package com.example.myapplication

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(private val events: List<Event>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvEventMeta)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val e = events[position]
        holder.tvTitle.text = e.title
        holder.tvMeta.text = "${e.category} • ${e.dateTime}"
        holder.tvLocation.text = e.location ?: "No location"

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EventDetailActivity::class.java)
            intent.putExtra("title", e.title)
            intent.putExtra("dateTime", e.dateTime)
            intent.putExtra("category", e.category)
            intent.putExtra("location", e.location)
            intent.putExtra("notesPreview", e.notesPreview)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = events.size
}