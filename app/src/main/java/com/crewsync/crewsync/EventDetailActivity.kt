package com.crewsync.crewsync

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.crewsync.crewsync.db.CrewSyncDatabase
import com.crewsync.crewsync.db.Task
import com.crewsync.crewsync.viewmodel.TaskViewModel
import com.crewsync.crewsync.viewmodel.TaskViewModelFactory
import android.view.View

class EventDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var eventId: Int = -1
    private lateinit var currentUserId: String

    private lateinit var tvProgress: TextView
    private lateinit var etSearch: EditText
    private lateinit var spCategory: Spinner

    private var currentCategory: String = "All"

    private val taskEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult

        val returnedEventId = data.getIntExtra("eventId", -1)
        if (returnedEventId != eventId) return@registerForActivityResult

        val taskId = data.getIntExtra("taskId", 0)
        val title = data.getStringExtra("title") ?: ""
        val category = data.getStringExtra("category") ?: "General"
        val dueMillis = data.getLongExtra("dueDateMillis", System.currentTimeMillis())

        if (title.isBlank()) {
            Toast.makeText(this, "Task title required", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        if (taskId == 0) {
            // ADD new task
            val newTask = Task(
                taskId = 0,
                eventId = eventId,
                title = title,
                category = category,
                dueDateMillis = dueMillis,
                isDone = false,
                createdByUserId = currentUserId
            )
            viewModel.insertWithCallback(newTask) { newId ->
                viewModel.setAssignments(newId, listOf(currentUserId))
                ReminderScheduler.schedule(this, newId, newTask.title, newTask.dueDateMillis)
            }
        } else {
            // EDIT existing task
            val isDone = data.getBooleanExtra("isDone", false)
            val createdBy = data.getStringExtra("createdByUserId") ?: currentUserId

            val editedTask = Task(
                taskId = taskId,
                eventId = eventId,
                title = title,
                category = category,
                dueDateMillis = dueMillis,
                isDone = isDone,
                createdByUserId = createdBy
            )
            viewModel.update(editedTask)

            ReminderScheduler.schedule(this, editedTask.taskId, editedTask.title, editedTask.dueDateMillis)
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            Log.d("CREWSYNC_TOUCH", "EventDetailActivity ACTION_DOWN x=${ev.x} y=${ev.y}")
        }
        if (ev.action == MotionEvent.ACTION_UP) {
            Log.d("CREWSYNC_TOUCH", "EventDetailActivity ACTION_UP x=${ev.x} y=${ev.y}")
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // ---- event header ----
        eventId = intent.getIntExtra("eventId", -1)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvMeta = findViewById<TextView>(R.id.tvMeta)
        val tvLocation = findViewById<TextView>(R.id.tvLocation)
        val tvNotes = findViewById<TextView>(R.id.tvNotes)

        tvTitle.text = intent.getStringExtra("title") ?: ""
        tvMeta.text = "${intent.getStringExtra("category") ?: ""} • ${intent.getStringExtra("dateTime") ?: ""}"
        tvLocation.text = intent.getStringExtra("location") ?: "No location"
        tvNotes.text = intent.getStringExtra("notesPreview") ?: "No notes"

        // ---- tasks UI ----
        currentUserId = UserPrefs.getCurrentUserId(this)

        tvProgress = findViewById(R.id.tvProgress)
        etSearch = findViewById(R.id.etSearch)
        spCategory = findViewById(R.id.spCategory)
        val btnAddTask = findViewById<Button>(R.id.btnAddTask)

        val rclTasks = findViewById<RecyclerView>(R.id.rclTasks)
        rclTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(
            currentUserId = currentUserId,

            onToggleDone = { task, checked ->
                Toast.makeText(this, "Toggle: ${task.title} -> $checked", Toast.LENGTH_SHORT).show()
                viewModel.update(task.copy(isDone = checked))
            },

            onDelete = { task ->
                Toast.makeText(this, "Delete pressed: ${task.title}", Toast.LENGTH_SHORT).show()
                viewModel.deleteIfCreator(task.taskId, currentUserId)
                ReminderScheduler.cancel(this, task.taskId)
            },

            onEdit = { task ->
                Toast.makeText(this, "Edit pressed: ${task.title}", Toast.LENGTH_SHORT).show()
                val i = Intent(this, TaskEditActivity::class.java)
                i.putExtra("eventId", eventId)
                i.putExtra("taskId", task.taskId)
                i.putExtra("title", task.title)
                i.putExtra("category", task.category)
                i.putExtra("dueDateMillis", task.dueDateMillis)
                i.putExtra("isDone", task.isDone)
                i.putExtra("createdByUserId", task.createdByUserId)
                taskEditLauncher.launch(i)
            }
        )

        rclTasks.adapter = taskAdapter

        rclTasks.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                if (e.action != android.view.MotionEvent.ACTION_UP) return false

                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                val position = rv.getChildAdapterPosition(child)
                val task = taskAdapter.getTaskAt(position) ?: return false

                val childX = e.x - child.left
                val childY = e.y - child.top

                val cb = child.findViewById<CheckBox>(R.id.cbDone)
                val btnEdit = child.findViewById<Button>(R.id.btnEdit)
                val btnDelete = child.findViewById<Button>(R.id.btnDelete)

                when {
                    isTapInsideView(child, cb, childX, childY) -> {
                        Toast.makeText(this@EventDetailActivity, "Manual checkbox tap", Toast.LENGTH_SHORT).show()
                        viewModel.update(task.copy(isDone = !task.isDone))
                        return true
                    }

                    btnDelete.visibility == android.view.View.VISIBLE &&
                            isTapInsideView(child, btnDelete, childX, childY) -> {
                        Toast.makeText(this@EventDetailActivity, "Manual delete tap", Toast.LENGTH_SHORT).show()
                        viewModel.deleteIfCreator(task.taskId, currentUserId)
                        ReminderScheduler.cancel(this@EventDetailActivity, task.taskId)
                        return true
                    }

                    isTapInsideView(child, btnEdit, childX, childY) -> {
                        Toast.makeText(this@EventDetailActivity, "Manual edit tap", Toast.LENGTH_SHORT).show()
                        val i = Intent(this@EventDetailActivity, TaskEditActivity::class.java)
                        i.putExtra("eventId", eventId)
                        i.putExtra("taskId", task.taskId)
                        i.putExtra("title", task.title)
                        i.putExtra("category", task.category)
                        i.putExtra("dueDateMillis", task.dueDateMillis)
                        i.putExtra("isDone", task.isDone)
                        i.putExtra("createdByUserId", task.createdByUserId)
                        taskEditLauncher.launch(i)
                        return true
                    }
                }

                return false
            }
        })

        // ---- ViewModel init (Room demo style) ----
        val dao = CrewSyncDatabase.getInstance(this).taskDao()
        val factory = TaskViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        viewModel.setEventId(eventId)
        viewModel.setUserId(currentUserId)

        // observe tasks -> update list + progress + category list
        viewModel.tasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
            updateProgress(tasks)
            updateCategorySpinner(tasks)
        }

        // add task
        btnAddTask.setOnClickListener {
            val i = Intent(this, TaskEditActivity::class.java)
            i.putExtra("eventId", eventId)
            i.putExtra("taskId", 0)
            taskEditLauncher.launch(i)
        }

        // search text
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // category selection
        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                currentCategory = parent?.getItemAtPosition(pos)?.toString() ?: "All"
                viewModel.setCategory(currentCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun isTapInsideView(parent: View, target: View, x: Float, y: Float): Boolean {
        val rect = Rect()
        target.getHitRect(rect)
        return rect.contains(x.toInt(), y.toInt())
    }

    private fun updateProgress(tasks: List<Task>) {
        val total = tasks.size
        val done = tasks.count { it.isDone }
        val percent = if (total == 0) 0 else (done * 100 / total)
        tvProgress.text = "Progress: $percent% ($done/$total)"
    }

    private fun updateCategorySpinner(tasks: List<Task>) {
        val categories = mutableListOf("All")
        categories.addAll(tasks.map { it.category }.distinct().sorted())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter

        val idx = categories.indexOf(currentCategory).takeIf { it >= 0 } ?: 0
        spCategory.setSelection(idx)
    }
}
