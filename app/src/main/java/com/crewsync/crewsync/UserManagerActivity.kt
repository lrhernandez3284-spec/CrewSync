package com.crewsync.crewsync

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crewsync.crewsync.db.CrewSyncDatabase
import com.crewsync.crewsync.db.TaskAssignment
import kotlinx.coroutines.launch

class UserManagerActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private var users: MutableList<String> = mutableListOf()
    private var labels: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_manager)

        val current = UserPrefs.getCurrentUserId(this)
        if (current != "manager") {
            Toast.makeText(this, "Manager only", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etNewUser = findViewById<EditText>(R.id.etNewUser)
        val etNewDisplayName = findViewById<EditText>(R.id.etNewDisplayName)
        val btnAdd = findViewById<Button>(R.id.btnAddUser)
        val list = findViewById<ListView>(R.id.listUsers)

        fun refresh() {
            users = UserStore.getUsers(this).toMutableList()
            labels = users.map { UserStore.getUserLabel(this, it) }.toMutableList()

            adapter.clear()
            adapter.addAll(labels)
            adapter.notifyDataSetChanged()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        list.adapter = adapter
        refresh()

        btnAdd.setOnClickListener {
            val newId = etNewUser.text.toString()
            val displayName = etNewDisplayName.text.toString()

            val ok = UserStore.addUser(this, newId, displayName)
            if (!ok) {
                Toast.makeText(this, "Invalid or existing user id", Toast.LENGTH_SHORT).show()
            } else {
                val addedUserId = newId.trim().lowercase()
                val actor = UserPrefs.getCurrentUserId(this)
                val addedName = UserStore.getDisplayName(this, addedUserId)

                NotificationStore.addForUsers(
                    context = this,
                    userIds = UserStore.getUsers(this),
                    actorUserId = actor,
                    message = "$actor added new user $addedName",
                    targetType = "user",
                    eventId = 0,
                    taskId = 0
                )

                Toast.makeText(this, "User added", Toast.LENGTH_SHORT).show()
                etNewUser.text.clear()
                etNewDisplayName.text.clear()
                refresh()
            }
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            val userId = users[pos]

            if (userId == "manager") {
                val options = arrayOf("Edit Display Name", "Set New PIN")
                AlertDialog.Builder(this)
                    .setTitle("Manage: ${UserStore.getUserLabel(this, userId)}")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> showEditDisplayNameDialog(userId, ::refresh)
                            1 -> showSetPinDialog(userId)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnItemClickListener
            }

            val options = arrayOf("Edit Display Name", "Reset PIN", "Set New PIN", "Delete User")
            AlertDialog.Builder(this)
                .setTitle("Manage: ${UserStore.getUserLabel(this, userId)}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showEditDisplayNameDialog(userId, ::refresh)
                        1 -> {
                            PinPrefs.resetPin(this, userId)
                            Toast.makeText(this, "PIN reset for ${UserStore.getDisplayName(this, userId)}", Toast.LENGTH_SHORT).show()
                        }
                        2 -> showSetPinDialog(userId)
                        3 -> confirmDeleteUser(userId, ::refresh)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditDisplayNameDialog(userId: String, refresh: () -> Unit) {
        val input = EditText(this)
        input.hint = "Display name"
        input.setText(UserStore.getDisplayName(this, userId))

        AlertDialog.Builder(this)
            .setTitle("Edit Display Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                UserStore.setDisplayName(this, userId, input.text.toString())
                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetPinDialog(userId: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val pin1 = EditText(this).apply {
            hint = "New PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val pin2 = EditText(this).apply {
            hint = "Re-enter new PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        layout.addView(pin1)
        layout.addView(pin2)

        AlertDialog.Builder(this)
            .setTitle("Set PIN for ${UserStore.getDisplayName(this, userId)}")
            .setView(layout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val a = pin1.text.toString().trim()
                        val b = pin2.text.toString().trim()

                        if (a.length < 4) {
                            Toast.makeText(this@UserManagerActivity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        if (a != b) {
                            Toast.makeText(this@UserManagerActivity, "PINs do not match", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        PinPrefs.savePin(this@UserManagerActivity, userId, a)
                        Toast.makeText(this@UserManagerActivity, "PIN updated for ${UserStore.getDisplayName(this@UserManagerActivity, userId)}", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun confirmDeleteUser(userId: String, refresh: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${UserStore.getDisplayName(this, userId)}?")
            .setMessage("If this user is the only assignee on a task, that task will be reassigned to manager.")
            .setPositiveButton("Delete") { _, _ ->
                safeDeleteUser(userId, refresh)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun safeDeleteUser(userId: String, refresh: () -> Unit) {
        val dao = CrewSyncDatabase.getInstance(this).taskDao()

        lifecycleScope.launch {
            val onlyAssignedTaskIds = dao.getTaskIdsOnlyAssignedToUser(userId)

            onlyAssignedTaskIds.forEach { taskId ->
                dao.insertAssignment(TaskAssignment(taskId = taskId, userId = "manager"))
            }

            dao.deleteAssignmentsForUser(userId)

            val displayName = UserStore.getDisplayName(this@UserManagerActivity, userId)

            UserStore.removeUser(this@UserManagerActivity, userId)
            PinPrefs.resetPin(this@UserManagerActivity, userId)

            Toast.makeText(
                this@UserManagerActivity,
                "Deleted $displayName",
                Toast.LENGTH_SHORT
            ).show()

            refresh()
        }
    }
}
