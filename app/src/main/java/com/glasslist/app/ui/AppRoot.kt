package com.shlok.taskcheck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shlok.taskcheck.data.Settings
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.screens.*
import com.shlok.taskcheck.ui.theme.*

private enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Rounded.Today), Upcoming("Upcoming", Icons.Rounded.Upcoming), Calendar("Calendar", Icons.Rounded.CalendarMonth),
    All("All", Icons.Rounded.FormatListBulleted), Insights("Insights", Icons.Rounded.Insights),
}

@Composable
fun AppRoot(vm: MainViewModel, openTaskId: String?, onOpenHandled: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return Box(Modifier.fillMaxSize().background(Color(0xFF0F0F23)))   // wait for DataStore
    GlasslistTheme(s) {
        Box(Modifier.fillMaxSize()) {
            GlassBackground()
            if (!s.onboarded) Onboarding(vm::finishOnboarding) else Main(vm, s, openTaskId, onOpenHandled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Main(vm: MainViewModel, s: Settings, openTaskId: String?, onOpenHandled: () -> Unit) {
    val g = LocalGlass.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showFocus by rememberSaveable { mutableStateOf(false) }
    var sheetId by rememberSaveable { mutableStateOf<String?>(null) }    // task id or "new"
    var newDate by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(openTaskId) { if (openTaskId != null) { sheetId = openTaskId; onOpenHandled() } }
    LaunchedEffect(Unit) {
        vm.snacks.collect { sn ->
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(sn.msg, sn.action, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) sn.onAction?.invoke()
        }
    }
    BackHandler(showFocus) { showFocus = false }
    BackHandler(showSettings && !showFocus) { showSettings = false }

    val ctx = TaskCtx(lists, s.h24, vm::toggle, { sheetId = it.id }, vm::delete, vm::toggleStar)

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            AnimatedContent(tab, transitionSpec = { fadeIn(tween(240)) + scaleIn(initialScale = .98f) togetherWith fadeOut(tween(120)) }, label = "tab") { t ->
                when (t) {
                    Tab.Today -> TodayScreen(tasks, ctx, vm::quickAdd, { showSettings = true }, { showFocus = true })
                    Tab.Upcoming -> UpcomingScreen(tasks, ctx)
                    Tab.Calendar -> CalendarScreen(tasks, ctx, s.monday) { newDate = it; sheetId = "new" }
                    Tab.All -> AllScreen(tasks, ctx, vm::clearCompleted, vm::newList, vm::saveList, vm::deleteList)
                    Tab.Insights -> InsightsScreen(tasks)
                }
            }
        }

        // floating nav + FAB
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp), horizontalAlignment = Alignment.End) {
            Box(Modifier.padding(end = 20.dp, bottom = 12.dp).size(58.dp).clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(g.accent, g.accent.copy(alpha = .7f))))
                .bounceClick { newDate = if (tab == Tab.Today) com.shlok.taskcheck.domain.D.today().toString() else null; sheetId = "new" }, Alignment.Center) {
                Icon(Icons.Rounded.Add, "New task", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Row(Modifier.align(Alignment.CenterHorizontally).glass(RoundedCornerShape(32.dp), strong = true).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Tab.entries.forEach { NavItem(it, it == tab) { tab = it } }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp)) {
            Snackbar(it, shape = RoundedCornerShape(16.dp), containerColor = g.sheet, contentColor = g.fg, actionColor = g.accent)
        }

        // task sheet
        sheetId?.let { id ->
            val isNew = id == "new"
            val existing = tasks.firstOrNull { it.id == id }
            if (isNew || existing != null) key(id) {
                TaskSheet(
                    initial = existing ?: vm.blank(newDate), isNew = isNew, lists = lists, h24 = s.h24, defaultOffset = s.offset, notifOn = s.notif,
                    onSave = { vm.save(it); sheetId = null }, onDelete = { existing?.let(vm::delete); sheetId = null },
                    onFocus = { vm.focusOn(id); sheetId = null; showFocus = true }, onDismiss = { sheetId = null },
                )
            }
        }

        Overlay(showSettings) { SettingsScreen(vm, s) { showSettings = false } }
        Overlay(showFocus) {
            val t = tasks.firstOrNull { it.id == focus.taskId }
            FocusScreen(focus, t?.title, vm::focusToggle, vm::focusReset, t?.takeIf { !it.completed }?.let { { vm.toggle(it); showFocus = false } }) { showFocus = false }
        }
    }
}

@Composable
private fun Overlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = slideInVertically(tween(380)) { it / 3 } + fadeIn(tween(300)), exit = slideOutVertically(tween(280)) { it / 3 } + fadeOut(tween(220))) {
        Box(Modifier.fillMaxSize()) { GlassBackground(); Box(Modifier.fillMaxSize().statusBarsPadding()) { content() } }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    val bg by androidx.compose.animation.animateColorAsState(if (selected) g.accent.copy(alpha = .22f) else Color.Transparent, label = "navbg")
    Row(Modifier.clip(RoundedCornerShape(26.dp)).background(bg).bounceClick(onClick = onClick).padding(horizontal = if (selected) 16.dp else 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(tab.icon, tab.label, tint = if (selected) g.accent else g.fg2, modifier = Modifier.size(22.dp))
        AnimatedVisibility(selected, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Text(tab.label, style = Ty.callout, color = g.accent, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
        }
    }
}
