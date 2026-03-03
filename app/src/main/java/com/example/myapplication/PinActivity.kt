package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        val isFirstTimeSetup = !PinPrefs.hasPin(this)
        tvSubtitle.text = if (isFirstTimeSetup) "Create a PIN" else "Enter your PIN"

        btnContinue.setOnClickListener {
            val pin = etPin.text.toString().trim()

            if (pin.length < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isFirstTimeSetup) {
                PinPrefs.savePin(this, pin)
                Toast.makeText(this, "PIN set!", Toast.LENGTH_SHORT).show()
                goToMain()
            } else {
                if (PinPrefs.verifyPin(this, pin)) {
                    goToMain()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    etPin.text.clear()
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // prevents going back to PIN after login
    }
}