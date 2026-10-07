package com.shlok.taskcheck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(tasks: List<Task>, ctx: TaskCtx, onQuickAdd: (String) -> Unit, onSettings: () -> Unit, onFocus: () -> Unit) {
    val g = LocalGlass.current
    val today = D.today().toString()
    val overdue = tasks.filter { D.isOverdue(it) }.sortedBy { it.dueDate }
    val dueToday = tasks.filter { !it.completed && it.dueDate == today }.sortedWith(compareBy({ it.dueTime == null }, { it.dueTime }, { it.sortOrder }))
    val inbox = tasks.filter { !it.completed && it.dueDate == null }.sortedBy { it.sortOrder }
    val doneToday = tasks.filter { it.completed && it.completedAt?.let { c -> D.dateOf(c) == D.today() } == true }
    val total = overdue.size + dueToday.size + doneToday.size
    val line = when {
        total == 0 -> "All clear. Enjoy your day."
        doneToday.isEmpty() -> "Ready when you are."
        doneToday.size < total / 2 -> "Good start — keep the momentum."
        overdue.size + dueToday.size > 0 -> "Almost there. You've got this."
        else -> "🎉 All done. Well earned."
    }
    ScreenList {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(D.today().format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = Ty.callout, color = g.fg2)
                    Text(D.greeting(), style = Ty.largeTitle, color = g.fg)
                    Text(line, style = Ty.callout, color = g.fg2, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (total > 0) ProgressRing(doneToday.size, total, 72)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Timer, "Focus", onFocus)
                GlassIconButton(Icons.Rounded.Settings, "Settings", onSettings)
            }
            QuickAdd(ctx.h24, onQuickAdd, Modifier.padding(horizontal = 16.dp))
        }
        taskSection("od", "Overdue", overdue, ctx, danger = true)
        taskSection("td", "Today", dueToday, ctx)
        taskSection("in", "Inbox", inbox.take(8), ctx)
        taskSection("dn", "Completed today", doneToday, ctx)
        if (overdue.isEmpty() && dueToday.isEmpty() && inbox.isEmpty() && doneToday.isEmpty())
            item { EmptyState(Icons.Rounded.WbSunny, "A blank canvas", "Add your first task above — dates and priorities are parsed for you.") }
    }
}
