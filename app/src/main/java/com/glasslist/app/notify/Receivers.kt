package com.glasslist.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glasslist.app.container
import com.glasslist.app.domain.D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try { handle(app, intent) } finally { pending.finish() }
        }
    }

    private suspend fun handle(ctx: Context, intent: Intent) {
        val c = ctx.container
        val s = c.settings.current()
        val taskId = intent.getStringExtra(Notifier.X_TASK)
        val remId = intent.getStringExtra(Notifier.X_REM) ?: ""
        when (intent.action) {
            Notifier.ACT_REMIND -> {
                if (!s.notif || taskId == null) return
                val t = c.repo.get(taskId) ?: return
                if (t.completed || t.reminders.none { it.id == remId }) return
                Notifier.showReminder(ctx, t, remId, silent = !s.sound || inQuietHours(s.quiet, s.quietStart, s.quietEnd), h24 = s.h24)
            }
            Notifier.ACT_DONE -> {
                val t = taskId?.let { c.repo.get(it) } ?: return
                if (!t.completed) c.repo.toggle(t)
                Notifier.cancel(ctx, t.id)
            }
            Notifier.ACT_SNOOZE -> {
                if (taskId == null) return
                c.scheduler.snooze(taskId, remId)
                Notifier.cancel(ctx, taskId)
            }
            Notifier.ACT_DIGEST -> {
                val tasks = c.repo.allTasks()
                val today = D.today().toString()
                if (s.notif) when (intent.getStringExtra(Notifier.X_KIND)) {
                    "plan" -> {
                        val due = tasks.filter { !it.completed && it.dueDate != null && it.dueDate <= today }
                        val text = if (due.isEmpty()) "Nothing due today. Enjoy the calm ✨"
                        else "${due.size} to do: " + due.take(3).joinToString(", ") { it.title } + if (due.size > 3) "…" else ""
                        Notifier.showInfo(ctx, 8001, Notifier.CH_DIGEST, "Your day, planned ☀️", text, !s.sound)
                    }
                    else -> {
                        val ms = System.currentTimeMillis() - 86_400_000L
                        val done = tasks.count { it.completed && (it.completedAt ?: 0) > ms }
                        val left = tasks.count { !it.completed && it.dueDate != null && it.dueDate <= today }
                        Notifier.showInfo(ctx, 8002, Notifier.CH_DIGEST, "Evening review 🌙", "You finished $done today · $left still open. Plan tomorrow?", !s.sound)
                    }
                }
                c.scheduler.scheduleDigests(s) // re-arm for tomorrow
            }
            Notifier.ACT_FOCUS -> Notifier.showInfo(ctx, 8003, Notifier.CH_FOCUS, "Focus session complete 🎉", intent.getStringExtra(Notifier.X_TEXT) ?: "Time for a break.")
        }
    }

    private fun inQuietHours(on: Boolean, start: String, end: String): Boolean {
        if (!on) return false
        val now = LocalTime.now(); val a = LocalTime.parse(start); val b = LocalTime.parse(end)
        return if (a <= b) now >= a && now < b else now >= a || now < b
    }
}

/** Re-arms every alarm after reboot, app update or time/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val c = app.container
                c.scheduler.rescheduleAll(c.repo.allTasks(), c.settings.current())
            } finally { pending.finish() }
        }
    }
}
