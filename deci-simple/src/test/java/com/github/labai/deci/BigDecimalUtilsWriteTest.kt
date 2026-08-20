/*
MIT License
Copyright (c) 2026 Augustus
*/
package com.github.labai.deci

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Random
import kotlin.test.assertEquals

internal class BigDecimalUtilsWriteTest {

    //
    // trimTrailingZeros
    //

    @ParameterizedTest(name = "trimTrailingZeros(\"{0}\") = \"{1}\"")
    @CsvSource(
        "100.5000,           100.5",
        "100.500,            100.5",
        "100.100,            100.1",
        "100.000,            100",
        "0.000,              0",
        "0.500,              0.5",
        "1.20,               1.2",
        "123.456,            123.456",
        "100.,               100",
        ".500,               .5",
        "42.00000,           42",
        "-12.340,            -12.34",
        "-0.000,             -0",
        "'',                 ''"
    )
    fun test_trimsTrailingZeros_decimal(input: String, expected: String) {
        assertEquals(expected, BigDecimalUtils.trimTrailingZeros(input))
    }

    @ParameterizedTest(name = "integers pass through unchanged: \"{0}\"")
    @CsvSource("100", "0", "05", "-42", "1000000")
    fun test_trimsTrailingZeros_integer(input: String) {
        assertEquals(input, BigDecimalUtils.trimTrailingZeros(input))
    }

    //
    // decimalToString
    //

    @Test
    fun test_zero() {
        assertDecimal("0")
        assertDecimal("0E-0")  // scale 0 zero
        assertTrimmed("0.00")     // scale > 0, digitCount(1) <= scale -> "0.00"
    }

    @ParameterizedTest
    @CsvSource(
        "0",
        "1",
        "9",
        "10",
        "123",
        "-123",
        "999999999",
        "1000000000",
    )
    fun test_simpleIntegers(str: String) {
        assertDecimal(str)
    }

    @ParameterizedTest
    @CsvSource(
        "1.5",
        "-1.5",
        "0.1",
        "0.01",
        "0.001",
        "123.456",
        "-123.456",
    )
    fun test_simpleDecimals(str: String) {
        assertDecimal(str)
    }

    @ParameterizedTest
    @CsvSource(
        "100.100",
        ".100",
        "100.00",
        "0.100",
        "-0.100",
        "-0.0000000000000000100",
        "0.001000000000000000000000",
    )
    fun test_trimTrailingZeros(str: String) {
        assertTrimmed(str)
    }

    @Test
    fun test_leadingZerosAfterDecimalPoint() {
        // digitCount < scale -> "0.000...digits" branch
        assertDecimal("0.0000001")
        assertDecimal("-0.0000001")
        assertDecimal("0.00000000000000001") // 17 zeros + 1 digit, scale 17 > 16
        assertDecimal(BigDecimal(BigInteger.ONE, 20).toPlainString())
        assertDecimal(BigDecimal(BigInteger.valueOf(-7), 25).toPlainString())
    }

    @Test
    fun test_digitCountEqualsScale() {
        // digitCount == scale -> "0." + digits exactly, no padding
        assertDecimal(BigDecimal(BigInteger.valueOf(123), 3).toPlainString()) // 0.123
        assertDecimal(BigDecimal(BigInteger.valueOf(12345), 5).toPlainString()) // 0.12345
    }

    @Test
    fun test_trailingZerosInUnscaledValue() {
        assertDecimal("100")
        assertDecimal("1000000")
        assertDecimal("10000000000000000") // 17 digits, all but first are zero
        assertDecimal("100000000000000000000000000000") // 30 zeros after '1'
    }


    @Test
    fun test_aroundSingleLongBoundary() {
        // bitLength <= 62 fast path vs the two-long divideAndRemainder path
        assertDecimal(BigInteger.valueOf(Long.MAX_VALUE).toString()) // 19 digits, fits in long
        assertDecimal(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString()) // just over long range
        assertDecimal(BigInteger.ONE.shiftLeft(63).subtract(BigInteger.ONE).toString()) // 2^63 - 1
        assertDecimal(BigInteger.ONE.shiftLeft(63).toString()) // 2^63
        assertDecimal(BigInteger.ONE.shiftLeft(64).toString()) // 2^64
    }

    @Test
    fun test_around16DigitHighLowSplit() {
        assertDecimal(pow10(16).subtract(BigInteger.ONE).toString()) // 16 nines
        assertDecimal(pow10(16).toString()) // 1 followed by 16 zeros
        assertDecimal(pow10(16).add(BigInteger.ONE).toString()) // 1 + 16 zeros + 1
        assertDecimal(pow10(17).subtract(BigInteger.ONE).toString()) // 17 nines
    }

    @Test
    fun test_maxSupported32Digits() {
        val max32: BigInteger = pow10(32).subtract(BigInteger.ONE) // 32 nines
        assertDecimal(max32.toString())
        assertDecimal(max32.negate().toString())
        assertDecimal(BigDecimal(max32, 10).toPlainString())
        assertDecimal(BigDecimal(max32, 40).toPlainString()) // scale > digitCount too
    }

    @Test
    fun test_invalid_more32Digits() {
        val big33: BigInteger = pow10(33).add(BigInteger.valueOf(7))
        Assertions.assertNull(BigDecimalUtils.decimalToString(BigDecimal(big33, 5)))
        Assertions.assertNull(BigDecimalUtils.decimalToString(BigDecimal(big33.negate(), 5)))
    }

    @Test
    fun test_invalid_negativeScale() {
        val negScale = BigDecimal(BigInteger.valueOf(123), -2) // 12300
        Assertions.assertNull(BigDecimalUtils.decimalToString(negScale))
    }

    @Test
    fun test_negativeValues() {
        assertDecimal("-1")
        assertDecimal("-0.5")
        assertDecimal("-0.0001")
        assertDecimal(pow10(20).negate().toString())
    }

    @ParameterizedTest
    @CsvSource(
        "123, 0",
        "123, 1",
        "123, 2",
        "123, 3",
        "123, 4",
        "123, 8",
        "-123, 0",
        "-123, 3",
        "-123, 8",
        "0, 0",
    )
    fun test_unscaledScaleCombinations(unscaled: Long, scale: Int) {
        assertDecimal(BigDecimal(BigInteger.valueOf(unscaled), scale))
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 8, 9, 15, 16, 17, 18, 19, 20, 24, 31, 32])
    fun test_allNinesAtEachDigitLength(digitCount: Int) {
        val allNines: BigInteger = pow10(digitCount).subtract(BigInteger.ONE)
        assertDecimal(allNines.toString())
        assertDecimal(allNines.negate().toString())
        assertDecimal(BigDecimal(allNines, digitCount)) // scale == digitCount
        assertDecimal(BigDecimal(allNines, digitCount + 3)) // scale > digitCount
        if (digitCount > 1) {
            assertDecimal(BigDecimal(allNines, digitCount - 1).toPlainString()) // scale < digitCount
        }
    }

    @Test
    fun test_randomizedRoundTripUpTo32Digits() {
        val rnd = Random(5)
        for (i in 0..<20000) {
            val digitCount = 1 + rnd.nextInt(32)
            val sb = StringBuilder(digitCount)
            sb.append(('1'.code + rnd.nextInt(9)).toChar()) // no leading zero in the raw digits
            for (d in 1..<digitCount) {
                sb.append(('0'.code + rnd.nextInt(10)).toChar())
            }
            var unscaled = BigInteger(sb.toString())
            if (rnd.nextBoolean())
                unscaled = unscaled.negate()

            val scale = rnd.nextInt(digitCount + 10) // covers scale < digitCount, ==, and >
            val decimal = BigDecimal(unscaled, scale)

            assertTrimmed(decimal)
        }
    }

    companion object {
        private fun pow10(n: Int): BigInteger {
            return BigInteger.TEN.pow(n)
        }

        private fun assertTrimmed(plainString: String) {
            assertTrimmed(BigDecimal(plainString))
        }

        private fun assertTrimmed(decimal: BigDecimal) {
            val expected = decimal.stripTrailingZeros().toPlainString()
            val actual = BigDecimalUtils.decimalToString(decimal)
            Assertions.assertEquals(expected, actual) { "unscaled=" + decimal.unscaledValue() + " scale=" + decimal.scale() }
        }

        private fun assertDecimal(plainString: String) {
            assertDecimal(BigDecimal(plainString))
        }

        private fun assertDecimal(decimal: BigDecimal) {
            val expected = decimal.toPlainString()
            val actual = BigDecimalUtils.decimalToString(decimal)
            Assertions.assertEquals(expected, actual) { "unscaled=" + decimal.unscaledValue() + " scale=" + decimal.scale() }
        }
    }
}
