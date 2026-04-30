package com.crewsync.crewsync.Fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crewsync.crewsync.EventAdapter
import com.crewsync.crewsync.EventRepository
import com.crewsync.crewsync.PinActivity
import com.crewsync.crewsync.R
import com.crewsync.crewsync.UserPrefs
import com.crewsync.crewsync.UserStore
import com.crewsync.crewsync.db.CrewSyncDatabase

class DashboardFragment : Fragment() {

    private var suppressSpinnerCallback = false
    private lateinit var spUser: Spinner
    private lateinit var rclEvents: RecyclerView

    private val switchUserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (!this::spUser.isInitialized) return@registerForActivityResult

            val users = UserStore.getUsers(requireContext())
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val user = result.data?.getStringExtra(PinActivity.RESULT_USER) ?: return@registerForActivityResult
                UserPrefs.setCurrentUserId(requireContext(), user)
                Toast.makeText(requireContext(), "Switched to $user", Toast.LENGTH_SHORT).show()

                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(user).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false

                refreshEvents()
            } else {
                val current = UserPrefs.getCurrentUserId(requireContext())
                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        spUser = view.findViewById(R.id.spUser)
        rclEvents = view.findViewById(R.id.rclEvents)

        setupUserSpinner()

        rclEvents.layoutManager = LinearLayoutManager(requireContext())
        refreshEvents()

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::rclEvents.isInitialized) {
            refreshEvents()
        }
    }

    private fun setupUserSpinner() {
        val users = UserStore.getUsers(requireContext())
        val userAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, users)
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUser.adapter = userAdapter

        val current = UserPrefs.getCurrentUserId(requireContext())
        spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)

        spUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (suppressSpinnerCallback) return

                val chosen = users[pos]
                val cur = UserPrefs.getCurrentUserId(requireContext())
                if (chosen == cur) return

                val i = Intent(requireContext(), PinActivity::class.java)
                i.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_SWITCH)
                i.putExtra(PinActivity.EXTRA_TARGET_USER, chosen)
                switchUserLauncher.launch(i)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun refreshEvents() {
        val dao = CrewSyncDatabase.getInstance(requireContext()).taskDao()
        val events = EventRepository.getEvents(requireContext())
        rclEvents.adapter = EventAdapter(events, dao, viewLifecycleOwner)
    }
}
