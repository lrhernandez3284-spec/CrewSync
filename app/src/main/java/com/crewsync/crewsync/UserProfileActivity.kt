package com.crewsync.crewsync

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.crewsync.crewsync.data.PinPrefs
import com.crewsync.crewsync.data.UserPrefs
import com.crewsync.crewsync.data.UserStore

class UserProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userId = UserPrefs.getCurrentUserId(this)

        val tvUserId = findViewById<TextView>(R.id.tvUserId)
        val etDisplayName = findViewById<EditText>(R.id.etDisplayName)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)

        val etOldPin = findViewById<EditText>(R.id.etOldPin)
        val etNewPin = findViewById<EditText>(R.id.etNewPin)
        val etConfirmPin = findViewById<EditText>(R.id.etConfirmPin)
        val btnChangePin = findViewById<Button>(R.id.btnChangePin)

        tvUserId.text = "User ID: $userId"
        etDisplayName.setText(UserStore.getDisplayName(this, userId))

        btnSaveProfile.setOnClickListener {
            UserStore.setDisplayName(this, userId, etDisplayName.text.toString())
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
        }

        btnChangePin.setOnClickListener {
            val oldPin = etOldPin.text.toString().trim()
            val newPin = etNewPin.text.toString().trim()
            val confirmPin = etConfirmPin.text.toString().trim()

            if (!PinPrefs.verifyPin(this, userId, oldPin)) {
                Toast.makeText(this, "Old PIN is incorrect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPin.length < 4) {
                Toast.makeText(this, "New PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPin != confirmPin) {
                Toast.makeText(this, "New PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PinPrefs.savePin(this, userId, newPin)

            etOldPin.text.clear()
            etNewPin.text.clear()
            etConfirmPin.text.clear()

            Toast.makeText(this, "PIN changed", Toast.LENGTH_SHORT).show()
        }
    }
}
