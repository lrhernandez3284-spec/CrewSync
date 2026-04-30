package com.crewsync.crewsync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventEditActivity : AppCompatActivity() {

    private var eventMillis: Long = System.currentTimeMillis()
    private var editingEventId: Int = 0
    private val displayFormat = SimpleDateFormat("EEE MMM d, yyyy h:mm a", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)

        val currentUser = UserPrefs.getCurrentUserId(this)
        if (currentUser != "manager") {
            Toast.makeText(this, "Manager only", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        editingEventId = intent.getIntExtra("eventId", 0)
        val existingEvent = if (editingEventId > 0) {
            EventRepository.getEventById(this, editingEventId)
        } else {
            null
        }

        val tvTitle = findViewById<TextView>(R.id.tvEventEditTitle)
        val etTitle = findViewById<EditText>(R.id.etEventTitle)
        val etCategory = findViewById<AutoCompleteTextView>(R.id.etEventCategory)
        val listUsers = findViewById<ListView>(R.id.listEventUsers)
        val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAllEventUsers)
        val tvDateTime = findViewById<TextView>(R.id.tvEventDateTime)
        val btnPickDateTime = findViewById<Button>(R.id.btnPickEventDateTime)
        val etLocation = findViewById<EditText>(R.id.etEventLocation)
        val etNotes = findViewById<EditText>(R.id.etEventNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveEvent)

        tvTitle.text = if (editingEventId > 0) "Edit Event" else "Create Event"
        btnSave.text = if (editingEventId > 0) "Save Event Changes" else "Save Event"

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            CategoryStore.getCategories(this)
        )
        etCategory.setAdapter(categoryAdapter)
        etCategory.threshold = 0
        etCategory.setOnClickListener { etCategory.showDropDown() }
        etCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) etCategory.showDropDown()
        }

        val users = UserStore.getUsers(this).filter { it != "manager" }
        val userLabels = users.map { UserStore.getUserLabel(this, it) }
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, userLabels)
        listUsers.adapter = userAdapter
        listUsers.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        fun updateSelectAllCheckbox() {
            cbSelectAll.setOnCheckedChangeListener(null)
            cbSelectAll.isChecked = users.isNotEmpty() && users.indices.all { listUsers.isItemChecked(it) }
            cbSelectAll.setOnCheckedChangeListener { _, checked ->
                for (i in users.indices) {
                    listUsers.setItemChecked(i, checked)
                }
            }
        }

        cbSelectAll.setOnCheckedChangeListener { _, checked ->
            for (i in users.indices) {
                listUsers.setItemChecked(i, checked)
            }
        }

        listUsers.setOnItemClickListener { _, _, _, _ ->
            updateSelectAllCheckbox()
        }

        if (existingEvent != null) {
            etTitle.setText(existingEvent.title)
            etCategory.setText(existingEvent.category, false)
            etLocation.setText(existingEvent.location ?: "")
            etNotes.setText(existingEvent.notesPreview ?: "")
            eventMillis = existingEvent.dateTimeMillis ?: System.currentTimeMillis()

            val selectedUsers =
                if (existingEvent.assignedUserIds.isNotEmpty()) {
                    existingEvent.assignedUserIds
                } else {
                    users
                }

            for (i in users.indices) {
                if (selectedUsers.contains(users[i])) {
                    listUsers.setItemChecked(i, true)
                }
            }
            updateSelectAllCheckbox()
        }

        tvDateTime.text = "Date/Time: ${displayFormat.format(eventMillis)}"

        btnPickDateTime.setOnClickListener {
            pickDateTime { picked ->
                eventMillis = picked
                tvDateTime.text = "Date/Time: ${displayFormat.format(eventMillis)}"
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Event title required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = etCategory.text.toString().trim().ifBlank { "General" }

            val assigned = mutableListOf<String>()
            for (i in users.indices) {
                if (listUsers.isItemChecked(i)) assigned.add(users[i])
            }

            if (assigned.isEmpty()) {
                Toast.makeText(this, "Assign this event to at least one user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CategoryStore.addCategory(this, category)

            val location = etLocation.text.toString().trim().ifBlank { null }
            val notes = etNotes.text.toString().trim().ifBlank { null }

            val success =
                if (editingEventId > 0) {
                    EventStore.updateEvent(
                        context = this,
                        eventId = editingEventId,
                        title = title,
                        category = category,
                        dateTimeMillis = eventMillis,
                        location = location,
                        notesPreview = notes,
                        assignedUserIds = assigned
                    )
                } else {
                    EventStore.addEvent(
                        context = this,
                        title = title,
                        category = category,
                        dateTimeMillis = eventMillis,
                        location = location,
                        notesPreview = notes,
                        assignedUserIds = assigned
                    )
                    true
                }

            if (!success) {
                Toast.makeText(this, "Could not save event", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                if (editingEventId > 0) "Event updated" else "Event created",
                Toast.LENGTH_SHORT
            ).show()

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun pickDateTime(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = eventMillis

        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
