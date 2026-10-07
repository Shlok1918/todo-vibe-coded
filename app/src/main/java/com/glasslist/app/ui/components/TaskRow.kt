package com.shlok.taskcheck.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.data.TaskList
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: Task, list: TaskList?, h24: Boolean,
    onToggle: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit, onStar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = LocalGlass.current
    val haptic = LocalHapticFeedback.current
    val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = {
        when (it) {
            SwipeToDismissBoxValue.StartToEnd -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggle(); false }
            SwipeToDismissBoxValue.EndToStart -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDelete(); true }
            else -> false
        }
    })
    val shape = RoundedCornerShape(20.dp)
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundContent = {
            val dir = dismiss.dismissDirection
            val bg by animateColorAsState(when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Success.copy(.85f); SwipeToDismissBoxValue.EndToStart -> Danger.copy(.85f); else -> Color.Transparent
            }, label = "swipe")
            Box(Modifier.fillMaxSize().clip(shape).background(bg).padding(horizontal = 24.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart) {
                if (dir != SwipeToDismissBoxValue.Settled)
                    Icon(if (dir == SwipeToDismissBoxValue.EndToStart) Icons.Rounded.Delete else Icons.Rounded.Check, null, tint = Color.White)
            }
        },
    ) {
        val pc = if (task.priority > 0) PriorityColors[task.priority] else g.accent.copy(alpha = .8f)
        val titleColor by animateColorAsState(if (task.completed) g.fg3 else g.fg, label = "title")
        Row(Modifier.fillMaxWidth().glass(shape).bounceClick(onClick = onOpen).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(start = 10.dp).width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(if (task.priority > 0) pc else Color.Transparent))
            GlassCheck(task.completed, pc) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggle() }
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(task.title, style = Ty.body, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null)
                val due = D.due(task, h24)
                val hasMeta = due != null || task.subtasks.isNotEmpty() || task.repeat != null || task.tags.isNotEmpty() || list != null
                if (hasMeta) FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 3.dp)) {
                    if (due != null) {
                        val c = if (D.isOverdue(task)) Danger else if (task.dueDate == D.today().toString()) g.accent else g.fg2
                        Meta(Icons.Rounded.Schedule, due, c)
                    }
                    if (task.subtasks.isNotEmpty()) Meta(Icons.Rounded.Checklist, "${task.subtasks.count { it.done }}/${task.subtasks.size}", g.fg2)
                    if (task.repeat != null) Meta(Icons.Rounded.Repeat, null, g.fg2)
                    if (task.reminders.isNotEmpty()) Meta(Icons.Rounded.Notifications, null, g.fg2)
                    if (list != null) Text("${list.emoji} ${list.name}", style = Ty.caption, color = g.fg2)
                    task.tags.take(2).forEach { Text("#$it", style = Ty.caption, color = g.accent) }
                }
            }
            IconButton(onClick = onStar, modifier = Modifier.size(40.dp)) {
                Icon(if (task.starred) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Important",
                    tint = if (task.starred) Color(0xFFFFB830) else g.fg3, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        if (text != null) Text(" $text", style = Ty.caption, color = color)
    }
}
