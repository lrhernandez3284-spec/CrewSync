package com.crewsync.crewsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crewsync.crewsync.db.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val currentUserId: String,
    private val onToggleDone: (Task, Boolean) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onEdit: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskVH>() {

    private var items: List<Task> = emptyList()

    fun submitList(newList: List<Task>) {
        items = newList
        notifyDataSetChanged()
    }

    fun getTaskAt(position: Int): Task? {
        return items.getOrNull(position)
    }

    class TaskVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvTaskMeta)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskVH(view)
    }

    override fun onBindViewHolder(holder: TaskVH, position: Int) {
        val t = items[position]

        holder.tvTitle.text = t.title

        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        val due = sdf.format(Date(t.dueDateMillis))
        val isOverdue = !t.isDone && t.dueDateMillis < System.currentTimeMillis()
        val overdueTag = if (isOverdue) " • OVERDUE" else ""

        holder.tvMeta.text = "${t.category} • Due $due$overdueTag"

        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = t.isDone

        holder.btnDelete.visibility =
            if (t.createdByUserId == currentUserId) View.VISIBLE else View.GONE

        // Keep normal click listeners too, but EventDetailActivity will also handle taps manually.
        holder.cbDone.setOnClickListener {
            onToggleDone(t, holder.cbDone.isChecked)
        }

        holder.btnEdit.setOnClickListener {
            onEdit(t)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(t)
        }
    }

    override fun getItemCount(): Int = items.size
}
