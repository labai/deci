package com.github.labai.deci

import com.github.labai.deci.RoundingMode.CEILING
import com.github.labai.deci.RoundingMode.DOWN
import com.github.labai.deci.RoundingMode.FLOOR
import com.github.labai.deci.RoundingMode.HALF_DOWN
import com.github.labai.deci.RoundingMode.HALF_EVEN
import com.github.labai.deci.RoundingMode.HALF_UP
import com.github.labai.deci.RoundingMode.UP
import com.github.labai.deci.impl.TinyUDecMath
import com.github.labai.deci.impl.TinyUDecMath.ERR
import com.github.labai.deci.impl.TinyUDecMath.TinyUDec
import com.github.labai.deci.impl.TinyUDecMath.parseString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertNotEquals

class TinyUDecTest {

    //
    // parseString, toString
    //

    @ParameterizedTest
    @CsvSource(
        "3,     3,   0",
        "+3,    3,   0",
        "0,     0,   0",
        "+0,    0,   0",
        "0.0,   0,   0",
        "+0.0,  0,   0",
        "+1.0,  1,   0",
        "1.01,  101, 2",
        "0.010, 1,   2",
        "0.010000000, 1, 2",
        "10000000, 10000000, 0",
        "10.,    10, 0",
        ".10,    1,  1",
        "+.10,   1,  1",
        "+.10,   1,  1",
    )
    fun test_parseString_correct(str: String, expectedUnscaled: Int, expectedPos: Int) {
        val dec = TinyUDec.parseString(str)
        assertEquals(expectedUnscaled, dec.unscaled())
        assertEquals(expectedPos, dec.pos())
    }

    @ParameterizedTest
    @CsvSource(
        "-3",
        "++3",
        "1.0.0",
        "1..0",
        "1000000000",
        "10.00000001",
        "0x1",
        "1 1",
    )
    fun test_parseString_invalid(str: String) {
        val dec = TinyUDecMath.parseStringOrErr(str)
        assertEquals(ERR, dec)
    }

    @Test
    fun test_parseString_invalid_space() {
        test_parseString_invalid(" 1")
        test_parseString_invalid("1 ")
        test_parseString_invalid("1,1")
    }

    @ParameterizedTest
    @CsvSource(
        "3,        3",
        "0,        0",
        "0.0,      0",
        "1.0,      1",
        "0.010,    0.01",
        "10000000, 10000000",
        "10.0,     10",
        "10001.001,10001.001",
    )
    fun test_toString(decStr: String, expectedStr: String) {
        val dec = dec(decStr)
        assertEquals(expectedStr, dec.toString())
    }

    @Test
    fun test_toString_err() {
        assertEquals("Err", ERR.toString())
    }

    //
    // add
    //

    @ParameterizedTest
    @CsvSource(
        "3,5,8",
        "0,0,0",
        "0,5,5",
        "5,0,5",
    )
    fun test_add_simpleInt(a: String, b: String, expected: String) {
        val result = dec(a).add(dec(b))
        assertEquals(dec(expected), result)
    }

    @ParameterizedTest
    @CsvSource(
        "1.10,  2.20,   3.3",
        "1.1,   2.02,   3.12",
        "1.1,   2,      3.1",
        "1.1,   0,      1.1",
        "1.5,   2.6,    4.1",
        "1,     0.001,  1.001",
        "0.001, 0.002,  0.003",
        "500000000, 400000000, 900000000",
    )
    fun test_add_scaled(a: String, b: String, expected: String) {
        val a = dec(a)
        val b = dec(b)
        val expected = dec(expected)
        val ab = a.add(b)
        val ba = b.add(a)
        assertEquals(expected, ab)
        assertEquals(expected, ba)
        assertEquals(ab.tiny, ba.tiny)
        assertNotEquals(ERR, ab)
        assertNotEquals(ERR, ba)
    }

    @ParameterizedTest
    @CsvSource(
        "500000000, 500000000, ERR",
        "500000000, 500000001, ERR",
        "50000000,  1.01,      ERR",
        "0,   ERR, ERR",
        "0.1, ERR, ERR",
        "ERR, 0,   ERR",
        "ERR, 0.1, ERR",
        "ERR, ERR, ERR",
    )
    fun test_add_invalid(a: String, b: String, expected: String) {
        val expected = dec(expected)
        val result = dec(a).add(dec(b))
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @CsvSource(
        "1.5,  1.5,  3, 0",
        "0.25, 0.75, 1, 0",
        "9999999.9, 9999999.9, 19999999.8, 1"
    )
    fun test_add_trailingZeros(a: String, b: String, expected: String, expectedPos: Int) {
        val expected = dec(expected)
        val result = dec(a).add(dec(b))
        assertEquals(expected, result)
        assertEquals(expectedPos, result.pos())
    }

    //
    // subtract
    //

    @ParameterizedTest
    @CsvSource(
        "8,3,5",
        "0,0,0",
        "5,0,5",
        "5,5,0",
    )
    fun test_sub_simpleInt(a: String, b: String, expected: String) {
        val result = dec(a).sub(dec(b))
        assertEquals(dec(expected), result)
    }

    @ParameterizedTest
    @CsvSource(
        "3.3,   1.10, 2.20",
        "3.12,  1.1,  2.02",
        "3.1,   1.1,  2",
        "1.1,   1.1,  0",
        "4.1,   1.5,  2.6",
        "1.001, 0.001, 1",
        "0.003, 0.001, 0.002",
    )
    fun test_sub_scaled(a: String, b: String, expected: String) {
        val a = dec(a)
        val b = dec(b)
        val expected = dec(expected)
        val res = a.sub(b)
        assertEquals(expected, res)
    }

    @ParameterizedTest
    @CsvSource(
        "3.5,  1.5,  2, 0",
        "1.25, 0.25, 1, 0",
        "1999999.85, 999999.95, 999999.9, 1"
    )
    fun test_sub_trailingZeros(a: String, b: String, expected: String, expectedPos: Int) {
        val expected = dec(expected)
        val result = dec(a).sub(dec(b))
        assertEquals(expected, result)
        assertEquals(expectedPos, result.pos())
    }

    @ParameterizedTest
    @CsvSource(
        "10, 11, ERR", // minus
        "10000000, 1.001, ERR", // precision overflow
        "15, 1.001, 13.999",
        "ERR, 0, ERR",
        "ERR, 1, ERR",
        "ERR, 0.1, ERR",
        "0, ERR, ERR",
        "1, ERR, ERR",
        "0.1, ERR, ERR",
        "ERR, ERR, ERR",
    )
    fun test_sub_invalid(a: String, b: String, expected: String) {
        val expected = dec(expected)
        val result = dec(a).sub(dec(b))
        assertEquals(expected, result)
    }

    //
    // multiply
    //

    @ParameterizedTest
    @CsvSource(
        "3,5,15",
        "0,0,0",
        "1,5,5",
        "5,0,0",
    )
    fun test_mul_simpleInt(a: String, b: String, expected: String) {
        val result = dec(a).mul(dec(b))
        assertEquals(dec(expected), result)
    }

    @ParameterizedTest
    @CsvSource(
        "1.10,  2,      2.2",
        "1.10,  2.20,   2.42",
        "1.1,   1,      1.1",
        "1.1,   0,      0",
        "1,     0.001,  0.001",
        "0.1,   0.2,    0.02",
        "1000.005, 400000, 400002000",
    )
    fun test_mul_scaled_ok(a: String, b: String, expected: String) {
        val a = dec(a)
        val b = dec(b)
        val expected = dec(expected)
        val ab = a.mul(b)
        val ba = b.mul(a)
        assertEquals(expected, ab)
        assertEquals(expected, ba)
        assertEquals(ab.tiny, ba.tiny)
        assertNotEquals(ERR, ab)
        assertNotEquals(ERR, ba)
    }


    @ParameterizedTest
    @CsvSource(
        "0,   ERR, ERR",
        "0.1, ERR, ERR",
        "ERR, 0,   ERR",
        "ERR, 0.1, ERR",
        "ERR, ERR, ERR",
        "0.001, 0.001, ERR",
        "30000, 40000, ERR",
    )
    fun test_mul_invalid(a: String, b: String, expected: String) {
        val result = dec(a).mul(dec(b))
        assertEquals(dec(expected), result)
    }

    //
    // divide
    //

    @ParameterizedTest
    @CsvSource(
        "3, 1, 3",
        "0, 10, 0",
        "30, 10, 3",
        "50.1, 10, 5.01",
        "0.12, 0.1, 1.2",
        "0.12, 10, 0.012",
        "0.12, 0.001, 120",
        "12, 0.01, 1200",
        "12, 6, 2",
        "1.2, 0.6, 2",
        "0.1,100,0.001",
        // div by 0
        "3, 0, ERR",
        "0, 0, ERR",
        // overflow
        "0.1, 10000000, ERR",
        "10000000, 0.01, ERR",
        // not supported
        "12, 5, ERR",
        "ERR, 1, ERR",
        "1, ERR, ERR",
        "ERR, ERR, ERR",
    )
    fun test_div_simpleCases(u: String, v: String, expected: String) {
        val result = dec(u).tryDiv(dec(v))
        assertEquals(dec(expected), result)
    }

    @ParameterizedTest
    @CsvSource(
        "10,   3,    1",
        "9,    3,    0",
        "3,    9,    3",
        "4.1,  3,    1.1",
        "4.11, 3.2,  0.91",
        "4.1,  3.21, 0.89",
        "1000000.1,  0.21, 0.05",
        "1,    0,    ERR",
        "ERR,  1,    ERR",
        "1,    ERR,  ERR",
        "ERR,  ERR,  ERR",
    )
    fun test_rem(u: String, v: String, expected: String) {
        val result = dec(u).rem(dec(v))
        assertEquals(dec(expected), result)
    }

    //
    // round
    //

    @ParameterizedTest
    @CsvSource(
        "1.14, 1, 1.1",
        "1.15, 1, 1.2",
        "1.1,  1, 1.1",
        "1,    1, 1",
        "10,   0, 10",
        "10,   -1, (error)",
    )
    fun test_round(a: String, scale: Int, expected: String) {
        val a = dec(a)
        if (expected == "(error)") {
            assertThrows<IllegalArgumentException> { a.round(scale, HALF_UP) }
        } else {
            assertEquals(dec(expected), a.round(scale, HALF_UP))
        }
    }

    @ParameterizedTest
    @CsvSource(
        "1.14, 1.2, 1.1, 1.2, 1.1, 1.1, 1.1, 1.1",
        "1.15, 1.2, 1.1, 1.2, 1.1, 1.2, 1.1, 1.2",
        "1.16, 1.2, 1.1, 1.2, 1.1, 1.2, 1.2, 1.2",
        "1.25, 1.3, 1.2, 1.3, 1.2, 1.3, 1.2, 1.2",
        "1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1",
        "1, 1, 1, 1, 1, 1, 1, 1",
        "1, 1, 1, 1, 1, 1, 1, 1",
    )
    fun test_round_modes(a: String, expUp: String, expDown: String, expCeiling: String, expFloor: String, expHalfUp: String, expHalfDown: String, expHalfEven: String) {
        val a = dec(a)
        assertEquals(dec(expUp), a.round(1, UP))
        assertEquals(dec(expDown), a.round(1, DOWN))
        assertEquals(dec(expCeiling), a.round(1, CEILING))
        assertEquals(dec(expFloor), a.round(1, FLOOR))
        assertEquals(dec(expHalfUp), a.round(1, HALF_UP))
        assertEquals(dec(expHalfDown), a.round(1, HALF_DOWN))
        assertEquals(dec(expHalfEven), a.round(1, HALF_EVEN))
    }

    @ParameterizedTest
    @CsvSource(
        "50000000.5, 0, 50000001",
        "50.005, 2, 50.01",
        "ERR, 0, ERR",
    )
    fun test_round_limit(a: String, pos: Int, expect: String) {
        val a = dec(a)
        assertEquals(dec(expect), a.round(pos, HALF_UP))
    }


    //
    // compare
    //
    @ParameterizedTest
    @CsvSource(
        "0,0,0",
        "1,1,0",
        "1,0,1",
        "0,1,-1",
        "0.12,1,-1",
        "1, 1.12,-1",
        "ERR,ERR,0",
        "0.1,10000000,-1",
    )
    fun test_compare(a: String, b: String, expectedRes: Int) {
        val result = dec(a).compareTo(dec(b))
        assertEquals(expectedRes, result)
    }

    @ParameterizedTest
    @CsvSource(
        "0,           0",
        "0.1,         0.1",
        "0.001,       0.001",
        "0.100,       0.1",
        "100.100,     100.1",
        "1.2e3,       1200",
        "1.23e2,      123",
        "1.2e-2,      0.012",
        "5e8,         500000000",
        "500000000,   500000000",
        // no trim trailing zeros
        "0.100000000, ERR",
        "500000000.0, ERR",
        "9.9e8,       990000000",
        // overflow
        "-1,          ERR",
        "1e9,         ERR",
        "0.0001,      ERR",
    )
    fun test_convertToTiny_bigDec(d: String, expected: String) {
        val dec = d.toBigDecimal()
        val result = TinyUDec.valueOf(dec)
        assertEquals(dec(expected), result)
    }

    @ParameterizedTest
    @CsvSource(
        "0,           0",
        "100,         100",
        "500000000,   500000000",
        "-1,          ERR",
        "1000000000,  ERR",
    )
    fun test_convertToTiny_long(d: String, expected: String) {
        val long = d.toLong()
        val res1 = TinyUDec.valueOf(long)
        assertEquals(dec(expected), res1)

        val int = d.toInt()
        val res2 = TinyUDec.valueOf(int)
        assertEquals(dec(expected), res2)
    }

    @ParameterizedTest
    @CsvSource(
        "2.1,       2",
        "200.020,   200",
        "500000000, 500000000",
        "ERR,       (error)",
    )
    fun test_getIntPart(dstr: String, expected: String) {
        val dec = dec(dstr)
        if (expected == "(error)") {
            assertThrows<IllegalArgumentException> { dec.intPart() }
        } else {
            val result = dec.intPart()
            assertEquals(expected.toInt(), result)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "0.1,         1",
        "0.020,       2",
        "500000000,   0",
        "ERR,         (error)",
    )
    fun test_getDecPart(dstr: String, expected: String) {
        val dec = dec(dstr)
        if (expected == "(error)") {
            assertThrows<IllegalArgumentException> { dec.decPart() }
        } else {
            val result = dec.decPart()
            assertEquals(expected.toInt(), result)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "5,    5,   true",
        "0.1,  0.1, true",
        "1.0,  1,   true",
        "ERR,  ERR, true", // errors are equal
        "0.1,  1.0, false",
        "ERR,  1,   false",
        "1,    ERR, false",
    )
    fun test_isEqual(a: String, b: String, expected: Boolean) {
        val a = dec(a)
        val b = dec(b)
        val result = a.isEqual(b)
        assertEquals(expected, result)
    }

    @Test
    fun test_trimTrailingZeros() {
        // (pos shl 29) or unsigned
        val raw1 = 3 shl 30 or 100
        val tiny1 = TinyUDec(raw1)
        val tiny1n = tiny1.trimTrailingZeros()
        assertEquals("0.100", tiny1.toString())
        assertEquals("0.1", tiny1n.toString())

        // check error case
        assertEquals(ERR, ERR.trimTrailingZeros())
    }

    @ParameterizedTest
    @CsvSource(
        "5,         5",
        "0.1,       0.1",
        "0.120,     0.12",
        "500000000, 500000000",
        "ERR,       (error)",
    )
    fun test_toBigDecimal(dstr: String, expected: String) {
        val tiny = dec(dstr)
        if (expected == "(error)") {
            assertThrows<IllegalArgumentException> { tiny.toBigDecimal() }
        } else {
            val bigd = tiny.toBigDecimal()
            assertEquals(expected, bigd.toString())
        }
    }

    //
    // helpers
    //

    private fun dec(s: String): TinyUDec {
        if (s == "ERR")
            return ERR
        return parseString(s)
    }
}
