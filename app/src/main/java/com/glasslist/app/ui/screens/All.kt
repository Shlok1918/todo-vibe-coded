package com.glasslist.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.glasslist.app.data.Task
import com.glasslist.app.data.TaskList
import com.glasslist.app.ui.components.*
import com.glasslist.app.ui.theme.*

private val EMOJIS = listOf("📁", "💼", "🏠", "🛒", "💪", "📚", "✈️", "💡", "🎯", "❤️")

@Composable
fun AllScreen(
    tasks: List<Task>, ctx: TaskCtx, onClearCompleted: () -> Unit,
    onNewList: (String, String, Int) -> Unit, onSaveList: (TaskList) -> Unit, onDeleteList: (TaskList) -> Unit,
) {
    val g = LocalGlass.current
    var filter by rememberSaveable { mutableStateOf("all") }   // all | star | done | list:<id>
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<TaskList?>(null) }
    var creating by remember { mutableStateOf(false) }

    val filtered = tasks.filter { t ->
        when {
            filter == "done" -> t.completed
            filter == "star" -> t.starred && !t.completed
            filter.startsWith("list:") -> t.listId == filter.removePrefix("list:") && !t.completed
            else -> !t.completed
        } && (query.isBlank() || t.title.contains(query, true) || t.notes.contains(query, true) || t.tags.any { it.contains(query, true) })
    }.let { l -> if (filter == "done") l.sortedByDescending { it.completedAt } else l.sortedWith(compareBy({ it.dueDate == null }, { it.dueDate }, { it.sortOrder })) }

    ScreenList {
        item { ScreenHeader("All tasks", "${tasks.count { !it.completed }} open") }
        item {
            TextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search title, notes, #tags", color = g.fg3) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = g.fg2) },
                textStyle = Ty.body.copy(color = g.fg),
                colors = TextFieldDefaults.colors(focusedContainerColor = g.glass, unfocusedContainerColor = g.glass,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = g.accent))
            Row(Modifier.padding(vertical = 12.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("All", filter == "all", { filter = "all" })
                Chip("Important", filter == "star", { filter = "star" }, color = Color(0xFFFFB830), leading = "⭐")
                Chip("Completed", filter == "done", { filter = "done" }, color = Success, leading = "✓")
                ctx.lists.forEach { l ->
                    Chip(l.name, filter == "list:${l.id}", { filter = "list:${l.id}" }, color = Color(l.color), leading = l.emoji, onLongClick = { editing = l })
                }
                Chip("New list", false, { creating = true }, leading = "＋")
            }
            if (filter == "done" && filtered.isNotEmpty())
                TextButton(onClearCompleted, Modifier.padding(horizontal = 8.dp)) { Text("Clear all completed", color = Danger) }
        }
        taskSection("all", null, filtered, ctx)
        if (filtered.isEmpty()) item { EmptyState(Icons.Rounded.Inbox, "Nothing here", if (query.isBlank()) "Tasks matching this view will show up here." else "No results for “$query”.") }
    }
    if (creating) ListDialog(null, { creating = false }, { n, e, c -> onNewList(n, e, c); creating = false }, null)
    editing?.let { l -> ListDialog(l, { editing = null }, { n, e, c -> onSaveList(l.copy(name = n, emoji = e, color = c)); editing = null }, { onDeleteList(l); filter = "all"; editing = null }) }
}

@Composable
private fun ListDialog(existing: TaskList?, onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: EMOJIS[0]) }
    var color by remember { mutableIntStateOf(existing?.color ?: Accents[0].color.toArgb()) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New list" else "Edit list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EMOJIS.forEach { e -> Chip("", emoji == e, { emoji = e }, leading = e) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accents.forEach { a -> Swatch(a.color, color == a.color.toArgb()) { color = a.color.toArgb() } }
                }
            }
        },
        confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name.trim(), emoji, color) }) { Text("Save") } },
        dismissButton = { Row { onDelete?.let { TextButton(it) { Text("Delete", color = Danger) } }; TextButton(onDismiss) { Text("Cancel") } } })
}

@Composable
fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color).bounceClick(onClick = onClick), Alignment.Center) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
