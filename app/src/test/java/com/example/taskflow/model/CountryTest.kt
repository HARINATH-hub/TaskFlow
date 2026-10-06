package com.example.taskflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryTest {

    private val india = Country.ALL.first { it.isoCode == "IN" }
    private val us = Country.ALL.first { it.isoCode == "US" }

    @Test
    fun testCountryListIsComprehensiveAndIndiaIsDefault() {
        val countries = Country.ALL
        assertTrue(countries.size >= 100)
        assertEquals("IN", countries[0].isoCode)
        assertEquals("India", countries[0].name)
        assertEquals("+91", countries[0].dialCode)
        assertEquals(india, Country.DEFAULT)
    }

    @Test
    fun testExtractLocalNumber_StandardLocalDigits() {
        // Input: 8328627099 -> Expected local: 8328627099
        val local = Country.extractLocalNumber(india, "8328627099")
        assertEquals("8328627099", local)
    }

    @Test
    fun testExtractLocalNumber_InternationalPlusPrefix() {
        // Input: +918328627099 -> Expected local: 8328627099
        val local = Country.extractLocalNumber(india, "+918328627099")
        assertEquals("8328627099", local)

        // With spaces and formatting: +91 83286 27099
        val formatted = Country.extractLocalNumber(india, "+91 83286 27099")
        assertEquals("8328627099", formatted)
    }

    @Test
    fun testExtractLocalNumber_CountryCodeWithoutPlus() {
        // Input: 918328627099 -> Expected local: 8328627099
        val local = Country.extractLocalNumber(india, "918328627099")
        assertEquals("8328627099", local)
    }

    @Test
    fun testExtractLocalNumber_TrunkPrefixZero() {
        // Input: 08328627099 -> Expected local: 8328627099
        val local = Country.extractLocalNumber(india, "08328627099")
        assertEquals("8328627099", local)
    }

    @Test
    fun testExtractLocalNumber_Prepend191Safeguard() {
        // Critical: Input 1918328627099 must strip the '191' prefix and output 8328627099
        val local = Country.extractLocalNumber(india, "1918328627099")
        assertEquals("8328627099", local)
        assertNotEquals("1918328627099", local)
    }

    @Test
    fun testNormalizePhoneNumber_AllCasesForIndia() {
        // 1. Direct local number
        val norm1 = Country.normalizePhoneNumber(india, "8328627099")
        assertEquals("+918328627099", norm1)

        // 2. International +91 format
        val norm2 = Country.normalizePhoneNumber(india, "+918328627099")
        assertEquals("+918328627099", norm2)

        // 3. 91 without plus
        val norm3 = Country.normalizePhoneNumber(india, "918328627099")
        assertEquals("+918328627099", norm3)

        // 4. Trunk 0 prefix
        val norm4 = Country.normalizePhoneNumber(india, "08328627099")
        assertEquals("+918328627099", norm4)

        // 5. Corrupted 191 prefix
        val norm5 = Country.normalizePhoneNumber(india, "1918328627099")
        assertEquals("+918328627099", norm5)

        // Under no circumstances should the output ever be 1918328627099
        assertNotEquals("1918328627099", norm1)
        assertNotEquals("1918328627099", norm2)
        assertNotEquals("1918328627099", norm3)
        assertNotEquals("1918328627099", norm4)
        assertNotEquals("1918328627099", norm5)
    }

    @Test
    fun testNormalizePhoneNumber_US() {
        assertEquals("+15551234567", Country.normalizePhoneNumber(us, "5551234567"))
        assertEquals("+15551234567", Country.normalizePhoneNumber(us, "+1 555-123-4567"))
        assertEquals("+15551234567", Country.normalizePhoneNumber(us, "15551234567"))
    }

    @Test
    fun testValidatePhoneNumber_IndiaRules() {
        // Valid 10-digit Indian numbers starting with 6, 7, 8, 9
        assertNull(Country.validatePhoneNumber(india, "8328627099"))
        assertNull(Country.validatePhoneNumber(india, "9876543210"))
        assertNull(Country.validatePhoneNumber(india, "7012345678"))
        assertNull(Country.validatePhoneNumber(india, "6234567890"))
        assertNull(Country.validatePhoneNumber(india, "+918328627099"))
        assertNull(Country.validatePhoneNumber(india, "918328627099"))
        assertNull(Country.validatePhoneNumber(india, "08328627099"))

        // Invalid: Blank
        assertNotNull(Country.validatePhoneNumber(india, ""))

        // Invalid: Too short
        assertNotNull(Country.validatePhoneNumber(india, "8328627"))

        // Invalid: Starting with invalid digit for Indian mobile (e.g. 1, 2, 3, 4, 5)
        assertNotNull(Country.validatePhoneNumber(india, "2345678901"))
    }

    @Test
    fun testSearchTerms() {
        assertTrue(india.searchTerms.contains("india"))
        assertTrue(india.searchTerms.contains("+91"))
        assertTrue(india.searchTerms.contains("in"))
    }
}
