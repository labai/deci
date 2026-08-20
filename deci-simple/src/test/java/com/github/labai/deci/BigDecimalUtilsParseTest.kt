/*
MIT License
Copyright (c) 2026 Augustus
*/
package com.github.labai.deci

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Augustus
 * created on 2026-07-02
 */
class BigDecimalUtilsParseTest {

    @Test
    fun test_basic() {
        val str = "12345678901234567890.1234567890"
        val bd = BigDecimalUtils.parseString(str)
        assertEquals(BigDecimal(str).stripTrailingZeros(), bd)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "0",
            "0.0",
            "1",
            "-1",
            "123",
            "-123",
            "123.456",
            "-123.456",

            "-123.45",
            "-12345678901234567890.1",
            "123.100",
            "1234567890123456.000",           // trim crossing the 16-digit boundary
            "0.000",
            ".100",

            "-0",
            "-0.0",
            "-0.001",
            "-123456789012345678901234567890.1",
            "+123.45",
            "+123456789012345678.9",

            "0.1",
            "0.123",
            ".5",
            "-0.5",
            "-.5",

            // --- trailing zeros (<=16 digits total) ---
            "123.100",
            "123.1000000",
            "100.000",
            "1000",
            "1000.0",
            "0.100",
            "10.00",
            "-000.100",

            // --- trailing zeros that cross the 16-digit boundary (Bug 2) ---
            "1234567890123456.000",     // exactly 16 int digits + trimmed decimals
            "1234567890123456.100",     // 16 int digits + non-trimmable decimal
            "123456789012345.6000",     // 15 int digits + 1 frac + trailing zeros
            "12345678901234567.000",    // 17 int digits, trims back across boundary
            "12345678901234560.000",    // trailing zero IN the integer part before dot

            // 16 digits
            "1234567890123456",         // 16 digits, no dot
            "1234567890123456.0",
            "123456789012345.9",        // 15 int + 1 frac = 16 total
            "12345678901234567",        // 17 digits, no dot -> forces buf2 split

            // 32 digits
            "12345678901234567890123456789012",     // exactly 32 digits
            "-99999999999999999999999999999999",      // 32 digits negative, boundary

            // leading zeros in integer part
            "007",
            "007.5",
            "000123.456",
            "0000000000000000123.456",  // leading zeros beyond 16 chars but few real digits

            // decimal near Long boundaries
            "9223372036854775807",       // Long.MAX_VALUE, 19 digits -> forces split
            "-9223372036854775808",      // Long.MIN_VALUE magnitude, 19 digits
            "18446744073709551615",      // 2^64 - 1, 20 digits

            // lots of trailing zeros after dot, single nonzero digit
            "1.0000000000000001",        // 1 + 16 frac digits, forces split, last digit nonzero
            "1.00000000000000010",       // same + one more trailing zero to trim
        ],
    )
    fun test_valid(str: String) {
        val bd = BigDecimalUtils.parseString(str)
        assertEquals(BigDecimal(str).stripTrailingZeros().toPlainString(), bd?.toPlainString())
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "0.00000000000000000000000000000111",   // leading zeros
            "0000000000000000000000000000000111",   // leading zeros
            "0.10000000000000000000000000000000",   // trailing zeros
            "1.00000000000000000000000000000000",   // trailing zeros
            "12345678901234567890123456789012.0",   // 32 digits + trimmable decimal
        ],
    )
    fun test_invalid_todoImprove(str: String) {
        val bd = BigDecimalUtils.parseString(str)
        assertNull(bd)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "123456789012345678901234567890123",    // 33 digits -> should error
            "99999999999999999999999999999999.99999999999999999999",
            "5e10",         // scientific notation - not supported
            "12.34.56",     // two dots
            "--123",        // double sign
            "12a34",        // invalid char
            "+",
            "-",
            ".",
            "-.",
            "+.",
        ],
    )
    fun test_invalid(str: String) {
        val bd = BigDecimalUtils.parseString(str)
        assertNull(bd)
    }

    @Test
    fun test_invalid_2() {
        assertNull(BigDecimalUtils.parseString(""))
        assertNull(BigDecimalUtils.parseString(" 1"))
        assertNull(BigDecimalUtils.parseString("1 "))
    }
}
