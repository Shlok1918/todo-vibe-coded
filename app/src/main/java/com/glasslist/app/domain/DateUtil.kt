package com.shlok.taskcheck.domain

import com.shlok.taskcheck.data.Task
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

object D {
    fun today(): LocalDate = LocalDate.now()
    fun parse(s: String): LocalDate = LocalDate.parse(s)
    fun pad(h: Int, m: Int) = "%02d:%02d".format(h, m)

    fun isOverdue(t: Task): Boolean {
        val d = t.dueDate ?: return false
        return !t.completed && parse(d).isBefore(today())
    }

    fun relative(iso: String): String {
        val d = parse(iso)
        val days = java.time.temporal.ChronoUnit.DAYS.between(today(), d)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days == -1L -> "Yesterday"
            days in 2L..6L -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            else -> "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
        }
    }

    fun time(t: String, h24: Boolean): String {
        val lt = LocalTime.parse(t)
        if (h24) return t
        val hour12 = if (lt.hour % 12 == 0) 12 else lt.hour % 12
        val suffix = if (lt.hour >= 12) "PM" else "AM"
        return if (lt.minute == 0) "$hour12 $suffix" else "$hour12:%02d $suffix".format(lt.minute)
    }

    fun due(t: Task, h24: Boolean): String? {
        val d = t.dueDate ?: return null
        return t.dueTime?.let { "${relative(d)}, ${time(it, h24)}" } ?: relative(d)
    }

    fun greeting(): String = when (LocalTime.now().hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

    fun offsetLabel(m: Int): String = when (m) {
        0 -> "At time"; 5 -> "5 min before"; 15 -> "15 min before"; 30 -> "30 min before"
        60 -> "1 hour before"; 1440 -> "1 day before"; else -> "$m min before"
    }

    fun millisOf(d: LocalDate, t: LocalTime): Long = LocalDateTime.of(d, t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
