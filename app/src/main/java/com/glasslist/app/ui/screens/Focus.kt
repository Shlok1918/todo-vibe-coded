package com.shlok.taskcheck.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.taskcheck.ui.FocusState
import com.shlok.taskcheck.ui.components.*
import com.shlok.taskcheck.ui.theme.*

@Composable
fun FocusScreen(f: FocusState, taskTitle: String?, onToggle: () -> Unit, onReset: () -> Unit, onDoneTask: (() -> Unit)?, onClose: () -> Unit) {
    val g = LocalGlass.current
    val col = if (f.onBreak) Success else g.accent
    val frac by animateFloatAsState(1f - f.remaining.toFloat() / f.total, tween(300, easing = LinearEasing), label = "focus")
    val breathe = rememberInfiniteTransition(label = "breathe")
    val glow by breathe.animateFloat(0.85f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { GlassIconButton(Icons.Rounded.Close, "Close", onClose) }
        Spacer(Modifier.weight(1f))
        Text(if (f.onBreak) "BREAK TIME" else "FOCUS · SESSION ${f.sessions + 1}", style = Ty.section, color = col)
        Text(taskTitle ?: "Free focus", style = Ty.title, color = g.fg, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        Box(Modifier.size(280.dp).glass(androidx.compose.foundation.shape.CircleShape, strong = true), Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                val st = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                drawArc(g.fg.copy(alpha = .1f), 0f, 360f, false, style = st)
                drawArc(Brush.sweepGradient(listOf(col.copy(.5f), col)), -90f, 360f * frac, false, style = st)
                if (f.running) drawCircle(col.copy(alpha = .08f * glow), radius = size.minDimension / 2.4f * glow)
            }
            Text("%02d:%02d".format(f.remaining / 60, f.remaining % 60), color = g.fg, fontSize = 54.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.Replay, "Reset", onReset)
            AccentButton(if (f.running) "Pause" else "Start", onToggle, Modifier.width(150.dp), if (f.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow)
        }
        if (onDoneTask != null) {
            Spacer(Modifier.height(20.dp))
            Chip("Mark task done", false, onDoneTask, color = Success, leading = "✓")
        }
        Spacer(Modifier.weight(1.4f))
        Text("Timer keeps running with the screen off — you'll get a notification when it ends.", style = Ty.caption, color = g.fg3, textAlign = TextAlign.Center)
    }
}
