package com.shlok.taskcheck.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shlok.taskcheck.data.Settings
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D
import java.time.LocalDate
import java.time.LocalTime

/** Wraps AlarmManager. Reminders are exact + Doze-safe when the user grants the exact-alarm permission. */
class AlarmScheduler(private val ctx: Context) {
    private val am = ctx.getSystemService(AlarmManager::class.java)

    fun canExact() = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    private fun pi(act: String, code: Int, build: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, code, Intent(ctx, AlarmReceiver::class.java).setAction(act).apply(build),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun fire(at: Long, p: PendingIntent) {
        if (canExact()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    private fun remCode(taskId: String, remId: String) = (taskId + remId).hashCode()
    private fun remPi(taskId: String, remId: String) = pi(Notifier.ACT_REMIND, remCode(taskId, remId)) {
        putExtra(Notifier.X_TASK, taskId); putExtra(Notifier.X_REM, remId)
    }

    fun schedule(task: Task) {
        cancel(task)
        if (task.completed) return
        val now = System.currentTimeMillis()
        task.reminders.forEach { r ->
            val at = r.absoluteMillis ?: task.dueDate?.let {
                D.millisOf(LocalDate.parse(it), task.dueTime?.let(LocalTime::parse) ?: LocalTime.of(9, 0)) - r.offsetMinutes * 60_000L
            } ?: return@forEach
            if (at > now) fire(at, remPi(task.id, r.id))
        }
    }

    fun cancel(task: Task) = task.reminders.forEach { am.cancel(remPi(task.id, it.id)) }

    fun snooze(taskId: String, remId: String, minutes: Int = 10) {
        fire(System.currentTimeMillis() + minutes * 60_000L, remPi(taskId, remId))
    }

    // ---- Daily plan / evening review ------------------------------------------------------
    private fun digest(kind: String, hhmm: String, on: Boolean) {
        val p = pi(Notifier.ACT_DIGEST, kind.hashCode()) { putExtra(Notifier.X_KIND, kind) }
        am.cancel(p)
        if (!on) return
        val t = LocalTime.parse(hhmm)
        var at = D.millisOf(LocalDate.now(), t)
        if (at <= System.currentTimeMillis() + 1000) at = D.millisOf(LocalDate.now().plusDays(1), t)
        fire(at, p)
    }

    fun scheduleDigests(s: Settings) {
        digest("plan", s.planAt, s.notif && s.plan)
        digest("review", s.reviewAt, s.notif && s.review)
    }

    fun rescheduleAll(tasks: List<Task>, s: Settings) {
        tasks.forEach(::schedule)
        scheduleDigests(s)
    }

    // ---- Focus timer end ---------------------------------------------------------------------
    fun scheduleFocus(atMillis: Long, text: String) = fire(atMillis, pi(Notifier.ACT_FOCUS, 7001) { putExtra(Notifier.X_TEXT, text) })
    fun cancelFocus() = am.cancel(pi(Notifier.ACT_FOCUS, 7001))
}
