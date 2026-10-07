package com.shlok.taskcheck.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shlok.taskcheck.domain.D
import com.shlok.taskcheck.domain.NlParser
import com.shlok.taskcheck.ui.theme.*

/** Type "Call mom tomorrow 6pm !high #family" — chips show what was understood, live. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAdd(h24: Boolean, onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { if (text.isBlank()) null else NlParser.parse(text) }
    fun submit() { if (text.isNotBlank()) { onSubmit(text); text = "" } }

    GlassCard(modifier.fillMaxWidth().animateContentSize(), strong = true) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Add a task…  try “gym tomorrow 7am !high”", color = g.fg3, style = Ty.callout, maxLines = 1) },
                textStyle = Ty.body, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = g.fg, unfocusedTextColor = g.fg, cursorColor = g.accent),
            )
            AnimatedVisibility(text.isNotBlank(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = { submit() }) { Icon(Icons.Rounded.ArrowUpward, "Add", tint = Color.White, modifier = Modifier.size(34.dp).glassAccent()) }
            }
        }
        AnimatedVisibility(parsed != null && (parsed.dueDate != null || parsed.dueTime != null || parsed.priority != null || parsed.tags.isNotEmpty() || parsed.listName != null)) {
            FlowRow(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                parsed?.let { p ->
                    p.dueDate?.let { ParsedChip("📅 ${D.relative(it)}", g.accent) }
                    p.dueTime?.let { ParsedChip("⏰ ${D.time(it, h24)}", g.accent) }
                    p.priority?.let { ParsedChip("⚑ ${PriorityNames[it]}", PriorityColors[it]) }
                    p.listName?.let { ParsedChip("@$it", g.fg2) }
                    p.tags.forEach { ParsedChip("#$it", g.accent) }
                }
            }
        }
    }
}

@Composable private fun ParsedChip(t: String, c: Color) {
    Text(t, style = Ty.caption, color = c, modifier = Modifier.background(c.copy(alpha = .15f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable private fun Modifier.glassAccent(): Modifier {
    val a = LocalGlass.current.accent
    return this.background(a, RoundedCornerShape(50)).padding(6.dp)
}
