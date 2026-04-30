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

        val etTitle = findViewById<EditText>(R.id.etEventTitle)
        val spCategory = findViewById<Spinner>(R.id.spEventCategory)
        val etCustomCategory = findViewById<EditText>(R.id.etCustomCategory)
        val listUsers = findViewById<ListView>(R.id.listEventUsers)
        val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAllEventUsers)
        val tvDateTime = findViewById<TextView>(R.id.tvEventDateTime)
        val btnPickDateTime = findViewById<Button>(R.id.btnPickEventDateTime)
        val etLocation = findViewById<EditText>(R.id.etEventLocation)
        val etNotes = findViewById<EditText>(R.id.etEventNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveEvent)

        val categories = mutableListOf<String>()
        categories.addAll(EventRepository.getCategories(this))
        categories.add("Custom")

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        val users = UserStore.getUsers(this).filter { it != "manager" }
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, users)
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

            val selectedCategory = spCategory.selectedItem?.toString() ?: "General"
            val customCategory = etCustomCategory.text.toString().trim()

            val category =
                if (selectedCategory == "Custom") customCategory.ifBlank { "General" }
                else selectedCategory

            val assigned = mutableListOf<String>()
            for (i in users.indices) {
                if (listUsers.isItemChecked(i)) assigned.add(users[i])
            }

            if (assigned.isEmpty()) {
                Toast.makeText(this, "Assign this event to at least one user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val location = etLocation.text.toString().trim().ifBlank { null }
            val notes = etNotes.text.toString().trim().ifBlank { null }

            EventStore.addEvent(
                context = this,
                title = title,
                category = category,
                dateTimeMillis = eventMillis,
                location = location,
                notesPreview = notes,
                assignedUserIds = assigned
            )

            Toast.makeText(this, "Event created", Toast.LENGTH_SHORT).show()
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
