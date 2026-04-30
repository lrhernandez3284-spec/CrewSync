package com.crewsync.crewsync

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private val lifecycleOwner: LifecycleOwner,
    private val showRemoveButton: Boolean = false,
    private val onRemoveEvent: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vCategoryBar: View? = itemView.findViewById(R.id.vCategoryBar)
        val vCategoryDot: View? = itemView.findViewById(R.id.vCategoryDot)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvEventMeta)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.tvEventStatus)
        val tvProgress: TextView = itemView.findViewById(R.id.tvEventProgress)
        val btnHideEvent: Button = itemView.findViewById(R.id.btnHideEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val e = events[position]

        holder.tvTitle.text = e.title

        val timingStatus = eventTimingStatus(e)
        holder.tvMeta.text = "${e.category} • ${e.dateTime} • $timingStatus"
        holder.tvLocation.text = e.location ?: "No location"

        val categoryColor = CategoryColorStore.getColor(holder.itemView.context, e.category)
        holder.vCategoryBar?.setBackgroundColor(categoryColor)
        holder.vCategoryDot?.setBackgroundColor(categoryColor)

        holder.tvStatus.text = "Event: $timingStatus"
        holder.tvProgress.text = "Tasks: —"

        holder.btnHideEvent.visibility = if (showRemoveButton) View.VISIBLE else View.GONE
        holder.btnHideEvent.setOnClickListener {
            onRemoveEvent?.invoke(e)
        }

        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val total = dao.countTotalNow(e.id)
            val done = dao.countDoneNow(e.id)
            val percent = if (total == 0) 0 else (done * 100 / total)

            val taskStatus = when {
                total == 0 -> "No tasks"
                done == total -> "Complete"
                done > 0 -> "In Progress"
                else -> "Not Started"
            }

            withContext(Dispatchers.Main) {
                holder.tvStatus.text = "Event: $timingStatus"
                holder.tvProgress.text = "Tasks: $taskStatus • $percent% ($done/$total)"
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
            intent.putExtra("dateTimeMillis", e.dateTimeMillis ?: -1L)
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun eventTimingStatus(event: Event): String {
        val millis = event.dateTimeMillis ?: return "No date"
        return if (millis < System.currentTimeMillis()) "Past" else "Upcoming"
    }

    override fun getItemCount(): Int = events.size
}
