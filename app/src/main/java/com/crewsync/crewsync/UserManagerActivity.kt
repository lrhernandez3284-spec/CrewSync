package com.crewsync.crewsync

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class UserManagerActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private var users: MutableList<String> = mutableListOf()

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
        val btnAdd = findViewById<Button>(R.id.btnAddUser)
        val list = findViewById<ListView>(R.id.listUsers)

        fun refresh() {
            users = UserStore.getUsers(this).toMutableList()
            adapter.clear()
            adapter.addAll(users)
            adapter.notifyDataSetChanged()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        list.adapter = adapter
        refresh()

        btnAdd.setOnClickListener {
            val newId = etNewUser.text.toString()
            val ok = UserStore.addUser(this, newId)
            if (!ok) {
                Toast.makeText(this, "Invalid or existing user id", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User added", Toast.LENGTH_SHORT).show()
                etNewUser.text.clear()
                refresh()
            }
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            val userId = users[pos]
            if (userId == "manager") {
                Toast.makeText(this, "Cannot modify manager", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            val options = arrayOf("Reset PIN", "Delete User")
            AlertDialog.Builder(this)
                .setTitle("Manage: $userId")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            PinPrefs.resetPin(this, userId)
                            Toast.makeText(this, "PIN reset for $userId", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            UserStore.removeUser(this, userId)
                            PinPrefs.resetPin(this, userId)
                            Toast.makeText(this, "Deleted $userId", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}

