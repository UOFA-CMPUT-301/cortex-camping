package com.example.cortex_camping.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("MMM d")
private val fullDate = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val clockTime = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

fun formatDate(date: LocalDate): String = date.format(fullDate)

fun formatShortDate(date: LocalDate): String = date.format(shortDate)

fun formatStay(start: LocalDate, end: LocalDate) = "${start.format(fullDate)} – ${end.format(fullDate)}"

fun formatRange(start: LocalDate, end: LocalDate) =
    if (start.year == end.year) "${start.format(shortDate)} – ${end.format(fullDate)}" else formatStay(start, end)

fun formatTime(time: LocalDateTime): String = time.format(clockTime)

fun formatSentAt(time: LocalDateTime): String =
    if (time.toLocalDate() == LocalDate.now()) formatTime(time) else time.format(shortDate)

fun nights(nights: Int) = "$nights night${if (nights == 1) "" else "s"}"

fun nightsGuests(nights: Int, guests: Int) = "${nights(nights)} · $guests guest${if (guests == 1) "" else "s"}"
