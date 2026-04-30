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
import com.crewsync.crewsync.CategoryManagerActivity
import com.crewsync.crewsync.R
import com.crewsync.crewsync.UserManagerActivity
import com.crewsync.crewsync.data.UserPrefs
import com.crewsync.crewsync.UserProfileActivity
import com.crewsync.crewsync.data.UserStore

class SettingsFragment : Fragment() {

    private lateinit var tv: TextView
    private lateinit var tvManagerSection: TextView
    private lateinit var btnManageUsers: Button
    private lateinit var btnManageCategories: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        tv = view.findViewById(R.id.tvCurrentUser)
        tvManagerSection = view.findViewById(R.id.tvManagerSection)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        btnManageUsers = view.findViewById(R.id.btnManageUsers)
        btnManageCategories = view.findViewById(R.id.btnManageCategories)

        refreshCurrentUserText()
        refreshManagerVisibility()

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

        btnManageCategories.setOnClickListener {
            val current = UserPrefs.getCurrentUserId(requireContext())
            if (current != "manager") {
                Toast.makeText(requireContext(), "Manager only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), CategoryManagerActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::tv.isInitialized) {
            refreshCurrentUserText()
            refreshManagerVisibility()
        }
    }

    private fun refreshCurrentUserText() {
        val current = UserPrefs.getCurrentUserId(requireContext())
        val display = UserStore.getDisplayName(requireContext(), current)
        tv.text = "Current user: $display ($current)"
    }

    private fun refreshManagerVisibility() {
        val isManager = UserPrefs.getCurrentUserId(requireContext()) == "manager"

        tvManagerSection.visibility = if (isManager) View.VISIBLE else View.GONE
        btnManageUsers.visibility = if (isManager) View.VISIBLE else View.GONE
        btnManageCategories.visibility = if (isManager) View.VISIBLE else View.GONE
    }
}
