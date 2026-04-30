package com.crewsync.crewsync.Fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.crewsync.crewsync.Event
import com.crewsync.crewsync.EventEditActivity
import com.crewsync.crewsync.EventRepository
import com.crewsync.crewsync.NotificationStore
import com.crewsync.crewsync.R
import com.crewsync.crewsync.ReminderScheduler
import com.crewsync.crewsync.TaskEditActivity
import com.crewsync.crewsync.UserPrefs
import com.crewsync.crewsync.db.CrewSyncDatabase
import com.crewsync.crewsync.db.Task
import com.crewsync.crewsync.db.TaskAssignment
import kotlinx.coroutines.launch

class CreateFragment : Fragment() {

    private lateinit var spEvent: Spinner
    private var events: List<Event> = emptyList()
    private var pendingEventId: Int = -1

    private val createEventLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshEvents()
                Toast.makeText(requireContext(), "Event list refreshed", Toast.LENGTH_SHORT).show()
            }
        }

    private val createTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult
            val eventId = data.getIntExtra("eventId", pendingEventId)

            if (eventId <= 0) {
                Toast.makeText(requireContext(), "Select an event first", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val title = data.getStringExtra("title") ?: ""
            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Task title required", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val assigned = data.getStringArrayListExtra("assignedUsers") ?: arrayListOf()
            if (assigned.isEmpty()) {
                Toast.makeText(requireContext(), "Assign this task to at least one user", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val currentUser = UserPrefs.getCurrentUserId(requireContext())
            val category = data.getStringExtra("category") ?: "General"
            val dueMillis = data.getLongExtra("dueDateMillis", System.currentTimeMillis())

            val task = Task(
                taskId = 0,
                eventId = eventId,
                title = title,
                category = category,
                dueDateMillis = dueMillis,
                isDone = false,
                createdByUserId = currentUser
            )

            val dao = CrewSyncDatabase.getInstance(requireContext()).taskDao()

            viewLifecycleOwner.lifecycleScope.launch {
                val newId = dao.insert(task).toInt()

                dao.deleteAssignmentsForTask(newId)
                dao.insertAssignments(
                    assigned.distinct().map {
                        TaskAssignment(taskId = newId, userId = it)
                    }
                )

                NotificationStore.addForUsers(
                    context = requireContext(),
                    userIds = assigned,
                    actorUserId = currentUser,
                    message = "$currentUser created task ${task.title}",
                    targetType = "task",
                    eventId = eventId,
                    taskId = newId
                )

                ReminderScheduler.schedule(requireContext(), newId, task.title, task.dueDateMillis)

                Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create, container, false)

        spEvent = view.findViewById(R.id.spEvent)
        val btnCreateTask = view.findViewById<Button>(R.id.btnCreateTask)
        val btnCreateEvent = view.findViewById<Button>(R.id.btnCreateEvent)

        refreshEvents()

        val currentUser = UserPrefs.getCurrentUserId(requireContext())
        if (currentUser != "manager") {
            btnCreateEvent.visibility = View.GONE
        } else {
            btnCreateEvent.visibility = View.VISIBLE
        }

        btnCreateTask.setOnClickListener {
            val idx = spEvent.selectedItemPosition
            if (idx < 0 || idx >= events.size) {
                Toast.makeText(requireContext(), "Select an event first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedEvent = events[idx]
            pendingEventId = selectedEvent.id

            val i = Intent(requireContext(), TaskEditActivity::class.java)
            i.putExtra("eventId", selectedEvent.id)
            i.putExtra("taskId", 0)
            if (selectedEvent.dateTimeMillis != null) {
                i.putExtra("dueDateMillis", selectedEvent.dateTimeMillis)
            }
            createTaskLauncher.launch(i)
        }

        btnCreateEvent.setOnClickListener {
            val currentUser = UserPrefs.getCurrentUserId(requireContext())
            if (currentUser != "manager") {
                Toast.makeText(requireContext(), "Manager only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(requireContext(), EventEditActivity::class.java)
            createEventLauncher.launch(i)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::spEvent.isInitialized) {
            refreshEvents()
        }
    }

    private fun refreshEvents() {
        events = EventRepository.getEvents(requireContext())

        val titles = events.map {
            "${it.title} (${it.category}) - ${it.dateTime}"
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, titles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEvent.adapter = adapter
    }
}
