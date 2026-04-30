package com.crewsync.crewsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.crewsync.crewsync.Fragments.CreateFragment
import com.crewsync.crewsync.Fragments.SettingsFragment
import com.crewsync.crewsync.Fragments.DashboardFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        // default tab
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, DashboardFragment())
                .commit()
        }

        bottom.setOnItemSelectedListener { item ->
            val frag = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_calendar -> CalendarFragment()
                R.id.nav_create -> CreateFragment()
                R.id.nav_notifications -> NotificationsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, frag)
                .commit()
            true
        }
    }
}

