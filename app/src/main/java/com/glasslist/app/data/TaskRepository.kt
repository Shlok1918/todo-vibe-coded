package com.shlok.taskcheck.data

import com.shlok.taskcheck.domain.Recurrence
import com.shlok.taskcheck.notify.AlarmScheduler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepository(private val db: AppDatabase, private val scheduler: AlarmScheduler) {
    val tasks = db.tasks().observeAll()
    val lists = db.lists().observeAll()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun allTasks() = db.tasks().all()
    suspend fun get(id: String) = db.tasks().get(id)

    suspend fun save(t: Task) { db.tasks().upsert(t); scheduler.schedule(t) }
    suspend fun saveAll(ts: List<Task>) { db.tasks().upsertAll(ts); ts.forEach(scheduler::schedule) }
    suspend fun delete(t: Task) { db.tasks().delete(t.id); scheduler.cancel(t) }

    /** Toggles completion; for repeating tasks spawns the next occurrence. Returns the spawned task (for undo). */
    suspend fun toggle(t: Task): Task? {
        val completing = !t.completed
        val done = t.copy(completed = completing, completedAt = if (completing) System.currentTimeMillis() else null)
        save(done)
        if (completing && t.repeat != null) Recurrence.next(t)?.let { save(it); return it }
        return null
    }

    suspend fun saveList(l: TaskList) = db.lists().upsert(l)
    suspend fun deleteList(l: TaskList) {
        db.lists().delete(l.id)
        db.tasks().all().filter { it.listId == l.id }.forEach { db.tasks().upsert(it.copy(listId = null)) }
    }

    suspend fun clearAll() {
        allTasks().forEach(scheduler::cancel)
        db.tasks().clear(); db.lists().clear()
    }

    suspend fun exportJson(): String = json.encodeToString(Backup(tasks = db.tasks().all(), lists = db.lists().all()))
    suspend fun importJson(s: String) {
        val b = json.decodeFromString<Backup>(s)
        clearAll()
        db.lists().upsertAll(b.lists); saveAll(b.tasks)
    }
}
