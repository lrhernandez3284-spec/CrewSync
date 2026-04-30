package com.crewsync.crewsync

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
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

class EventDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var eventId: Int = -1
    private var eventDateTimeMillis: Long = -1L
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
        val assigned = data.getStringArrayListExtra("assignedUsers") ?: arrayListOf()

        if (title.isBlank()) {
            Toast.makeText(this, "Task title required", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val finalAssigned = assigned.toList()

        if (finalAssigned.isEmpty()) {
            Toast.makeText(this, "Assign this task to at least one user", Toast.LENGTH_SHORT).show()
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
                viewModel.setAssignments(newId, finalAssigned)

                NotificationStore.addForUsers(
                    context = this,
                    userIds = finalAssigned,
                    actorUserId = currentUserId,
                    message = "$currentUserId created task ${newTask.title}",
                    targetType = "task",
                    eventId = eventId,
                    taskId = newId
                )

                ReminderScheduler.scheduleTaskReminder(
                    context = this,
                    taskId = newId,
                    taskTitle = newTask.title,
                    dueDateMillis = newTask.dueDateMillis,
                    eventId = eventId,
                    assignedUserIds = finalAssigned
                )
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
            viewModel.setAssignments(editedTask.taskId, finalAssigned)
            ReminderScheduler.scheduleTaskReminder(
                context = this,
                taskId = editedTask.taskId,
                taskTitle = editedTask.title,
                dueDateMillis = editedTask.dueDateMillis,
                eventId = eventId,
                assignedUserIds = finalAssigned
            )
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
        eventDateTimeMillis = intent.getLongExtra("dateTimeMillis", -1L)

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

        // ---- ViewModel init ----
        val dao = CrewSyncDatabase.getInstance(this).taskDao()
        val factory = TaskViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        viewModel.setEventId(eventId)
        viewModel.setUserId(currentUserId)

        taskAdapter = TaskAdapter(
            currentUserId = currentUserId,
            onToggleDone = { task, checked ->
                viewModel.update(task.copy(isDone = checked))
            },
            onDelete = { task ->
                viewModel.deleteTask(task.taskId, currentUserId)
                ReminderScheduler.cancelTaskReminder(this, task.taskId)
            },
            onEdit = { task ->
                launchEditTask(task)
            }
        )
        rclTasks.adapter = taskAdapter

        // Manual tap handler (checkbox/edit/delete)
        rclTasks.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action != MotionEvent.ACTION_UP) return false

                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                val position = rv.getChildAdapterPosition(child)
                val task = taskAdapter.getTaskAt(position) ?: return false

                val childX = e.x - child.left
                val childY = e.y - child.top

                val cb = child.findViewById<CheckBox>(R.id.cbDone)
                val btnEdit = child.findViewById<Button>(R.id.btnEdit)
                val btnDelete = child.findViewById<Button>(R.id.btnDelete)

                when {
                    isTapInsideView(cb, childX, childY) -> {
                        viewModel.update(task.copy(isDone = !task.isDone))
                        return true
                    }

                    btnDelete.visibility == View.VISIBLE && isTapInsideView(btnDelete, childX, childY) -> {
                        viewModel.deleteTask(task.taskId, currentUserId)
                        ReminderScheduler.cancelTaskReminder(this@EventDetailActivity, task.taskId)
                        return true
                    }

                    isTapInsideView(btnEdit, childX, childY) -> {
                        launchEditTask(task)
                        return true
                    }
                }
                return false
            }
        })

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
            if (eventDateTimeMillis > 0L) {
                i.putExtra("dueDateMillis", eventDateTimeMillis)
            }
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentCategory = parent?.getItemAtPosition(pos)?.toString() ?: "All"
                viewModel.setCategory(currentCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun launchEditTask(task: Task) {
        viewModel.getAssignedUsers(task.taskId) { assigned ->
            runOnUiThread {
                val i = Intent(this, TaskEditActivity::class.java)
                i.putExtra("eventId", eventId)
                i.putExtra("taskId", task.taskId)
                i.putExtra("title", task.title)
                i.putExtra("category", task.category)
                i.putExtra("dueDateMillis", task.dueDateMillis)
                i.putExtra("isDone", task.isDone)
                i.putExtra("createdByUserId", task.createdByUserId)
                i.putStringArrayListExtra("assignedUsers", ArrayList(assigned))
                taskEditLauncher.launch(i)
            }
        }
    }

    private fun isTapInsideView(target: View, x: Float, y: Float): Boolean {
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
