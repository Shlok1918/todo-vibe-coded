# Glasslist — native Android (Kotlin · Jetpack Compose)

Offline-first glassmorphism to-do app. No backend, no account.

## Run
1. Android Studio Ladybug (2024.2) or newer → **Open** this folder → let Gradle sync (JDK 17).
2. Run on a device/emulator (Android 8.0+ / API 26+).
3. On Android 12+, allow "exact alarms" from Settings → Reminders for on-the-minute alerts.

## Architecture
MVVM + Repository, manual DI (`AppContainer`), single-activity Compose.

    data/     Room (tasks, lists) · DataStore (settings) · TaskRepository · JSON backup
    domain/   NlParser (quick-add), Recurrence, date helpers
    notify/   AlarmScheduler (AlarmManager) · Notifier · AlarmReceiver · BootReceiver
    ui/       MainViewModel · AppRoot · theme/ (dual theme, wallpapers, accents)
              components/ (glass modifier, TaskRow, QuickAdd, TaskSheet)
              screens/ (Today, Upcoming, Calendar, All, Insights, Focus, Settings, Onboarding)

## Features ported from the web app
Today/Upcoming/All/Important/Completed/Calendar, lists, subtasks, tags, priority, repeat,
NL quick add with live chips, swipe complete/delete + undo, search, focus (Pomodoro), insights
(streak, weekly chart, heatmap), 6 wallpapers × 8 accents × light/dark/system, glass intensity,
text size, reduce motion, JSON export/import.

## Native extras
Exact-alarm reminders (Done / Snooze 10m from the notification), re-armed after reboot,
daily plan + evening review digests, quiet hours, focus timer that notifies when the app is closed.
