package com.glasslist.app.ui

import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glasslist.app.GlasslistApp
import com.glasslist.app.data.*
import com.glasslist.app.domain.NlParser
import com.glasslist.app.notify.Notifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.util.UUID

data class Snack(val msg: String, val action: String? = null, val onAction: (() -> Unit)? = null)

data class FocusState(
    val taskId: String? = null, val onBreak: Boolean = false, val running: Boolean = false,
    val total: Int = WORK, val remaining: Int = WORK, val sessions: Int = 0,
) { companion object { const val WORK = 25 * 60; const val BREAK = 5 * 60 } }

class MainViewModel(private val app: GlasslistApp) : ViewModel() {
    private val c = app.container
    val tasks = c.repo.tasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val lists = c.repo.lists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<Settings?> = c.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _snacks = Channel<Snack>(Channel.BUFFERED)
    val snacks = _snacks.receiveAsFlow()
    private val _focus = MutableStateFlow(FocusState())
    val focus = _focus.asStateFlow()
    private var ticker: Job? = null
    private var endAt = 0L

    init {
        viewModelScope.launch { c.scheduler.rescheduleAll(c.repo.allTasks(), c.settings.current()) }
    }

    private fun snack(m: String, a: String? = null, f: (() -> Unit)? = null) { _snacks.trySend(Snack(m, a, f)) }
    private fun id() = UUID.randomUUID().toString()

    fun blank(date: String? = null): Task {
        val s = settings.value ?: Settings()
        return Task(id = id(), title = "", createdAt = System.currentTimeMillis(), sortOrder = System.currentTimeMillis(),
            dueDate = date, priority = s.defPriority)
    }

    fun quickAdd(text: String) = viewModelScope.launch {
        val s = settings.value ?: Settings()
        val p = NlParser.parse(text)
        val listId = p.listName?.let { n -> lists.value.firstOrNull { it.name.equals(n, true) }?.id }
        val rems = if (s.notif && p.dueDate != null) listOf(Reminder(id(), s.offset)) else emptyList()
        c.repo.save(blank(p.dueDate).copy(
            title = p.title, dueTime = p.dueTime, priority = p.priority ?: s.defPriority, tags = p.tags,
            listId = listId, starred = p.starred, reminders = rems))
    }

    fun save(t: Task) = viewModelScope.launch { c.repo.save(t.copy(title = t.title.trim().ifBlank { "Untitled" })) }

    fun toggle(t: Task) = viewModelScope.launch {
        val next = c.repo.toggle(t)
        if (!t.completed) snack("Completed “${t.title.take(32)}”", "Undo") {
            viewModelScope.launch { c.repo.save(t.copy(completed = false, completedAt = null)); next?.let { c.repo.delete(it) } }
        }
    }

    fun toggleStar(t: Task) = viewModelScope.launch { c.repo.save(t.copy(starred = !t.starred)) }

    fun delete(t: Task) = viewModelScope.launch {
        c.repo.delete(t)
        snack("Task deleted", "Undo") { viewModelScope.launch { c.repo.save(t) } }
    }

    fun clearCompleted() = viewModelScope.launch { tasks.value.filter { it.completed }.forEach { c.repo.delete(it) } }

    fun saveList(l: TaskList) = viewModelScope.launch { c.repo.saveList(l) }
    fun deleteList(l: TaskList) = viewModelScope.launch { c.repo.deleteList(l) }
    fun newList(name: String, emoji: String, color: Int) = saveList(TaskList(id(), name, emoji, color, System.currentTimeMillis()))

    // ---- settings ---------------------------------------------------------------------------
    fun <T> set(key: Preferences.Key<T>, v: T) = viewModelScope.launch {
        c.settings.set(key, v)
        c.scheduler.scheduleDigests(c.settings.current())
    }

    fun finishOnboarding() = viewModelScope.launch {
        if (tasks.value.isEmpty()) {
            val now = System.currentTimeMillis(); val today = LocalDate.now().toString()
            c.repo.saveAll(listOf(
                Task(id(), "Welcome to Glasslist 👋", "Swipe right to complete, left to delete.", createdAt = now, sortOrder = now),
                Task(id(), "Try quick add", "Type: Call mom tomorrow 6pm !high #family", createdAt = now, sortOrder = now + 1, dueDate = today, priority = 1, tags = listOf("tip")),
                Task(id(), "Explore themes & wallpapers", "", createdAt = now, sortOrder = now + 2, starred = true,
                    subtasks = listOf(Subtask(id(), "Pick an accent"), Subtask(id(), "Try light theme"))),
            ))
        }
        c.settings.set(SK.onboarded, true)
        c.scheduler.scheduleDigests(c.settings.current())
    }

    fun testNotification() = Notifier.showInfo(app, 9999, Notifier.CH_DIGEST, "Glasslist", "Notifications are working ✨")
    fun canExact() = c.scheduler.canExact()

    fun reset() = viewModelScope.launch { c.repo.clearAll(); snack("All data deleted") }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { val j = c.repo.exportJson(); withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri)?.use { it.write(j.toByteArray()) } } }
            .onSuccess { snack("Backup saved") }.onFailure { snack("Export failed") }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching { val j = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: "" }; c.repo.importJson(j) }
            .onSuccess { snack("Backup restored") }.onFailure { snack("Invalid backup file") }
    }

    // ---- focus (pomodoro) -----------------------------------------------------------------------
    fun focusOn(taskId: String?) { _focus.update { it.copy(taskId = taskId) } }

    fun focusToggle() {
        val f = _focus.value
        if (f.running) {
            ticker?.cancel(); c.scheduler.cancelFocus()
            _focus.update { it.copy(running = false) }
        } else {
            endAt = System.currentTimeMillis() + f.remaining * 1000L
            c.scheduler.scheduleFocus(endAt, if (f.onBreak) "Back to it!" else "Nice work — take a 5 minute break.")
            _focus.update { it.copy(running = true) }
            ticker = viewModelScope.launch {
                while (true) {
                    val left = ((endAt - System.currentTimeMillis()) / 1000).toInt()
                    if (left <= 0) { phaseDone(); break }
                    _focus.update { it.copy(remaining = left) }
                    delay(250)
                }
            }
        }
    }

    private fun phaseDone() {
        c.scheduler.cancelFocus()
        Notifier.showInfo(app, 8003, Notifier.CH_FOCUS, if (_focus.value.onBreak) "Break over" else "Focus session complete 🎉",
            if (_focus.value.onBreak) "Ready for another round?" else "Time for a 5 minute break.")
        _focus.update {
            val br = !it.onBreak; val tot = if (br) FocusState.BREAK else FocusState.WORK
            it.copy(onBreak = br, running = false, total = tot, remaining = tot, sessions = if (br) it.sessions + 1 else it.sessions)
        }
    }

    fun focusReset() {
        ticker?.cancel(); c.scheduler.cancelFocus()
        _focus.update { FocusState(taskId = it.taskId, sessions = it.sessions) }
    }
}
