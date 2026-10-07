package com.shlok.taskcheck.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shlok.taskcheck.MainActivity
import com.shlok.taskcheck.R
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D

object Notifier {
    const val CH_REMINDER = "reminders"
    const val CH_DIGEST = "digest"
    const val CH_FOCUS = "focus"

    const val ACT_REMIND = "com.shlok.taskcheck.REMIND"
    const val ACT_DONE = "com.shlok.taskcheck.DONE"
    const val ACT_SNOOZE = "com.shlok.taskcheck.SNOOZE"
    const val ACT_DIGEST = "com.shlok.taskcheck.DIGEST"
    const val ACT_FOCUS = "com.shlok.taskcheck.FOCUS"
    const val X_TASK = "taskId"
    const val X_REM = "reminderId"
    const val X_KIND = "kind"
    const val X_TEXT = "text"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts when a task is due"; enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_DIGEST, "Daily plan & review", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_FOCUS, "Focus timer", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openApp(ctx: Context, taskId: String?, code: Int): PendingIntent =
        PendingIntent.getActivity(
            ctx, code,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(X_TASK, taskId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun action(ctx: Context, act: String, taskId: String, remId: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, (act + taskId).hashCode(),
            Intent(ctx, AlarmReceiver::class.java).setAction(act).putExtra(X_TASK, taskId).putExtra(X_REM, remId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun notifId(taskId: String) = taskId.hashCode()

    @SuppressLint("MissingPermission")
    fun showReminder(ctx: Context, task: Task, remId: String, silent: Boolean, h24: Boolean) {
        val whenText = D.due(task, h24) ?: "Now"
        val n = NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(whenText + if (task.subtasks.isNotEmpty()) " · ${task.subtasks.count { it.done }}/${task.subtasks.size} steps" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.notes.ifBlank { whenText }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFF5B7CFD.toInt())
            .setAutoCancel(true)
            .setSilent(silent)
            .setContentIntent(openApp(ctx, task.id, notifId(task.id)))
            .addAction(0, "Done", action(ctx, ACT_DONE, task.id, remId))
            .addAction(0, "Snooze 10m", action(ctx, ACT_SNOOZE, task.id, remId))
            .build()
        NotificationManagerCompat.from(ctx).notify(notifId(task.id), n)
    }

    @SuppressLint("MissingPermission")
    fun showInfo(ctx: Context, id: Int, channel: String, title: String, text: String, silent: Boolean = false) {
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFF5B7CFD.toInt()).setAutoCancel(true).setSilent(silent)
            .setContentIntent(openApp(ctx, null, id))
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    fun cancel(ctx: Context, taskId: String) = NotificationManagerCompat.from(ctx).cancel(notifId(taskId))
}
