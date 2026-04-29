package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val users = listOf("luis", "mike", "guest")
    private var suppressSpinnerCallback = false
    private lateinit var spUser: Spinner

    private val switchUserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val user = result.data?.getStringExtra(PinActivity.RESULT_USER) ?: return@registerForActivityResult
                UserPrefs.setCurrentUserId(this, user)
                Toast.makeText(this, "Switched to $user", Toast.LENGTH_SHORT).show()

                // keep spinner synced
                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(user).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            } else {
                // revert spinner if user cancelled/failed
                val current = UserPrefs.getCurrentUserId(this)
                suppressSpinnerCallback = true
                spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)
                suppressSpinnerCallback = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        spUser = findViewById(R.id.spUser)

        // spinner setup
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUser.adapter = userAdapter

        val current = UserPrefs.getCurrentUserId(this)
        spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)

        spUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (suppressSpinnerCallback) return

                val chosen = users[pos]
                val currentUser = UserPrefs.getCurrentUserId(this@MainActivity)
                if (chosen == currentUser) return

                // Require PIN for the chosen user
                val i = Intent(this@MainActivity, PinActivity::class.java)
                i.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_SWITCH)
                i.putExtra(PinActivity.EXTRA_TARGET_USER, chosen)
                switchUserLauncher.launch(i)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // recycler view
        val rcl = findViewById<RecyclerView>(R.id.rclEvents)
        rcl.layoutManager = LinearLayoutManager(this)

        val events = listOf(
            Event(1, "Rehearsal - Setlist Run", "Mon 7:00 PM", "Rehearsal", "Garage Studio", "Practice intros + transitions"),
            Event(2, "Gig @ Calakas Raza", "Sat 9:00 PM", "Performance", "Downtown", "Arrive early, soundcheck 8:15"),
            Event(3, "Team Meeting", "Wed 5:30 PM", "Meeting", "Coffee shop", null),
            Event(4, "Personal: Replace Strings", "Thu 6:00 PM", "Personal", null, "NYXL set + tune stability check"),
            Event(5, "Photo/Promo Content", "Sun 2:00 PM", "Content", "Home", "Record 3 clips for TikTok")
        )
        rcl.adapter = EventAdapter(events)
    }
}

