package com.crewsync.crewsync

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {

    private lateinit var listNotifications: LinearLayout
    private lateinit var tvSubtitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        listNotifications = view.findViewById(R.id.listNotifications)
        tvSubtitle = view.findViewById(R.id.tvNotificationsSubtitle)

        return view
    }

    override fun onResume() {
        super.onResume()
        renderNotifications()
    }

    private fun renderNotifications() {
        val currentUser = UserPrefs.getCurrentUserId(requireContext())
        val items = NotificationStore.getNotificationsForUser(requireContext(), currentUser)

        tvSubtitle.text =
            if (currentUser == "manager") "Recent activity for all users"
            else "Recent activity for $currentUser"

        listNotifications.removeAllViews()

        if (items.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No notifications yet."
                textSize = 16f
                setPadding(0, 20, 0, 20)
            }
            listNotifications.addView(empty)
            return
        }

        items.forEach { item ->
            listNotifications.addView(buildNotificationRow(item))
        }
    }

    private fun buildNotificationRow(item: NotificationItem): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            setOnClickListener {
                openTarget(item)
            }
        }

        val message = TextView(requireContext()).apply {
            text = item.message
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val time = TextView(requireContext()).apply {
            text = "For ${item.userId} • ${formatTime(item.createdAtMillis)}"
            textSize = 13f
        }

        row.addView(message)
        row.addView(time)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 10
        }

        row.layoutParams = params

        return row
    }

    private fun openTarget(item: NotificationItem) {
        val event = EventRepository.getEventById(requireContext(), item.eventId)

        if (event == null) {
            Toast.makeText(requireContext(), "Related event not found", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), EventDetailActivity::class.java)
        intent.putExtra("eventId", event.id)
        intent.putExtra("title", event.title)
        intent.putExtra("dateTime", event.dateTime)
        intent.putExtra("category", event.category)
        intent.putExtra("location", event.location)
        intent.putExtra("notesPreview", event.notesPreview)
        intent.putExtra("targetTaskId", item.taskId)
        startActivity(intent)
    }

    private fun formatTime(millis: Long): String {
        return DateFormat.format("MMM d, h:mm a", millis).toString()
    }
}
