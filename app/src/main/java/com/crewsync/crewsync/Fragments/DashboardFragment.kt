package com.crewsync.crewsync

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crewsync.crewsync.EventRepository

class DashboardFragment : Fragment() {

    private val users = listOf("luis", "mike", "guest")
    private var suppressSpinnerCallback = false
    private lateinit var spUser: Spinner

    private val switchUserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val user = result.data?.getStringExtra(PinActivity.RESULT_USER) ?: return@registerForActivityResult
                UserPrefs.setCurrentUserId(requireContext(), user)
                Toast.makeText(requireContext(), "Switched to $user", Toast.LENGTH_SHORT).show()

                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(user).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            } else {
                val current = UserPrefs.getCurrentUserId(requireContext())
                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        spUser = view.findViewById(R.id.spUser)

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

        val rcl = view.findViewById<RecyclerView>(R.id.rclEvents)
        rcl.layoutManager = LinearLayoutManager(requireContext())

        val events = EventRepository.getEvents()
        rcl.adapter = EventAdapter(events)

        return view
    }
}

