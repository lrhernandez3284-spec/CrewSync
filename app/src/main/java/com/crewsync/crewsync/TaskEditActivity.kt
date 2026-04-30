package com.crewsync.crewsync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskEditActivity : AppCompatActivity() {

    private var dueMillis: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_edit)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val tvDue = findViewById<TextView>(R.id.tvDue)
        val btnPickDue = findViewById<Button>(R.id.btnPickDue)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val listUsers = findViewById<ListView>(R.id.listUsers)
        val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAllUsers)

        val eventId = intent.getIntExtra("eventId", -1)
        val taskId = intent.getIntExtra("taskId", 0)
        val isDone = intent.getBooleanExtra("isDone", false)
        val createdByUserId = intent.getStringExtra("createdByUserId")
            ?: UserPrefs.getCurrentUserId(this)

        val currentUser = UserPrefs.getCurrentUserId(this)
        val isNewTask = taskId == 0

        // Prefill
        etTitle.setText(intent.getStringExtra("title") ?: "")
        etCategory.setText(intent.getStringExtra("category") ?: "")
        dueMillis = intent.getLongExtra("dueDateMillis", System.currentTimeMillis())
        tvDue.text = "Due: ${formatDue(dueMillis)}"

        // Users multi-select. Manager is not a normal assignee because manager already sees all tasks.
        val users = UserStore.getUsers(this).filter { it != "manager" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, users)
        listUsers.adapter = adapter
        listUsers.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val preselected = intent.getStringArrayListExtra("assignedUsers") ?: arrayListOf()

        // Creating a new task automatically assigns the creator, if the creator is a normal user.
        val selectedSet = preselected.toMutableSet()
        if (isNewTask && currentUser != "manager") {
            selectedSet.add(currentUser)
        }

        fun updateSelectAllCheckbox() {
            cbSelectAll.setOnCheckedChangeListener(null)
            cbSelectAll.isChecked = users.isNotEmpty() && users.all { selected ->
                val idx = users.indexOf(selected)
                idx >= 0 && listUsers.isItemChecked(idx)
            }
            cbSelectAll.setOnCheckedChangeListener { _, checked ->
                for (i in users.indices) {
                    listUsers.setItemChecked(i, checked)
                }
            }
        }

        for (i in users.indices) {
            if (selectedSet.contains(users[i])) {
                listUsers.setItemChecked(i, true)
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

        updateSelectAllCheckbox()

        btnPickDue.setOnClickListener {
            pickDateTime { picked ->
                dueMillis = picked
                tvDue.text = "Due: ${formatDue(dueMillis)}"
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = etCategory.text.toString().trim().ifBlank { "General" }

            val chosen = mutableListOf<String>()
            for (i in users.indices) {
                if (listUsers.isItemChecked(i)) chosen.add(users[i])
            }

            // Safety: make sure creator is included on new tasks.
            if (isNewTask && currentUser != "manager" && !chosen.contains(currentUser)) {
                chosen.add(currentUser)
            }

            if (chosen.isEmpty()) {
                Toast.makeText(
                    this,
                    "Assign this task to at least one user",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val result = android.content.Intent()
            result.putExtra("eventId", eventId)
            result.putExtra("taskId", taskId)
            result.putExtra("title", title)
            result.putExtra("category", category)
            result.putExtra("dueDateMillis", dueMillis)
            result.putExtra("isDone", isDone)
            result.putExtra("createdByUserId", createdByUserId)
            result.putStringArrayListExtra("assignedUsers", ArrayList(chosen))

            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun pickDateTime(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
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

    private fun formatDue(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
        return sdf.format(millis)
    }
}
