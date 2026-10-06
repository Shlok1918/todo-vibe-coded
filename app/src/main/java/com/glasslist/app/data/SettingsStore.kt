package com.glasslist.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

private val Context.ds by preferencesDataStore("settings")

object SK {
    val theme = intPreferencesKey("theme")            // 0 system · 1 light · 2 dark
    val accent = intPreferencesKey("accent")
    val wallpaper = intPreferencesKey("wallpaper")
    val glass = floatPreferencesKey("glass")
    val reduceMotion = booleanPreferencesKey("reduceMotion")
    val textScale = floatPreferencesKey("textScale")
    val h24 = booleanPreferencesKey("h24")
    val monday = booleanPreferencesKey("monday")
    val defPriority = intPreferencesKey("defPriority")
    val notif = booleanPreferencesKey("notif")
    val offset = intPreferencesKey("offset")
    val sound = booleanPreferencesKey("sound")
    val quiet = booleanPreferencesKey("quiet")
    val quietStart = stringPreferencesKey("quietStart")
    val quietEnd = stringPreferencesKey("quietEnd")
    val plan = booleanPreferencesKey("plan")
    val planAt = stringPreferencesKey("planAt")
    val review = booleanPreferencesKey("review")
    val reviewAt = stringPreferencesKey("reviewAt")
    val onboarded = booleanPreferencesKey("onboarded")
}

data class Settings(
    val theme: Int = 0, val accent: Int = 0, val wallpaper: Int = 0, val glass: Float = 1f,
    val reduceMotion: Boolean = false, val textScale: Float = 1f, val h24: Boolean = false,
    val monday: Boolean = true, val defPriority: Int = 0, val notif: Boolean = true,
    val offset: Int = 0, val sound: Boolean = true, val quiet: Boolean = false,
    val quietStart: String = "22:00", val quietEnd: String = "07:00",
    val plan: Boolean = true, val planAt: String = "08:00",
    val review: Boolean = false, val reviewAt: String = "20:00", val onboarded: Boolean = false,
)

class SettingsStore(private val ctx: Context) {
    val flow: Flow<Settings> = ctx.ds.data.map { p ->
        val d = Settings()
        Settings(
            p[SK.theme] ?: d.theme, p[SK.accent] ?: d.accent, p[SK.wallpaper] ?: d.wallpaper, p[SK.glass] ?: d.glass,
            p[SK.reduceMotion] ?: d.reduceMotion, p[SK.textScale] ?: d.textScale, p[SK.h24] ?: d.h24,
            p[SK.monday] ?: d.monday, p[SK.defPriority] ?: d.defPriority, p[SK.notif] ?: d.notif,
            p[SK.offset] ?: d.offset, p[SK.sound] ?: d.sound, p[SK.quiet] ?: d.quiet,
            p[SK.quietStart] ?: d.quietStart, p[SK.quietEnd] ?: d.quietEnd,
            p[SK.plan] ?: d.plan, p[SK.planAt] ?: d.planAt, p[SK.review] ?: d.review,
            p[SK.reviewAt] ?: d.reviewAt, p[SK.onboarded] ?: d.onboarded,
        )
    }
    suspend fun <T> set(key: Preferences.Key<T>, v: T) { ctx.ds.edit { it[key] = v } }
    suspend fun current(): Settings = flow.first()
}
