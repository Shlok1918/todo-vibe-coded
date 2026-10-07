package com.shlok.taskcheck.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(tasks: List<Task>, ctx: TaskCtx, monday: Boolean, onAddOn: (String) -> Unit) {
    val g = LocalGlass.current
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selected by rememberSaveable { mutableStateOf(D.today().toString()) }
    val ym = YearMonth.parse(month)
    val first = if (monday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val lead = (ym.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val byDate = tasks.filter { !it.completed && it.dueDate != null }.groupBy { it.dueDate!! }
    val dayTasks = tasks.filter { it.dueDate == selected }.sortedBy { it.completed }

    ScreenList {
        item {
            ScreenHeader("Calendar", ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) {
                GlassIconButton(Icons.Rounded.ChevronLeft, "Previous", { month = ym.minusMonths(1).toString() })
                GlassIconButton(Icons.Rounded.ChevronRight, "Next", { month = ym.plusMonths(1).toString() })
            }
            GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), strong = true, radius = 28) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 0..6) Text(first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), style = Ty.caption, color = g.fg3, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(6.dp))
                    val rows = (lead + ym.lengthOfMonth() + 6) / 7
                    for (r in 0 until rows) Row(Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val day = r * 7 + c - lead + 1
                            Box(Modifier.weight(1f).height(48.dp), Alignment.Center) {
                                if (day in 1..ym.lengthOfMonth()) DayCell(ym.atDay(day), selected, byDate[ym.atDay(day).toString()]?.size ?: 0) { selected = it.toString() }
                            }
                        }
                    }
                }
            }
        }
        item {
            val d = D.parse(selected)
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(d.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = Ty.headline, color = g.fg, modifier = Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Add, "Add on this day", { onAddOn(selected) })
            }
        }
        taskSection("cal-$selected", null, dayTasks, ctx)
        if (dayTasks.isEmpty()) item { Text("Nothing planned.", style = Ty.callout, color = g.fg3, modifier = Modifier.padding(horizontal = 24.dp)) }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: String, count: Int, onPick: (LocalDate) -> Unit) {
    val g = LocalGlass.current
    val isSel = date.toString() == selected
    val isToday = date == D.today()
    Column(Modifier.size(42.dp).clip(CircleShape)
        .background(if (isSel) g.accent else if (isToday) g.accent.copy(alpha = .2f) else Color.Transparent)
        .bounceClick { onPick(date) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("${date.dayOfMonth}", color = if (isSel) Color.White else g.fg, style = Ty.callout, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count.coerceAtMost(3)) { Box(Modifier.padding(top = 2.dp).size(4.dp).background(if (isSel) Color.White else g.accent, CircleShape)) }
        }
    }
}
