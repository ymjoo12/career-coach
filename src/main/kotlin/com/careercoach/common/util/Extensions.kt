package com.careercoach.common.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDateTime.toKoreanDateTimeString(): String {
    return this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"))
}

fun LocalDate.toKoreanDateString(): String {
    return this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
}

fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return this.matches(emailRegex)
}

fun String.isValidPhoneNumber(): Boolean {
    val phoneRegex = "^(010|011|016|017|018|019)-?\\d{3,4}-?\\d{4}$".toRegex()
    return this.matches(phoneRegex)
}

fun String.normalizePhoneNumber(): String {
    return this.replace("-", "").replace(" ", "")
}

inline fun <T> T?.orThrow(message: () -> String): T {
    return this ?: throw IllegalArgumentException(message())
}