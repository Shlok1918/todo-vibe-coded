package com.glasslist.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder") fun observeAll(): Flow<List<Task>>
    @Query("SELECT * FROM tasks") suspend fun all(): List<Task>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun get(id: String): Task?
    @Upsert suspend fun upsert(t: Task)
    @Upsert suspend fun upsertAll(t: List<Task>)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM tasks") suspend fun clear()
}

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY sortOrder") fun observeAll(): Flow<List<TaskList>>
    @Query("SELECT * FROM lists") suspend fun all(): List<TaskList>
    @Upsert suspend fun upsert(l: TaskList)
    @Upsert suspend fun upsertAll(l: List<TaskList>)
    @Query("DELETE FROM lists WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM lists") suspend fun clear()
}

@Database(entities = [Task::class, TaskList::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun lists(): ListDao
}
