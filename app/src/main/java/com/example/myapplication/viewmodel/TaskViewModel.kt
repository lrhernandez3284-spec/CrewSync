package com.example.myapplication.viewmodel

import androidx.lifecycle.*
import com.example.myapplication.db.Task
import com.example.myapplication.db.TaskDao
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val eventIdLive = MutableLiveData<Int>()
    private val queryLive = MutableLiveData<String>("")
    private val categoryLive = MutableLiveData<String>("All")

    private var currentSource: LiveData<List<Task>>? = null

    val tasks: LiveData<List<Task>> = MediatorLiveData<List<Task>>().apply {
        fun update() {
            val eventId = eventIdLive.value ?: return
            val q = queryLive.value ?: ""
            val cat = categoryLive.value ?: "All"

            currentSource?.let { (this as MediatorLiveData).removeSource(it) }

            val source = dao.searchTasks(eventId, q, cat)
            currentSource = source

            (this as MediatorLiveData).addSource(source) { value = it }
        }

        addSource(eventIdLive) { update() }
        addSource(queryLive) { update() }
        addSource(categoryLive) { update() }
    }

    fun setEventId(eventId: Int) { eventIdLive.value = eventId }
    fun setQuery(q: String) { queryLive.value = q }
    fun setCategory(cat: String) { categoryLive.value = cat }

    fun insert(task: Task) = viewModelScope.launch { dao.insert(task) }
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

    fun insertWithCallback(task: Task, onInserted: (newId: Int) -> Unit) {
	viewModelScope.launch {
            val id = dao.insert(task).toInt()
            onInserted(id)
    }
}
}

