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

    fun getTaskAt(position: Int): Task? = items.getOrNull(position)

    class TaskVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvTaskMeta)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskVH(view)
    }

    override fun onBindViewHolder(holder: TaskVH, position: Int) {
        val t = items[position]

        holder.tvTitle.text = t.title

        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        val due = sdf.format(Date(t.dueDateMillis))

        val isOverdue = !t.isDone && t.dueDateMillis < System.currentTimeMillis()
        val overdueTag = if (isOverdue) " • OVERDUE" else ""

        val creatorName = UserStore.getDisplayName(holder.itemView.context, t.createdByUserId)
        holder.tvMeta.text = "${t.category} • Created by $creatorName • Due $due$overdueTag"

        // checkbox
        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = t.isDone
        holder.cbDone.setOnCheckedChangeListener { _, checked ->
            onToggleDone(t, checked)
        }

        // creator OR manager can delete
        val canDelete = (t.createdByUserId == currentUserId) || (currentUserId == "manager")
        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener { onDelete(t) }

        // tap row to edit
        holder.itemView.setOnClickListener { onEdit(t) }
    }

    override fun getItemCount(): Int = items.size
}
