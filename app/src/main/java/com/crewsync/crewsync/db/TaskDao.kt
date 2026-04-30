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

    // creator-only delete, BUT manager can delete any task
    @Query("""
        DELETE FROM tasks
        WHERE taskId = :taskId
          AND (createdByUserId = :userId OR :userId = 'manager')
    """)
    suspend fun deleteIfCreator(taskId: Int, userId: String): Int

    // Original: tasks for event (no filtering)
    @Query("""
        SELECT * FROM tasks
        WHERE eventId = :eventId
        ORDER BY category ASC, dueDateMillis ASC
    """)
    fun getTasksForEvent(eventId: Int): LiveData<List<Task>>

    // Original: search tasks for event (no user filtering)
    @Query("""
        SELECT * FROM tasks
        WHERE eventId = :eventId
          AND title LIKE '%' || :query || '%'
          AND (:category = 'All' OR category = :category)
        ORDER BY category ASC, dueDateMillis ASC
    """)
    fun searchTasks(eventId: Int, query: String, category: String): LiveData<List<Task>>

    // User-filtered search (manager sees all)
    @Query("""
        SELECT DISTINCT t.* FROM tasks t
        LEFT JOIN task_assignments a ON a.taskId = t.taskId
        WHERE t.eventId = :eventId
          AND t.title LIKE '%' || :query || '%'
          AND (:category = 'All' OR t.category = :category)
          AND (
                :userId = 'manager'
                OR t.createdByUserId = :userId
                OR a.userId = :userId
          )
        ORDER BY t.category ASC, t.dueDateMillis ASC
    """)
    fun searchTasksForUser(eventId: Int, userId: String, query: String, category: String): LiveData<List<Task>>

    // IMPORTANT: Non-suspend counts (so you can call them from Dispatchers.IO)
    @Query("SELECT COUNT(*) FROM tasks WHERE eventId = :eventId")
    fun countTotalNow(eventId: Int): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE eventId = :eventId AND isDone = 1")
    fun countDoneNow(eventId: Int): Int



    @Query("UPDATE tasks SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateTaskCategory(oldCategory: String, newCategory: String)

    // ---------------- ASSIGNMENTS (join table) ----------------

    @Insert
    suspend fun insertAssignment(row: TaskAssignment)

    @Insert
    suspend fun insertAssignments(list: List<TaskAssignment>)

    @Query("DELETE FROM task_assignments WHERE taskId = :taskId")
    suspend fun deleteAssignmentsForTask(taskId: Int)

    @Query("""
        SELECT taskId FROM task_assignments
        WHERE userId = :userId
          AND taskId NOT IN (
              SELECT taskId FROM task_assignments
              WHERE userId != :userId
          )
    """)
    suspend fun getTaskIdsOnlyAssignedToUser(userId: String): List<Int>

    @Query("DELETE FROM task_assignments WHERE userId = :userId")
    suspend fun deleteAssignmentsForUser(userId: String)

    @Query("SELECT userId FROM task_assignments WHERE taskId = :taskId")
    suspend fun getAssignedUsers(taskId: Int): List<String>

    @Query("SELECT * FROM task_assignments WHERE taskId = :taskId")
    suspend fun getAssignments(taskId: Int): List<TaskAssignment>
}
