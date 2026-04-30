package com.crewsync.crewsync.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    // creator-only delete enforced in SQL:
    @Query("DELETE FROM tasks WHERE taskId = :taskId AND createdByUserId = :userId")
    suspend fun deleteIfCreator(taskId: Int, userId: String): Int

    @Query("""
    	SELECT * FROM tasks
    	WHERE eventId = :eventId
    	ORDER BY category ASC, dueDateMillis ASC
    """)
    fun getTasksForEvent(eventId: Int): LiveData<List<Task>>

    @Query("""
    	SELECT * FROM tasks
    	WHERE eventId = :eventId
    	  AND title LIKE '%' || :query || '%'
    	  AND (:category = 'All' OR category = :category)
    	ORDER BY category ASC, dueDateMillis ASC
    """)
    fun searchTasks(eventId: Int, query: String, category: String): LiveData<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks WHERE eventId = :eventId")
    suspend fun countTotal(eventId: Int): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE eventId = :eventId AND isDone = 1")
    suspend fun countDone(eventId: Int): Int
}

