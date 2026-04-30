package com.crewsync.crewsync

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_LOGIN = "login"
        const val MODE_SWITCH = "switch"

        const val EXTRA_TARGET_USER = "target_user"
        const val RESULT_USER = "result_user"
    }

    private val users = listOf("luis", "mike", "guest")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val spUser = findViewById<Spinner>(R.id.spUser)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_LOGIN
        val forcedUser = intent.getStringExtra(EXTRA_TARGET_USER)

        // Spinner setup
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUser.adapter = adapter

        if (mode == MODE_SWITCH && forcedUser != null) {
            // Switching: user is fixed
            spUser.isEnabled = false
            spUser.setSelection(users.indexOf(forcedUser).takeIf { it >= 0 } ?: 0)
        } else {
            // Normal login: default to current user
            val current = UserPrefs.getCurrentUserId(this)
            spUser.setSelection(users.indexOf(current).takeIf { it >= 0 } ?: 0)
        }

        fun selectedUser(): String {
            return if (mode == MODE_SWITCH && forcedUser != null) forcedUser
            else spUser.selectedItem.toString()
        }

        fun refreshSubtitle() {
            val userId = selectedUser()
            val isFirstTimeSetup = !PinPrefs.hasPin(this, userId)
            tvSubtitle.text = if (isFirstTimeSetup) "Create PIN for $userId" else "Enter PIN for $userId"
        }

        refreshSubtitle()

        spUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                refreshSubtitle()
                etPin.text.clear()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnContinue.setOnClickListener {
            val userId = selectedUser()
            val pin = etPin.text.toString().trim()

            if (pin.length < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isFirstTimeSetup = !PinPrefs.hasPin(this, userId)

            if (isFirstTimeSetup) {
                PinPrefs.savePin(this, userId, pin)
                Toast.makeText(this, "PIN set for $userId!", Toast.LENGTH_SHORT).show()
            } else {
                if (!PinPrefs.verifyPin(this, userId, pin)) {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    etPin.text.clear()
                    return@setOnClickListener
                }
            }

            // success:
            if (mode == MODE_SWITCH) {
                val result = Intent()
                result.putExtra(RESULT_USER, userId)
                setResult(RESULT_OK, result)
                finish()
            } else {
                UserPrefs.setCurrentUserId(this, userId)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        val userId = selectedUser()
	val isFirstTimeSetup = !PinPrefs.hasPin(this, userId)

	tvSubtitle.text =
	    if (mode == MODE_SWITCH) {
	        if (isFirstTimeSetup) "Create PIN to switch to $userId"
	        else "Authenticate to switch to $userId"
	    } else {
	        if (isFirstTimeSetup) "Create PIN for $userId"
	        else "Enter PIN for $userId"
        }
    }
}

