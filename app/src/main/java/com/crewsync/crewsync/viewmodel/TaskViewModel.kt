package com.crewsync.crewsync.viewmodel

import androidx.lifecycle.*
import com.crewsync.crewsync.db.Task
import com.crewsync.crewsync.db.TaskAssignment
import com.crewsync.crewsync.db.TaskDao
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val eventIdLive = MutableLiveData<Int>()
    private val queryLive = MutableLiveData<String>("")
    private val categoryLive = MutableLiveData<String>("All")
    private val userIdLive = MutableLiveData<String>("guest")

    private var currentSource: LiveData<List<Task>>? = null

    val tasks: LiveData<List<Task>> = MediatorLiveData<List<Task>>().apply {
        fun update() {
            val eventId = eventIdLive.value ?: return
            val q = queryLive.value ?: ""
            val cat = categoryLive.value ?: "All"
            val user = userIdLive.value ?: "guest"

            currentSource?.let { (this as MediatorLiveData).removeSource(it) }

            val source =
                if (user == "manager") dao.searchTasks(eventId, q, cat)
                else dao.searchTasksForUser(eventId, user, q, cat)

            currentSource = source
            (this as MediatorLiveData).addSource(source) { value = it }
        }

        addSource(eventIdLive) { update() }
        addSource(queryLive) { update() }
        addSource(categoryLive) { update() }
        addSource(userIdLive) { update() }
    }

    fun setEventId(eventId: Int) { eventIdLive.value = eventId }
    fun setQuery(q: String) { queryLive.value = q }
    fun setCategory(cat: String) { categoryLive.value = cat }
    fun setUserId(userId: String) { userIdLive.value = userId }

    fun insertWithCallback(task: Task, onInserted: (newId: Int) -> Unit) {
        viewModelScope.launch {
            val id = dao.insert(task).toInt()
            onInserted(id)
        }
    }

    fun update(task: Task) = viewModelScope.launch { dao.update(task) }

    fun deleteIfCreator(taskId: Int, userId: String) = viewModelScope.launch {
        dao.deleteIfCreator(taskId, userId)
    }

    fun loadProgress(eventId: Int, callback: (done: Int, total: Int) -> Unit) {
        viewModelScope.launch {
            val total = dao.countTotal(eventId)
            val done = dao.countDone(eventId)
            callback(done, total)
        }
    }

    // assignments
    fun setAssignments(taskId: Int, userIds: List<String>) = viewModelScope.launch {
        dao.deleteAssignmentsForTask(taskId)
        val rows = userIds.distinct().map { TaskAssignment(taskId = taskId, userId = it) }
        if (rows.isNotEmpty()) dao.insertAssignments(rows)
    }

    fun getAssignedUsers(taskId: Int, callback: (List<String>) -> Unit) {
        viewModelScope.launch {
            callback(dao.getAssignedUsers(taskId))
        }
    }

    fun deleteAssignmentsForUser(userId: String) = viewModelScope.launch {
        dao.deleteAssignmentsForUser(userId)
    }
}
