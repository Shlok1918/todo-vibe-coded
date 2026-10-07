package com.shlok.taskcheck.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(tasks: List<Task>) {
    val g = LocalGlass.current
    val today = D.today()
    val perDay = remember(tasks) { tasks.filter { it.completed && it.completedAt != null }.groupingBy { D.dateOf(it.completedAt!!) }.eachCount() }
    val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val max = (last7.maxOf { perDay[it] ?: 0 }).coerceAtLeast(1)
    val completed = tasks.count { it.completed }
    val open = tasks.size - completed
    val rate = if (tasks.isEmpty()) 0 else completed * 100 / tasks.size
    var streak = 0
    var d = if ((perDay[today] ?: 0) > 0) today else today.minusDays(1)
    while ((perDay[d] ?: 0) > 0) { streak++; d = d.minusDays(1) }
    val week = last7.sumOf { perDay[it] ?: 0 }

    ScreenList {
        item { ScreenHeader("Insights", "Your momentum") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Done", "$completed", Success, Modifier.weight(1f))
                Stat("Streak", "🔥 ${streak}d", g.accent, Modifier.weight(1f))
                Stat("Rate", "$rate%", Color3, Modifier.weight(1f))
            }
        }
        item {
            GlassCard(Modifier.padding(16.dp).fillMaxWidth(), strong = true, radius = 28) {
                Column(Modifier.padding(20.dp)) {
                    Text("This week · $week completed", style = Ty.headline, color = g.fg)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        last7.forEach { day ->
                            val n = perDay[day] ?: 0
                            val h by animateFloatAsState(n.toFloat() / max, spring(dampingRatio = 0.65f, stiffness = 120f), label = "bar")
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text("$n", style = Ty.caption, color = g.fg3)
                                Box(Modifier.fillMaxWidth().fillMaxHeight(h.coerceAtLeast(0.04f) * 0.78f).clip(RoundedCornerShape(8.dp))
                                    .background(if (day == today) g.accent else g.accent.copy(alpha = .45f)))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        last7.forEach { Text(it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), style = Ty.caption, color = g.fg2, textAlign = TextAlign.Center) }
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 28) {
                Column(Modifier.padding(20.dp)) {
                    Text("Last 5 weeks", style = Ty.headline, color = g.fg)
                    Spacer(Modifier.height(12.dp))
                    val start = today.minusDays(34)
                    for (w in 0 until 5) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (dd in 0 until 7) {
                            val date = start.plusDays((w * 7 + dd).toLong()); val n = perDay[date] ?: 0
                            Box(Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (n == 0) g.fg.copy(alpha = .07f) else g.accent.copy(alpha = (0.3f + n * 0.2f).coerceAtMost(1f))))
                        }
                    }
                    Text("$open open · ${tasks.count { it.starred && !it.completed }} important", style = Ty.caption, color = g.fg2, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

private val Color3 = androidx.compose.ui.graphics.Color(0xFFFFB830)

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val g = LocalGlass.current
    GlassCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = Ty.title, color = color)
            Text(label, style = Ty.caption, color = g.fg2, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
