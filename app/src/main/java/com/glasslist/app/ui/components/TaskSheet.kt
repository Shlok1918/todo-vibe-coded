package com.glasslist.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.glasslist.app.data.*
import com.glasslist.app.domain.D
import com.glasslist.app.ui.theme.*
import java.time.*
import java.util.UUID

private val OFFSETS = listOf(0, 5, 15, 30, 60, 1440)
private val REPEATS = listOf(null to "Never", "daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSheet(
    initial: Task, isNew: Boolean, lists: List<TaskList>, h24: Boolean, defaultOffset: Int, notifOn: Boolean,
    onSave: (Task) -> Unit, onDelete: () -> Unit, onFocus: () -> Unit, onDismiss: () -> Unit,
) {
    val g = LocalGlass.current
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var date by remember { mutableStateOf(initial.dueDate) }
    var time by remember { mutableStateOf(initial.dueTime) }
    var prio by remember { mutableIntStateOf(initial.priority) }
    var listId by remember { mutableStateOf(initial.listId) }
    var tags by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var subs by remember { mutableStateOf(initial.subtasks) }
    var newSub by remember { mutableStateOf("") }
    var offsets by remember { mutableStateOf(initial.reminders.mapNotNull { if (it.absoluteMillis == null) it.offsetMinutes else null }.toSet()) }
    var repeat by remember { mutableStateOf(initial.repeat?.freq) }
    var starred by remember { mutableStateOf(initial.starred) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun build(): Task {
        val keep = initial.reminders.filter { it.absoluteMillis != null }
        val rems = if (date == null) keep else keep + offsets.map { o -> initial.reminders.firstOrNull { it.offsetMinutes == o && it.absoluteMillis == null } ?: Reminder(UUID.randomUUID().toString(), o) }
        return initial.copy(
            title = title.trim(), notes = notes, dueDate = date, dueTime = if (date == null) null else time, priority = prio, listId = listId,
            tags = tags.split(",").map { it.trim().trimStart('#') }.filter { it.isNotEmpty() },
            subtasks = subs, reminders = rems, starred = starred,
            repeat = if (repeat == null || date == null) null else (initial.repeat?.takeIf { it.freq == repeat } ?: Repeat(freq = repeat!!)),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = g.sheet, contentColor = g.fg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNew) "New task" else "Edit task", style = Ty.title, color = g.fg, modifier = Modifier.weight(1f))
                IconButton(onClick = { starred = !starred }) { Icon(if (starred) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Important", tint = if (starred) Color(0xFFFFB830) else g.fg3) }
                if (!isNew) IconButton(onClick = onFocus) { Icon(Icons.Rounded.Timer, "Focus", tint = g.accent) }
            }
            SheetField(title, { title = it }, "What needs doing?", Ty.headline, singleLine = true)
            SheetField(notes, { notes = it }, "Notes", Ty.body, minLines = 2)

            Label("When")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Today", date == D.today().toString(), { date = D.today().toString() })
                Chip("Tomorrow", date == D.today().plusDays(1).toString(), { date = D.today().plusDays(1).toString() })
                Chip(date?.let { D.relative(it) + " ·  pick" } ?: "Pick date", false, { pickDate = true }, leading = "📅")
                if (date != null) Chip(time?.let { D.time(it, h24) } ?: "Add time", time != null, { pickTime = true }, leading = "⏰")
                if (date != null) Chip("Clear", false, { date = null; time = null }, color = Danger)
            }

            Label("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityNames.forEachIndexed { i, n -> Chip(n, prio == i, { prio = i }, color = if (i == 0) g.accent else PriorityColors[i]) }
            }

            if (lists.isNotEmpty()) {
                Label("List")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("None", listId == null, { listId = null })
                    lists.forEach { Chip(it.name, listId == it.id, { listId = it.id }, color = Color(it.color), leading = it.emoji) }
                }
            }

            Label("Subtasks")
            subs.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(s.done, { subs = subs.toMutableList().also { l -> l[i] = s.copy(done = it) } })
                    Text(s.title, color = if (s.done) g.fg3 else g.fg, modifier = Modifier.weight(1f))
                    IconButton(onClick = { subs = subs.filterIndexed { j, _ -> j != i } }) { Icon(Icons.Rounded.Close, "Remove", tint = g.fg3) }
                }
            }
            SheetField(newSub, { newSub = it }, "Add a step", Ty.body, singleLine = true, ime = ImeAction.Done,
                onDone = { if (newSub.isNotBlank()) { subs = subs + Subtask(UUID.randomUUID().toString(), newSub.trim()); newSub = "" } })

            if (date != null) {
                Label(if (time == null) "Reminders (at 9:00 — add a time to be precise)" else "Reminders")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OFFSETS.forEach { o -> Chip(D.offsetLabel(o), o in offsets, { offsets = if (o in offsets) offsets - o else offsets + o }) }
                }
                if (!notifOn) Text("Notifications are off in Settings.", style = Ty.caption, color = Danger, modifier = Modifier.padding(top = 4.dp))
                Label("Repeat")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REPEATS.forEach { (k, n) -> Chip(n, repeat == k, { repeat = k }) }
                }
            }

            Label("Tags")
            SheetField(tags, { tags = it }, "work, home, idea", Ty.body, singleLine = true)

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isNew) Box(Modifier.glass(RoundedCornerShape(16.dp)).bounceClick(onClick = onDelete).padding(14.dp)) { Icon(Icons.Rounded.Delete, "Delete", tint = Danger) }
                AccentButton(if (isNew) "Add task" else "Save", { if (title.isNotBlank()) onSave(build()) }, Modifier.weight(1f), Icons.Rounded.Check)
            }
        }
    }

    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = (date?.let(LocalDate::parse) ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pickDate = false },
            confirmButton = { TextButton({ st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; pickDate = false }) { Text("OK") } },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel") } }) { DatePicker(st) }
    }
    if (pickTime) {
        val t0 = time?.let(LocalTime::parse) ?: LocalTime.now().plusHours(1).withMinute(0)
        val st = rememberTimePickerState(t0.hour, t0.minute, h24)
        AlertDialog(onDismissRequest = { pickTime = false },
            confirmButton = { TextButton({ time = D.pad(st.hour, st.minute); pickTime = false }) { Text("OK") } },
            dismissButton = { TextButton({ pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(st) })
    }
}

@Composable private fun Label(t: String) {
    Text(t.uppercase(), style = Ty.section, color = LocalGlass.current.fg2, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun SheetField(
    v: String, onChange: (String) -> Unit, hint: String, style: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean = false, minLines: Int = 1, ime: ImeAction = ImeAction.Default, onDone: () -> Unit = {},
) {
    val g = LocalGlass.current
    TextField(
        v, onChange, Modifier.fillMaxWidth().padding(vertical = 2.dp), textStyle = style.copy(color = g.fg),
        placeholder = { Text(hint, color = g.fg3, style = style) }, singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ime), keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = g.glass.copy(alpha = .3f), unfocusedContainerColor = g.glass.copy(alpha = .2f),
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = g.accent),
    )
}
