package com.glasslist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glasslist.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

/** Slowly drifting colour orbs on the wallpaper canvas — gives the glass something to refract. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val motion = LocalMotion.current
    val t = rememberInfiniteTransition(label = "orbs")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "p")
    val phase = if (motion) p else 0f
    val base = listOf(Offset(.2f, .3f), Offset(.85f, .18f), Offset(.6f, .82f), Offset(.1f, .72f))
    Canvas(modifier.fillMaxSize()) {
        drawRect(g.canvas)
        val r = size.height * 0.5f
        g.orbs.forEachIndexed { i, c ->
            val a = 2 * PI * (phase + i * 0.25)
            val ctr = Offset(
                size.width * (base[i].x + 0.07f * sin(a).toFloat()),
                size.height * (base[i].y + 0.05f * sin(a * 2 + i).toFloat()),
            )
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), ctr, r), r, ctr)
        }
    }
}

/** Frosted-glass surface: translucent gradient fill + lit edge. Intensity follows the user setting. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp), strong: Boolean = false): Modifier = composed {
    val g = LocalGlass.current
    val k = LocalGlassK.current
    val base = if (strong) g.glassStrong else g.glass
    val a = (base.alpha * (0.45f + 0.55f * k)).coerceIn(0f, 0.95f)
    val fill = base.copy(alpha = a)
    this.clip(shape)
        .background(Brush.linearGradient(listOf(fill.copy(alpha = (a * 1.25f).coerceAtMost(0.95f)), fill)), shape)
        .border(1.dp, Brush.linearGradient(listOf(g.highlight, g.edge, g.highlight.copy(alpha = g.highlight.alpha * .4f))), shape)
}

/** Spring-press feedback + click. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }.clickable(src, null, enabled = enabled, onClick = onClick)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, strong: Boolean = false, radius: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass(RoundedCornerShape(radius.dp), strong), content = content)
}

@Composable
fun GlassIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = LocalGlass.current.fg) {
    Box(modifier.size(42.dp).glass(CircleShape).bounceClick(onClick = onClick), Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalGlass.current.accent, leading: String? = null, onLongClick: (() -> Unit)? = null) {
    val g = LocalGlass.current
    val bg by animateColorAsState(if (selected) color.copy(alpha = .25f) else g.glass.copy(alpha = .35f), label = "chipbg")
    val fg by animateColorAsState(if (selected) color else g.fg2, label = "chipfg")
    Row(
        modifier.clip(RoundedCornerShape(50)).background(bg)
            .border(1.dp, if (selected) color.copy(.6f) else g.highlight.copy(alpha = g.highlight.alpha * .5f), RoundedCornerShape(50))
            .then(if (onLongClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier.bounceClick(onClick = onClick))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) Text(leading, Modifier.padding(end = 6.dp))
        Text(label, style = Ty.callout, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val g = LocalGlass.current
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, style = Ty.callout, color = g.fg2)
            Text(title, style = Ty.largeTitle, color = g.fg)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = trailing)
    }
}

@Composable
fun SectionLabel(text: String, color: Color = LocalGlass.current.fg2, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Ty.section, color = color, modifier = modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp))
}

@Composable
fun EmptyState(icon: ImageVector, title: String, sub: String, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).glass(CircleShape, strong = true), Alignment.Center) {
            Icon(icon, null, tint = g.accent, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = Ty.headline, color = g.fg, textAlign = TextAlign.Center)
        Text(sub, style = Ty.callout, color = g.fg2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ProgressRing(done: Int, total: Int, size: Int = 76) {
    val g = LocalGlass.current
    val frac by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, tween(700, easing = FastOutSlowInEasing), label = "ring")
    Box(Modifier.size(size.dp), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            val st = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(g.fg.copy(alpha = .12f), 0f, 360f, false, style = st)
            drawArc(Brush.sweepGradient(listOf(g.accent.copy(.6f), g.accent)), -90f, 360f * frac, false, style = st)
        }
        Text("$done/$total", style = Ty.callout, color = g.fg, fontWeight = FontWeight.SemiBold)
    }
}

/** Circle check with a path-drawn tick. */
@Composable
fun GlassCheck(checked: Boolean, color: Color, onClick: () -> Unit) {
    val prog by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "tick")
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(220), label = "fill")
    Box(Modifier.size(44.dp).bounceClick(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(26.dp)) {
            drawCircle(color.copy(alpha = fill), radius = size.minDimension / 2 - 1.dp.toPx())
            drawCircle(color, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            if (prog > 0f) {
                val path = Path().apply {
                    moveTo(size.width * .28f, size.height * .52f); lineTo(size.width * .43f, size.height * .67f); lineTo(size.width * .74f, size.height * .34f)
                }
                val pm = PathMeasure().apply { setPath(path, false) }
                val out = Path(); pm.getSegment(0f, pm.length * prog.coerceIn(0f, 1f), out, true)
                drawPath(out, Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val g = LocalGlass.current
    Row(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(g.accent, g.accent.copy(alpha = .75f))))
            .bounceClick(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        Arrangement.Center, Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color.White, style = Ty.headline)
    }
}
