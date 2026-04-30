package com.crewsync.crewsync

object EventRepository {
    fun getEvents(): List<Event> = listOf(
        Event(1, "Rehearsal - Setlist Run", "Mon 7:00 PM", "Rehearsal", "Garage Studio", "Practice intros + transitions"),
        Event(2, "Gig @ Calakas Raza", "Sat 9:00 PM", "Performance", "Downtown", "Arrive early, soundcheck 8:15"),
        Event(3, "Team Meeting", "Wed 5:30 PM", "Meeting", "Coffee shop", null),
        Event(4, "Personal: Replace Strings", "Thu 6:00 PM", "Personal", null, "NYXL set + tune stability check"),
        Event(5, "Photo/Promo Content", "Sun 2:00 PM", "Content", "Home", "Record 3 clips for TikTok")
    )
}
