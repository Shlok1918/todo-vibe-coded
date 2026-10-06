package com.glasslist.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import com.glasslist.app.data.SK
import com.glasslist.app.data.Settings
import com.glasslist.app.ui.MainViewModel
import com.glasslist.app.ui.components.*
import com.glasslist.app.ui.theme.*
import com.glasslist.app.ui.theme.Wallpapers
import com.glasslist.app.domain.D
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, s: Settings, onBack: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.set(SK.notif, it) }
    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportBackup) }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importBackup) }
    var confirmReset by remember { mutableStateOf(false) }
    var timeFor by remember { mutableStateOf<Pair<String, Preferences.Key<String>>?>(null) }

    fun <T> put(k: Preferences.Key<T>, v: T) { vm.set(k, v) }
    fun toggleNotif(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else put(SK.notif, on)
    }

    ScreenList {
        item { ScreenHeader("Settings") { GlassIconButton(Icons.Rounded.Close, "Close", onBack) } }

        item { Group("Appearance") {
            Label("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, n -> Chip(n, s.theme == i, { put(SK.theme, i) }) }
            }
            Label("Accent")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accents.forEachIndexed { i, a -> Swatch(a.color, s.accent == i) { put(SK.accent, i) } }
            }
            Label("Wallpaper")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Wallpapers.forEachIndexed { i, w ->
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(w.orbs[0].copy(alpha = .9f), w.canvas, w.orbs[1].copy(alpha = .8f))))
                        .bounceClick { put(SK.wallpaper, i) }, Alignment.Center) {
                        if (s.wallpaper == i) Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
            Label("Glass intensity")
            Slider(s.glass, { put(SK.glass, it) }, valueRange = 0.4f..1.6f)
            Label("Text size")
            Slider(s.textScale, { put(SK.textScale, it) }, valueRange = 0.85f..1.3f)
            ToggleRow("Reduce motion", "Stops the drifting wallpaper", s.reduceMotion) { put(SK.reduceMotion, it) }
        } }

        item { Group("Preferences") {
            Label("Time format")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("12-hour", !s.h24, { put(SK.h24, false) }); Chip("24-hour", s.h24, { put(SK.h24, true) }) }
            Label("Week starts on")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("Monday", s.monday, { put(SK.monday, true) }); Chip("Sunday", !s.monday, { put(SK.monday, false) }) }
            Label("Default priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PriorityNames.forEachIndexed { i, n -> Chip(n, s.defPriority == i, { put(SK.defPriority, i) }) } }
        } }

        item { Group("Reminders") {
            ToggleRow("Notifications", "Task reminders & digests", s.notif) { toggleNotif(it) }
            if (Build.VERSION.SDK_INT >= 31 && !vm.canExact()) {
                TextButton({ ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) }) {
                    Text("Allow exact alarms for on-time reminders →", color = Danger)
                }
            }
            ToggleRow("Sound & vibration", null, s.sound) { put(SK.sound, it) }
            Label("Default reminder for new dated tasks")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 15, 30, 60, 1440).forEach { Chip(D.offsetLabel(it), s.offset == it, { put(SK.offset, it) }) }
            }
            ToggleRow("Daily plan", "Morning summary of what's due", s.plan) { put(SK.plan, it) }
            if (s.plan) TimeRow("Plan time", s.planAt) { timeFor = "Plan time" to SK.planAt }
            ToggleRow("Evening review", "How the day went", s.review) { put(SK.review, it) }
            if (s.review) TimeRow("Review time", s.reviewAt) { timeFor = "Review time" to SK.reviewAt }
            ToggleRow("Quiet hours", "Reminders arrive silently", s.quiet) { put(SK.quiet, it) }
            if (s.quiet) { TimeRow("From", s.quietStart) { timeFor = "From" to SK.quietStart }; TimeRow("Until", s.quietEnd) { timeFor = "Until" to SK.quietEnd } }
            TextButton(vm::testNotification) { Text("Send test notification") }
        } }

        item { Group("Data") {
            Text("Everything is stored on this device. No account, no servers.", style = Ty.callout, color = g.fg2)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Export backup", false, { exportL.launch("glasslist-backup.json") }, leading = "⬆")
                Chip("Import backup", false, { importL.launch(arrayOf("application/json", "*/*")) }, leading = "⬇")
            }
            TextButton({ confirmReset = true }) { Text("Delete all data", color = Danger) }
        } }
        item { Text("Glasslist 1.0 · made with care", style = Ty.caption, color = g.fg3, modifier = Modifier.padding(24.dp)) }
    }

    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Delete everything?") },
        text = { Text("All tasks and lists will be removed permanently.") },
        confirmButton = { TextButton({ vm.reset(); confirmReset = false }) { Text("Delete", color = Danger) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } })

    timeFor?.let { (label, key) ->
        val cur = LocalTime.parse(when (key) { SK.planAt -> s.planAt; SK.reviewAt -> s.reviewAt; SK.quietStart -> s.quietStart; else -> s.quietEnd })
        val st = rememberTimePickerState(cur.hour, cur.minute, s.h24)
        AlertDialog(onDismissRequest = { timeFor = null }, title = { Text(label) }, text = { TimePicker(st) },
            confirmButton = { TextButton({ put(key, D.pad(st.hour, st.minute)); timeFor = null }) { Text("OK") } },
            dismissButton = { TextButton({ timeFor = null }) { Text("Cancel") } })
    }
}

@Composable private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 24) { Column(Modifier.padding(16.dp), content = content) }
    }
}

@Composable private fun Label(t: String) = Text(t, style = Ty.caption, color = LocalGlass.current.fg2, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))

@Composable private fun ToggleRow(title: String, sub: String?, v: Boolean, onChange: (Boolean) -> Unit) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = Ty.body, color = g.fg); if (sub != null) Text(sub, style = Ty.caption, color = g.fg2) }
        Switch(v, onChange)
    }
}

@Composable private fun TimeRow(title: String, v: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Ty.body, color = g.fg2, modifier = Modifier.weight(1f))
        Chip(v, false, onClick)
    }
}
