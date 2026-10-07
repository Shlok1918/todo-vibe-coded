package com.shlok.taskcheck.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Onboarding(onFinish: () -> Unit) {
    val g = LocalGlass.current
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onFinish() }
    val pages = listOf(
        Triple(Icons.Rounded.AutoAwesome, "Welcome to Glasslist", "A calm, glassy home for everything you need to do."),
        Triple(Icons.Rounded.Bolt, "Type it. It's planned.", "Write “Pay rent friday 9am !high #bills” and dates, times, priority and tags are filled in for you."),
        Triple(Icons.Rounded.NotificationsActive, "Reminders that actually ring", "Exact alarms with Done and Snooze right from the notification. Works offline, no account."),
    )
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val (icon, title, sub) = pages[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(120.dp).glass(CircleShape, strong = true), Alignment.Center) { Icon(icon, null, tint = g.accent, modifier = Modifier.size(56.dp)) }
                Spacer(Modifier.height(32.dp))
                Text(title, style = Ty.largeTitle, color = g.fg, textAlign = TextAlign.Center)
                Text(sub, style = Ty.body, color = g.fg2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            repeat(3) { Box(Modifier.size(if (pager.currentPage == it) 22.dp else 8.dp, 8.dp).background(if (pager.currentPage == it) g.accent else g.fg3, CircleShape)) }
        }
        val last = pager.currentPage == 2
        AccentButton(if (last) "Enable reminders" else "Continue", {
            if (!last) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            else if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS) else onFinish()
        }, Modifier.fillMaxWidth())
        TextButton(onFinish) { Text(if (last) "Not now" else "Skip", color = g.fg2) }
    }
}
