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
import com.crewsync.crewsync.UserProfileActivity
import com.crewsync.crewsync.UserStore

class SettingsFragment : Fragment() {

    private lateinit var tv: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        tv = view.findViewById(R.id.tvCurrentUser)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        val btnManageUsers = view.findViewById<Button>(R.id.btnManageUsers)

        refreshCurrentUserText()

        val currentUser = UserPrefs.getCurrentUserId(requireContext())
        btnManageUsers.visibility = if (currentUser == "manager") View.VISIBLE else View.GONE

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), UserProfileActivity::class.java))
        }

        btnManageUsers.setOnClickListener {
            val current = UserPrefs.getCurrentUserId(requireContext())
            if (current != "manager") {
                Toast.makeText(requireContext(), "Manager only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), UserManagerActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::tv.isInitialized) {
            refreshCurrentUserText()
        }
    }

    private fun refreshCurrentUserText() {
        val current = UserPrefs.getCurrentUserId(requireContext())
        val display = UserStore.getDisplayName(requireContext(), current)
        tv.text = "Current user: $display ($current)"
    }
}
