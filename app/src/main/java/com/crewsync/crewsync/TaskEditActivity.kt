package com.crewsync.crewsync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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

        val eventId = intent.getIntExtra("eventId", -1)
        val taskId = intent.getIntExtra("taskId", 0)

        // prefill if editing
        etTitle.setText(intent.getStringExtra("title") ?: "")
        etCategory.setText(intent.getStringExtra("category") ?: "")
        dueMillis = intent.getLongExtra("dueDateMillis", System.currentTimeMillis())
        tvDue.text = "Due: ${formatDue(dueMillis)}"

        btnPickDue.setOnClickListener {
            pickDateTime { picked ->
                dueMillis = picked
                tvDue.text = "Due: ${formatDue(dueMillis)}"
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val category = etCategory.text.toString().trim().ifBlank { "General" }

            // Send result back to EventDetailActivity
            val result = android.content.Intent()
	    result.putExtra("eventId", eventId)
	    result.putExtra("taskId", taskId)
	    result.putExtra("title", title)
	    result.putExtra("category", category)
	    result.putExtra("dueDateMillis", dueMillis)

            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun pickDateTime(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(this,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
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

