package com.example.quickpay

import com.example.quickpay.utils.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun normalizePhoneNumber_validFormats_returnsNormalized254() {
        assertEquals("254712345678", PhoneUtils.normalizePhoneNumber("0712345678"))
        assertEquals("254112345678", PhoneUtils.normalizePhoneNumber("0112345678"))
        assertEquals("254712345678", PhoneUtils.normalizePhoneNumber("+254712345678"))
        assertEquals("254712345678", PhoneUtils.normalizePhoneNumber("254712345678"))
        assertEquals("254712345678", PhoneUtils.normalizePhoneNumber("712345678"))
    }

    @Test
    fun normalizePhoneNumber_invalidFormats_returnsNull() {
        assertNull(PhoneUtils.normalizePhoneNumber("12345"))
        assertNull(PhoneUtils.normalizePhoneNumber("0201234567"))
        assertNull(PhoneUtils.normalizePhoneNumber(""))
    }

    @Test
    fun maskPhoneNumber_masksMiddleDigits() {
        assertEquals("2547****5678", PhoneUtils.maskPhoneNumber("254712345678"))
    }
}
