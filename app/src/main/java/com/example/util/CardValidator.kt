package com.example.util

object CardValidator {

    fun isValidLuhn(cardNumber: String): Boolean {
        val sanitized = cardNumber.replace("\\s|-".toRegex(), "")
        if (sanitized.length < 12 || !sanitized.all { it.isDigit() }) return false

        var sum = 0
        var alternate = false
        for (i in sanitized.length - 1 downTo 0) {
            var n = sanitized[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    fun detectBrand(cardNumber: String): String {
        val sanitized = cardNumber.replace("\\s|-".toRegex(), "")
        return when {
            sanitized.startsWith("4") -> "Visa"
            sanitized.matches(Regex("^(5[1-5]|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[0-1][0-9]|2720).*")) -> "Mastercard"
            sanitized.matches(Regex("^(34|37).*")) -> "American Express"
            sanitized.matches(Regex("^(6011|65|64[4-9]|622).*")) -> "Discover"
            sanitized.matches(Regex("^(352[8-9]|35[3-8][0-9]).*")) -> "JCB"
            else -> "Unknown"
        }
    }

    fun isValidExpiry(monthStr: String, yearStr: String): Boolean {
        val month = monthStr.toIntOrNull() ?: return false
        val year = yearStr.toIntOrNull() ?: return false

        if (month !in 1..12) return false

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

        val fullYear = if (year < 100) 2000 + year else year

        if (fullYear < currentYear) return false
        if (fullYear == currentYear && month < currentMonth) return false
        if (fullYear > currentYear + 20) return false

        return true
    }

    fun isValidCvc(cvcStr: String, brand: String = "Visa"): Boolean {
        val sanitized = cvcStr.trim()
        val requiredLength = if (brand == "American Express") 4 else 3
        return sanitized.length == requiredLength && sanitized.all { it.isDigit() }
    }
}
