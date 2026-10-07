package com.shlok.taskcheck.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun UpcomingScreen(tasks: List<Task>, ctx: TaskCtx) {
    val today = D.today()
    val future = tasks.filter { !it.completed && it.dueDate != null && D.parse(it.dueDate).isAfter(today) }
    val byDay = future.groupBy { it.dueDate!! }.toSortedMap()
    ScreenList {
        item { ScreenHeader("Upcoming", "${future.size} planned") }
        byDay.forEach { (d, ts) ->
            val date = D.parse(d)
            val label = if (date == today.plusDays(1)) "Tomorrow" else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            taskSection(d, label, ts.sortedWith(compareBy({ it.dueTime == null }, { it.dueTime })), ctx)
        }
        if (future.isEmpty()) item { EmptyState(Icons.Rounded.Upcoming, "Nothing ahead", "Tasks with future dates will appear here, grouped by day.") }
    }
}
