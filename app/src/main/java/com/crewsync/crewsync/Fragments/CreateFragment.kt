package com.crewsync.crewsync.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.crewsync.crewsync.EventRepository
import com.crewsync.crewsync.R
import com.crewsync.crewsync.TaskEditActivity

class CreateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create, container, false)

        val spEvent = view.findViewById<Spinner>(R.id.spEvent)
        val btnCreateTask = view.findViewById<Button>(R.id.btnCreateTask)
        val btnCreateEvent = view.findViewById<Button>(R.id.btnCreateEvent)

        val events = EventRepository.getEvents()
        val titles = events.map { "${it.title} (${it.category})" }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, titles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEvent.adapter = adapter

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
            Toast.makeText(requireContext(), "Create Event coming soon", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
