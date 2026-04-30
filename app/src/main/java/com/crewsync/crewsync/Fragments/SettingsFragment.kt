package com.crewsync.crewsync.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.crewsync.crewsync.R
import com.crewsync.crewsync.UserManagerActivity
import com.crewsync.crewsync.UserPrefs

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val tv = view.findViewById<TextView>(R.id.tvCurrentUser)
        val btn = view.findViewById<Button>(R.id.btnManageUsers)

        val current = UserPrefs.getCurrentUserId(requireContext())
        tv.text = "Current user: $current"

        btn.setOnClickListener {
            if (current != "manager") {
                Toast.makeText(requireContext(), "Manager only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), UserManagerActivity::class.java))
        }

        return view
    }
}

