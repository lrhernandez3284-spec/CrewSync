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
import com.crewsync.crewsync.Event
import com.crewsync.crewsync.EventEditActivity
import com.crewsync.crewsync.EventRepository
import com.crewsync.crewsync.R
import com.crewsync.crewsync.TaskEditActivity
import com.crewsync.crewsync.UserPrefs

class CreateFragment : Fragment() {

    private lateinit var spEvent: Spinner
    private var events: List<Event> = emptyList()

    private val createEventLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshEvents()
                Toast.makeText(requireContext(), "Event list refreshed", Toast.LENGTH_SHORT).show()
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

        btnCreateTask.setOnClickListener {
            val idx = spEvent.selectedItemPosition
            if (idx < 0 || idx >= events.size) {
                Toast.makeText(requireContext(), "Select an event first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedEvent = events[idx]
            val i = Intent(requireContext(), TaskEditActivity::class.java)
            i.putExtra("eventId", selectedEvent.id)
            i.putExtra("taskId", 0)
            startActivity(i)
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
