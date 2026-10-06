package com.glasslist.app.data

import androidx.room.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable data class Subtask(val id: String, val title: String, val done: Boolean = false)
@Serializable data class Reminder(val id: String, val offsetMinutes: Int = 0, val absoluteMillis: Long? = null)
/** freq: daily | weekly | monthly | yearly | custom. daysOfWeek: 0 = Sunday .. 6 = Saturday */
@Serializable data class Repeat(val freq: String = "daily", val interval: Int = 1, val daysOfWeek: List<Int> = emptyList(), val endDate: String? = null)

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val dueDate: String? = null,      // yyyy-MM-dd
    val dueTime: String? = null,      // HH:mm
    val priority: Int = 0,            // 0 none · 1 low · 2 medium · 3 high
    val listId: String? = null,
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val repeat: Repeat? = null,
    val starred: Boolean = false,
    val sortOrder: Long = 0,
)

@Serializable
@Entity(tableName = "lists")
data class TaskList(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "📁",
    val color: Int = 0xFF5B7CFD.toInt(),
    val sortOrder: Long = 0,
)

@Serializable data class Backup(val version: Int = 1, val tasks: List<Task>, val lists: List<TaskList>)

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    @TypeConverter fun tagsTo(v: List<String>): String = json.encodeToString(v)
    @TypeConverter fun tagsFrom(s: String): List<String> = json.decodeFromString(s)
    @TypeConverter fun subsTo(v: List<Subtask>): String = json.encodeToString(v)
    @TypeConverter fun subsFrom(s: String): List<Subtask> = json.decodeFromString(s)
    @TypeConverter fun remsTo(v: List<Reminder>): String = json.encodeToString(v)
    @TypeConverter fun remsFrom(s: String): List<Reminder> = json.decodeFromString(s)
    @TypeConverter fun repTo(v: Repeat?): String? = v?.let { json.encodeToString(it) }
    @TypeConverter fun repFrom(s: String?): Repeat? = s?.let { json.decodeFromString<Repeat>(it) }
}
