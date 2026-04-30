package com.crewsync.crewsync.Fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crewsync.crewsync.DashboardVisibilityStore
import com.crewsync.crewsync.Event
import com.crewsync.crewsync.EventAdapter
import com.crewsync.crewsync.EventRepository
import com.crewsync.crewsync.PinActivity
import com.crewsync.crewsync.R
import com.crewsync.crewsync.UserPrefs
import com.crewsync.crewsync.UserStore
import com.crewsync.crewsync.db.CrewSyncDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private var suppressSpinnerCallback = false
    private var suppressFilterCallback = false

    private lateinit var spUser: Spinner
    private lateinit var spFilter: Spinner
    private lateinit var rcl: RecyclerView

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

                setupFilterSpinner()
                refreshEvents()
            } else {
                val current = UserPrefs.getCurrentUserId(requireContext())
                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        spUser = view.findViewById(R.id.spUser)
        spFilter = view.findViewById(R.id.spDashboardFilter)
        rcl = view.findViewById(R.id.rclEvents)

        rcl.layoutManager = LinearLayoutManager(requireContext())

        setupUserSpinner()
        setupFilterSpinner()
        refreshEvents()

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::rcl.isInitialized) {
            setupFilterSpinner()
            refreshEvents()
        }
    }

    private fun setupUserSpinner() {
        val users = UserStore.getUsers(requireContext())
        val userLabels = users.map { UserStore.getUserLabel(requireContext(), it) }

        val userAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, userLabels)
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

    private fun setupFilterSpinner() {
        val current = UserPrefs.getCurrentUserId(requireContext())
        val filters = DashboardVisibilityStore.filters

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFilter.adapter = adapter

        val saved = DashboardVisibilityStore.getFilter(requireContext(), current)
        val idx = filters.indexOf(saved).takeIf { it >= 0 } ?: 0

        suppressFilterCallback = true
        spFilter.setSelection(idx)
        suppressFilterCallback = false

        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (suppressFilterCallback) return

                val selected = filters[pos]
                DashboardVisibilityStore.setFilter(requireContext(), current, selected)
                refreshEvents()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun refreshEvents() {
        val context = requireContext()
        val currentUser = UserPrefs.getCurrentUserId(context)
        val filter = DashboardVisibilityStore.getFilter(context, currentUser)
        val dao = CrewSyncDatabase.getInstance(context).taskDao()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allEvents = EventRepository.getEvents(context)

            val filtered = allEvents.filter { event ->
                if (DashboardVisibilityStore.isHidden(context, currentUser, event.id)) {
                    return@filter false
                }

                val total = dao.countTotalNow(event.id)
                val done = dao.countDoneNow(event.id)
                val complete = total > 0 && done == total
                val past = isPast(event)

                when (filter) {
                    DashboardVisibilityStore.FILTER_ALL -> true
                    DashboardVisibilityStore.FILTER_PAST -> past
                    DashboardVisibilityStore.FILTER_COMPLETED -> complete
                    DashboardVisibilityStore.FILTER_UPCOMING -> !past
                    else -> !past
                }
            }

            withContext(Dispatchers.Main) {
                rcl.adapter = EventAdapter(
                    events = filtered,
                    dao = dao,
                    lifecycleOwner = viewLifecycleOwner,
                    showRemoveButton = currentUser == "manager",
                    onRemoveEvent = { event -> showRemoveOptions(event) }
                )
            }
        }
    }

    private fun showRemoveOptions(event: Event) {
        val currentUser = UserPrefs.getCurrentUserId(requireContext())
        if (currentUser != "manager") return

        val options = arrayOf(
            "Remove from my dashboard",
            "Remove from assigned users' dashboards",
            "Remove from everyone including manager"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Remove ${event.title}?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        DashboardVisibilityStore.hideEventForUser(requireContext(), "manager", event.id)
                        Toast.makeText(requireContext(), "Removed from manager dashboard", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val users = usersForEvent(event).filter { it != "manager" }
                        DashboardVisibilityStore.hideEventForUsers(requireContext(), users, event.id)
                        Toast.makeText(requireContext(), "Removed from assigned users' dashboards", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val users = (usersForEvent(event) + "manager").distinct()
                        DashboardVisibilityStore.hideEventForUsers(requireContext(), users, event.id)
                        Toast.makeText(requireContext(), "Removed from dashboards", Toast.LENGTH_SHORT).show()
                    }
                }
                refreshEvents()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun usersForEvent(event: Event): List<String> {
        return if (event.assignedUserIds.isNotEmpty()) {
            event.assignedUserIds
        } else {
            UserStore.getUsers(requireContext()).filter { it != "manager" }
        }
    }

    private fun isPast(event: Event): Boolean {
        val millis = event.dateTimeMillis ?: return false
        return millis < System.currentTimeMillis()
    }
}
