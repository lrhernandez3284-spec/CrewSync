package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val rcl = findViewById<RecyclerView>(R.id.rclEvents)
        rcl.layoutManager = LinearLayoutManager(this)

        val events = listOf(
            Event("Rehearsal - Setlist Run", "Mon 7:00 PM", "Rehearsal", "Garage Studio", "Practice intros + transitions"),
            Event("Gig @ Calakas Raza", "Sat 9:00 PM", "Performance", "Downtown", "Arrive early, soundcheck 8:15"),
            Event("Team Meeting", "Wed 5:30 PM", "Meeting", "Coffee shop", null),
            Event("Personal: Replace Strings", "Thu 6:00 PM", "Personal", null, "NYXL set + tune stability check"),
            Event("Photo/Promo Content", "Sun 2:00 PM", "Content", "Home", "Record 3 clips for TikTok")
        )
        rcl.adapter = EventAdapter(events)
    }
}