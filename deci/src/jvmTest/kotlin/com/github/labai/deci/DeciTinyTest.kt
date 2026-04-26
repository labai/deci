package com.github.labai.deci

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertEquals

/**
 * @author Augustus
 * created on 2026-04-25
 */
class DeciTinyTest {

    @ParameterizedTest
    @CsvSource(
        "0",
        "-0",
        "-1",
        "-1.1",
        "-0.1",
        )
    fun test_negative_parse(str: String) {
        val d = Deci(str)
        assertEquals(BigDecimal(str), d.toBigDecimal())
    }


    @ParameterizedTest
    @CsvSource(
        "-1,    1,      0",
        "-1.1,  0.9,    -0.2",
        "-1,    1.1,    0.1",
        "-1,    -1.1,   -2.1",
        "-1, -99999999.1, -100000000.1",
        "-99999999.1, -1, -100000000.1",
    )
    fun test_negative_add(a: String, b: String, expected: String) {
        assertEquals(expected.deci, a.deci + b.deci)
    }

    @ParameterizedTest
    @CsvSource(
        "0,   1,     -1",
        "0,   -1,    1",
        "-1,  0,     -1",
        "-1,  1,     -2",
        "1,   -1,    2",
        "-1,  -1.1,  0.1",
        "-1,  1.1,   -2.1",
        "1,   -1.1,  2.1",
        "-99999999.1, 1, -100000000.1",
        "-1, 99999999.1, -100000000.1",
    )
    fun test_negative_sub(a: String, b: String, expected: String) {
        assertEquals(expected.deci, a.deci - b.deci)
    }


    @ParameterizedTest
    @CsvSource(
        "0,   -1,    0",
        "-1,  0 ,    0",
        "1,   1,     1",
        "1,   -1,    -1",
        "-1,  1,     -1",
        "-1,  -1,    1",
        "1.1, -1.1,  -1.21",
        "-10000000.1, -11, 110000001.1",
        "10000000.1, -11, -110000001.1",
    )
    fun test_negative_mul(a: String, b: String, expected: String) {
        assertEquals(expected.deci, a.deci * b.deci)
    }

    @ParameterizedTest
    @CsvSource(
        "-10, -1,  10",
        "100, -10, -10",
        "0,   -1,  0",
        "10000000, -20, -500000",
        "-10000000, 20, -500000",
    )
    fun test_negative_tryDiv(a: String, b: String, expected: String) {
        assertEquals(expected.deci, a.deci / b.deci)
    }

    @ParameterizedTest
    @CsvSource(
        "-10, 6,   -4",
        "-10, -6,  -4",
        "10,  -6,  4",
        "-10, 10,  0",
        "-1000000000, 10, 0",
        "-1000000000, 6, -4",
        "-1000000000, -6, -4",
        "1000000000, -6, 4",
    )
    fun test_negative_rem(a: String, b: String, expected: String) {
        assertEquals(expected.deci, a.deci % b.deci)
    }

    @ParameterizedTest
    @CsvSource(
        "-2, -2,  0",
        "-2, -3,  1",
        "-2, -1, -1",
        " 1, -2,  1",
        "-2,  1, -1",
        "-2,  2, -1",
        "-1,  0, -1",
        " 1, -2,  1",
        " 2, -2,  1",
        " 0, -1,  1",
        "-1000000000, -1,  -1",
        "-1000000000, -1000000000,  0",
        "0, -1000000000,  1",
    )
    fun test_negative_compare(a: String, b: String, expected: String) {
        assertEquals(expected.deci, (a.deci.compareTo(b.deci)).deci)
    }

    @ParameterizedTest
    @CsvSource(
        "-1.1, 1.1",
        "1.1,  -1.1",
        "1000000000.1, -1000000000.1",
        "-1000000000.1, 1000000000.1",
    )
    fun test_negative_negate(a: String, expected: String) {
        assertEquals(expected.deci, a.deci.unaryMinus())
    }
}
