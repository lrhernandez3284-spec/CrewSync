package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvMeta = findViewById<TextView>(R.id.tvMeta)
        val tvLocation = findViewById<TextView>(R.id.tvLocation)
        val tvNotes = findViewById<TextView>(R.id.tvNotes)

        val title = intent.getStringExtra("title") ?: ""
        val dateTime = intent.getStringExtra("dateTime") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val location = intent.getStringExtra("location")
        val notes = intent.getStringExtra("notesPreview")

        tvTitle.text = title
        tvMeta.text = "$category • $dateTime"
        tvLocation.text = location ?: "No location"
        tvNotes.text = notes ?: "No notes"
    }
}