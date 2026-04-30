package com.crewsync.crewsync

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var tvCalendarTitle: TextView
    private lateinit var gridWeekHeader: GridLayout
    private lateinit var gridCalendar: GridLayout
    private lateinit var tvSelectedDay: TextView
    private lateinit var listSelectedEvents: LinearLayout

    private val visibleMonth: Calendar = Calendar.getInstance()
    private val selectedDay: Calendar = Calendar.getInstance()

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val selectedFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        tvCalendarTitle = view.findViewById(R.id.tvCalendarTitle)
        gridWeekHeader = view.findViewById(R.id.gridWeekHeader)
        gridCalendar = view.findViewById(R.id.gridCalendar)
        tvSelectedDay = view.findViewById(R.id.tvSelectedDay)
        listSelectedEvents = view.findViewById(R.id.listSelectedEvents)

        val btnPrev = view.findViewById<Button>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<Button>(R.id.btnNextMonth)

        btnPrev.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            renderCalendar()
        }

        btnNext.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            renderCalendar()
        }

        renderWeekHeader()
        renderCalendar()

        return view
    }

    private fun renderWeekHeader() {
        gridWeekHeader.removeAllViews()

        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (day in days) {
            val tv = TextView(requireContext()).apply {
                text = day
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = 13f
                setTextColor(Color.DKGRAY)
            }

            gridWeekHeader.addView(tv, gridParams())
        }
    }

    private fun renderCalendar() {
        gridCalendar.removeAllViews()

        tvCalendarTitle.text = monthFormat.format(visibleMonth.time)

        val monthCal = visibleMonth.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Blank cells before the first day
        for (i in 1 until firstDayOfWeek) {
            val blank = TextView(requireContext()).apply {
                text = ""
            }
            gridCalendar.addView(blank, dayCellParams())
        }

        for (day in 1..daysInMonth) {
            val cellDate = visibleMonth.clone() as Calendar
            cellDate.set(Calendar.DAY_OF_MONTH, day)

            val cell = buildDayCell(cellDate)
            gridCalendar.addView(cell, dayCellParams())
        }

        renderSelectedDayEvents()
    }

    private fun buildDayCell(date: Calendar): LinearLayout {
        val events = eventsForDate(date)

        val isToday = sameDate(date, Calendar.getInstance())
        val isSelected = sameDate(date, selectedDay)

        val bg = GradientDrawable().apply {
            cornerRadius = 12f
            setColor(
                when {
                    isSelected -> Color.rgb(230, 240, 255)
                    isToday -> Color.rgb(240, 240, 240)
                    else -> Color.TRANSPARENT
                }
            )
            setStroke(1, Color.rgb(220, 220, 220))
        }

        val cell = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(6, 6, 6, 6)
            background = bg
            isClickable = true
            setOnClickListener {
                selectedDay.timeInMillis = date.timeInMillis
                renderCalendar()
            }
        }

        val dayNumber = TextView(requireContext()).apply {
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 13f
            typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(Color.BLACK)
        }

        cell.addView(dayNumber)

        // Show up to two colored event chips in the day cell
        events.take(2).forEach { event ->
            val chip = TextView(requireContext()).apply {
                text = event.title.take(12)
                textSize = 10f
                setTextColor(Color.WHITE)
                maxLines = 1
                setPadding(5, 2, 5, 2)
                background = roundedColor(CategoryColorStore.getColor(requireContext(), event.category))
                isClickable = true
                setOnClickListener {
                    openEvent(event)
                }
            }

            val chipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }

            cell.addView(chip, chipParams)
        }

        if (events.size > 2) {
            val more = TextView(requireContext()).apply {
                text = "+${events.size - 2} more"
                textSize = 10f
                setTextColor(Color.DKGRAY)
            }
            cell.addView(more)
        }

        return cell
    }

    private fun renderSelectedDayEvents() {
        val events = eventsForDate(selectedDay)

        tvSelectedDay.text = "Events for ${selectedFormat.format(selectedDay.time)}"
        listSelectedEvents.removeAllViews()

        if (events.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No events scheduled."
                textSize = 15f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 8)
            }
            listSelectedEvents.addView(empty)
            return
        }

        for (event in events) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
                isClickable = true
                setOnClickListener {
                    openEvent(event)
                }
            }

            val colorBar = View(requireContext()).apply {
                background = roundedColor(CategoryColorStore.getColor(requireContext(), event.category))
            }

            val colorParams = LinearLayout.LayoutParams(14, 60).apply {
                marginEnd = 10
            }

            val text = TextView(requireContext()).apply {
                this.text = "${event.title}\n${event.category} • ${event.dateTime}"
                textSize = 15f
                setTextColor(Color.BLACK)
            }

            row.addView(colorBar, colorParams)
            row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            listSelectedEvents.addView(row)
        }
    }

    private fun eventsForDate(date: Calendar): List<Event> {
        val dayName = SimpleDateFormat("EEE", Locale.US).format(date.time)

        return EventRepository.getEvents(requireContext()).filter { event ->
            if (event.dateTimeMillis != null) {
                val eventCal = Calendar.getInstance()
                eventCal.timeInMillis = event.dateTimeMillis
                sameDate(date, eventCal)
            } else {
                event.dateTime.startsWith(dayName, ignoreCase = true)
            }
        }
    }

    private fun openEvent(event: Event) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java)
        intent.putExtra("eventId", event.id)
        intent.putExtra("title", event.title)
        intent.putExtra("dateTime", event.dateTime)
        intent.putExtra("category", event.category)
        intent.putExtra("location", event.location)
        intent.putExtra("notesPreview", event.notesPreview)
        startActivity(intent)
    }

    private fun sameDate(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    }

    private fun roundedColor(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 10f
            setColor(color)
        }
    }

    private fun gridParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(2, 2, 2, 2)
        }
    }

    private fun dayCellParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = 130
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(3, 3, 3, 3)
        }
    }
}
