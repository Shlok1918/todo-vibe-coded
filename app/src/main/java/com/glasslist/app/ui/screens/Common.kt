package com.shlok.taskcheck.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.data.Task
import com.shlok.taskcheck.data.TaskList
import com.shlok.taskcheck.ui.components.SectionLabel
import com.shlok.taskcheck.ui.components.TaskRow
import com.shlok.taskcheck.ui.theme.Danger

/** Everything a screen needs to render and act on tasks. */
class TaskCtx(
    val lists: List<TaskList>, val h24: Boolean,
    val onToggle: (Task) -> Unit, val onOpen: (Task) -> Unit, val onDelete: (Task) -> Unit, val onStar: (Task) -> Unit,
)

fun LazyListScope.taskSection(key: String, title: String?, tasks: List<Task>, ctx: TaskCtx, danger: Boolean = false) {
    if (tasks.isEmpty()) return
    if (title != null) item(key = "h-$key") {
        SectionLabel("$title · ${tasks.size}", color = if (danger) Danger else com.shlok.taskcheck.ui.theme.LocalGlass.current.fg2)
    }
    items(tasks.size, key = { "$key-${tasks[it].id}" }) { i ->
        val t = tasks[i]
        TaskRow(t, ctx.lists.firstOrNull { it.id == t.listId }, ctx.h24, { ctx.onToggle(t) }, { ctx.onOpen(t) }, { ctx.onDelete(t) }, { ctx.onStar(t) },
            Modifier.animateItem())
    }
}

@Composable
fun ScreenList(state: LazyListState = rememberLazyListState(), content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), state, contentPadding = PaddingValues(top = 4.dp, bottom = 190.dp), content = content)
}
