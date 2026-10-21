package com.example.quickpay.utils

object PhoneUtils {
    /**
     * Normalizes Kenyan phone numbers into 254XXXXXXXXX format.
     * Returns null if invalid format.
     */
    fun normalizePhoneNumber(rawPhone: String): String? {
        if (rawPhone.isBlank()) return null
        var cleaned = rawPhone.replace("\\s+".toRegex(), "").replace("-", "").replace("+", "")

        if (cleaned.startsWith("0")) {
            cleaned = "254" + cleaned.substring(1)
        } else if (cleaned.startsWith("7") || cleaned.startsWith("1")) {
            cleaned = "254" + cleaned
        }

        val regex = "^254[71]\\d{8}$".toRegex()
        return if (regex.matches(cleaned)) cleaned else null
    }

    /**
     * Masks phone number for display (e.g. 2547****5678 or 07****5678)
     */
    fun maskPhoneNumber(phone: String): String {
        if (phone.length < 8) return "****"
        val prefix = phone.substring(0, 4)
        val suffix = phone.substring(phone.length - 4)
        return "$prefix****$suffix"
    }
}
