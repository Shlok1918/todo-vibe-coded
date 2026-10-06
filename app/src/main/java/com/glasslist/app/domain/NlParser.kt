package com.glasslist.app.domain

import java.time.LocalDate
import java.time.LocalTime

data class Parsed(
    val title: String, val dueDate: String? = null, val dueTime: String? = null,
    val priority: Int? = null, val tags: List<String> = emptyList(),
    val listName: String? = null, val starred: Boolean = false,
)

/** Offline natural-language quick-add: "Call mom tomorrow 6pm !high #family @home" */
object NlParser {
    private val IC = RegexOption.IGNORE_CASE
    private val DAYS = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    private val MONTHS = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
    private val dayRx = DAYS.joinToString("|") { "$it|${it.take(3)}" }
    private val monRx = MONTHS.joinToString("|") { if (it == "may") it else "$it|${it.take(3)}" }
    private fun dayIdx(s: String) = DAYS.indexOfFirst { it.startsWith(s.lowercase().take(3)) }

    private fun parseDate(text: String): Pair<String?, String> {
        fun cut(m: MatchResult) = text.replace(m.value, " ").trim()
        val today = LocalDate.now()
        Regex("""\b(\d{4}-\d{2}-\d{2})\b""").find(text)?.let { return it.groupValues[1] to cut(it) }
        Regex("""\btoday\b""", IC).find(text)?.let { return today.toString() to cut(it) }
        Regex("""\btomorrow\b""", IC).find(text)?.let { return today.plusDays(1).toString() to cut(it) }
        Regex("""\bnext week\b""", IC).find(text)?.let { return today.plusDays(7).toString() to cut(it) }
        Regex("""\bnext month\b""", IC).find(text)?.let { return today.plusMonths(1).toString() to cut(it) }
        Regex("""\bin (\d+) days?\b""", IC).find(text)?.let { return today.plusDays(it.groupValues[1].toLong()).toString() to cut(it) }
        Regex("""\bnext ($dayRx)\b""", IC).find(text)?.let {
            var d = today.plusDays(7); val t = dayIdx(it.groupValues[1])
            while (d.dayOfWeek.value % 7 != t) d = d.plusDays(1)
            return d.toString() to cut(it)
        }
        Regex("""\b($dayRx)\b""", IC).find(text)?.let {
            var d = today.plusDays(1); val t = dayIdx(it.groupValues[1])
            while (d.dayOfWeek.value % 7 != t) d = d.plusDays(1)
            return d.toString() to cut(it)
        }
        Regex("""\b($monRx)\s+(\d{1,2})\b""", IC).find(text)?.let {
            val m = MONTHS.indexOfFirst { n -> n.startsWith(it.groupValues[1].lowercase().take(3)) } + 1
            runCatching {
                var d = LocalDate.of(today.year, m, it.groupValues[2].toInt())
                if (d.isBefore(today)) d = d.plusYears(1)
                return d.toString() to cut(it)
            }
        }
        return null to text
    }

    private fun parseTime(text: String): Pair<String?, String> {
        fun cut(m: MatchResult) = text.replace(m.value, " ").trim()
        fun to24(h: Int, pm: Boolean) = if (pm && h != 12) h + 12 else if (!pm && h == 12) 0 else h
        Regex("""\b(\d{1,2}):(\d{2})\s*(am|pm)\b""", IC).find(text)?.let {
            val h = to24(it.groupValues[1].toInt(), it.groupValues[3].equals("pm", true))
            return D.pad(h, it.groupValues[2].toInt()) to cut(it)
        }
        Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(text)?.let { return D.pad(it.groupValues[1].toInt(), it.groupValues[2].toInt()) to cut(it) }
        Regex("""\bnoon\b""", IC).find(text)?.let { return "12:00" to cut(it) }
        Regex("""\bmidnight\b""", IC).find(text)?.let { return "00:00" to cut(it) }
        Regex("""\b(\d{1,2})\s*(am|pm)\b""", IC).find(text)?.let {
            return D.pad(to24(it.groupValues[1].toInt(), it.groupValues[2].equals("pm", true)), 0) to cut(it)
        }
        return null to text
    }

    fun parse(input: String): Parsed {
        var s = " ${input.trim()} "
        var starred = false
        if (s.trimStart().startsWith("*")) { starred = true; s = s.replaceFirst("*", " ") }
        val tags = Regex("""#([\w-]+)""").findAll(s).map { it.groupValues[1] }.toList()
        s = s.replace(Regex("""#[\w-]+"""), " ")
        val list = Regex("""@([\w-]+)""").find(s)?.groupValues?.get(1)
        s = s.replace(Regex("""@[\w-]+"""), " ")

        var prio: Int? = null
        Regex("""\s!(high|h|medium|med|m|low|l)(?=\s)""", IC).find(s)?.let {
            prio = when (it.groupValues[1].lowercase().first()) { 'h' -> 3; 'm' -> 2; else -> 1 }
            s = s.replace(it.value, " ")
        } ?: Regex("""\s(!{1,3})(?=\s)""").find(s)?.let {
            prio = it.groupValues[1].length; s = s.replace(it.value, " ")
        }
        val (date, a) = parseDate(s.trim())
        val (time, b) = parseTime(a.trim())
        val title = b.trim().replace(Regex("\\s+"), " ").ifBlank { input.trim() }
        return Parsed(title, date, time, prio, tags, list, starred)
    }
}
