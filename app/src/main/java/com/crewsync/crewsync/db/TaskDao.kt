package com.crewsync.crewsync.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {

    // ---------------- TASKS ----------------

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    // creator-only delete, BUT manager can delete all:
    @Query("""
        DELETE FROM tasks
        WHERE taskId = :taskId
          AND (createdByUserId = :userId OR :userId = 'manager')
    """)
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


    // ---------------- ASSIGNMENTS (join table) ----------------

    @Insert
    suspend fun insertAssignments(list: List<TaskAssignment>)

    @Query("DELETE FROM task_assignments WHERE taskId = :taskId")
    suspend fun deleteAssignmentsForTask(taskId: Int)

    @Query("DELETE FROM task_assignments WHERE userId = :userId")
    suspend fun deleteAssignmentsForUser(userId: String)

    @Query("SELECT userId FROM task_assignments WHERE taskId = :taskId")
    suspend fun getAssignedUsers(taskId: Int): List<String>

    @Query("""
        SELECT DISTINCT t.* FROM tasks t
        INNER JOIN task_assignments a ON a.taskId = t.taskId
        WHERE t.eventId = :eventId
          AND a.userId = :userId
          AND t.title LIKE '%' || :query || '%'
          AND (:category = 'All' OR t.category = :category)
        ORDER BY t.category ASC, t.dueDateMillis ASC
    """)
    fun searchTasksForUser(eventId: Int, userId: String, query: String, category: String): LiveData<List<Task>>
}
