package com.crewsync.crewsync

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.crewsync.crewsync.db.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventAdapter(
    private val events: List<Event>,
    private val dao: TaskDao,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vCategoryBar: View? = itemView.findViewById(R.id.vCategoryBar)
        val vCategoryDot: View? = itemView.findViewById(R.id.vCategoryDot)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvEventMeta)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.tvEventStatus)
        val tvProgress: TextView = itemView.findViewById(R.id.tvEventProgress)
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

        val categoryColor = CategoryColorStore.getColor(holder.itemView.context, e.category)
        holder.vCategoryBar?.setBackgroundColor(categoryColor)
        holder.vCategoryDot?.setBackgroundColor(categoryColor)

        holder.tvStatus.text = "Status: loading..."
        holder.tvProgress.text = "Progress: loading..."

        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val total = dao.countTotalNow(e.id)
            val done = dao.countDoneNow(e.id)
            val percent = if (total == 0) 0 else (done * 100 / total)

            val now = System.currentTimeMillis()
            val eventMillis = e.dateTimeMillis
            val isPast = eventMillis != null && eventMillis < now

            val status = when {
                total > 0 && done == total -> "Complete"
                total > 0 && done > 0 -> "In Progress"
                isPast -> "Past Due"
                else -> "Upcoming"
            }

            val statusColor = when (status) {
                "Complete" -> Color.rgb(34, 139, 34)
                "In Progress" -> Color.rgb(204, 132, 0)
                "Past Due" -> Color.rgb(180, 40, 40)
                else -> Color.DKGRAY
            }

            withContext(Dispatchers.Main) {
                holder.tvStatus.text = "Status: $status"
                holder.tvStatus.setTextColor(statusColor)
                holder.tvProgress.text = "Progress: $percent% ($done/$total)"
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EventDetailActivity::class.java)
            intent.putExtra("eventId", e.id)
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
