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
        val currentName = UserStore.getDisplayName(requireContext(), currentUser)
        val items = NotificationStore.getNotificationsForUser(requireContext(), currentUser)

        tvSubtitle.text =
            if (currentUser == "manager") "Recent activity across CrewSync"
            else "Recent activity for $currentName"

        listNotifications.removeAllViews()

        if (items.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No notifications yet."
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_muted, null))
                setPadding(16, 28, 16, 28)
                setBackgroundResource(R.drawable.bg_notification_card)
            }

            listNotifications.addView(
                empty,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }

        items.forEach { item ->
            listNotifications.addView(buildNotificationRow(item))
        }
    }

    private fun buildNotificationRow(item: NotificationItem): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(14, 14, 14, 14)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_notification_card)
            setOnClickListener {
                openTarget(item)
            }
        }

        val actorName = UserStore.getDisplayName(requireContext(), item.actorUserId)
        val displayMessage =
            if (item.message.startsWith(item.actorUserId)) {
                actorName + item.message.removePrefix(item.actorUserId)
            } else {
                item.message
            }

        val avatarText = TextView(requireContext()).apply {
            text = actorInitial(actorName, item.actorUserId)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.text_primary, null))
            setBackgroundResource(R.drawable.bg_notification_avatar)
        }

        val avatarParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            marginEnd = dp(12)
        }

        row.addView(avatarText, avatarParams)

        val textColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val message = TextView(requireContext()).apply {
            text = displayMessage
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resources.getColor(R.color.text_primary, null))
            setLineSpacing(2f, 1.05f)
        }

        val time = TextView(requireContext()).apply {
            text = notificationMeta(item)
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted, null))
            setPadding(0, 6, 0, 0)
        }

        textColumn.addView(message)
        textColumn.addView(time)

        row.addView(
            textColumn,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        row.layoutParams = params

        return row
    }

    private fun notificationMeta(item: NotificationItem): String {
        val currentUser = UserPrefs.getCurrentUserId(requireContext())

        val audience =
            if (currentUser == "manager") {
                val userName = UserStore.getDisplayName(requireContext(), item.userId)
                "For $userName"
            } else {
                "For you"
            }

        return "$audience • ${formatTime(item.createdAtMillis)}"
    }

    private fun actorInitial(actorName: String, actorUserId: String): String {
        val source = actorName.ifBlank { actorUserId }.ifBlank { "C" }
        return source.trim().first().uppercaseChar().toString()
    }

    private fun openTarget(item: NotificationItem) {
        if (item.targetType == "user") {
            Toast.makeText(requireContext(), "User update notification", Toast.LENGTH_SHORT).show()
            return
        }

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
        intent.putExtra("dateTimeMillis", event.dateTimeMillis ?: -1L)
        intent.putExtra("targetTaskId", item.taskId)
        startActivity(intent)
    }

    private fun formatTime(millis: Long): String {
        return DateFormat.format("MMM d, h:mm a", millis).toString()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
