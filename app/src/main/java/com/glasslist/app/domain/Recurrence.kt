package com.shlok.taskcheck.domain

import com.shlok.taskcheck.data.Task
import java.time.LocalDate
import java.util.UUID

object Recurrence {
    fun next(t: Task): Task? {
        val r = t.repeat ?: return null
        val due = t.dueDate?.let(LocalDate::parse) ?: return null
        val n = r.interval.coerceAtLeast(1)
        val nextDate: LocalDate = when (r.freq) {
            "daily", "custom" -> due.plusDays(n.toLong())
            "weekly" -> if (r.daysOfWeek.isEmpty()) due.plusWeeks(n.toLong()) else {
                var d = due.plusDays(1)
                var guard = 0
                while (d.dayOfWeek.value % 7 !in r.daysOfWeek && guard++ < 7 * n) d = d.plusDays(1)
                if (guard > 7 * n) due.plusWeeks(n.toLong()) else d
            }
            "monthly" -> due.plusMonths(n.toLong())
            "yearly" -> due.plusYears(n.toLong())
            else -> return null
        }
        if (r.endDate != null && nextDate.isAfter(LocalDate.parse(r.endDate))) return null
        return t.copy(
            id = UUID.randomUUID().toString(), completed = false, completedAt = null,
            createdAt = System.currentTimeMillis(), dueDate = nextDate.toString(),
            reminders = t.reminders.map { it.copy(id = UUID.randomUUID().toString()) },
            subtasks = t.subtasks.map { it.copy(id = UUID.randomUUID().toString(), done = false) },
        )
    }
}
