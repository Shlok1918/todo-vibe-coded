package com.shlok.taskcheck

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.shlok.taskcheck.data.AppDatabase
import com.shlok.taskcheck.data.SettingsStore
import com.shlok.taskcheck.data.TaskRepository
import com.shlok.taskcheck.notify.AlarmScheduler
import com.shlok.taskcheck.notify.Notifier

/** Manual DI container: tiny, explicit, no codegen. */
class AppContainer(ctx: Context) {
    val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "glasslist.db").build()
    val settings = SettingsStore(ctx)
    val scheduler = AlarmScheduler(ctx)
    val repo = TaskRepository(db, scheduler)
}

class GlasslistApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as GlasslistApp).container
