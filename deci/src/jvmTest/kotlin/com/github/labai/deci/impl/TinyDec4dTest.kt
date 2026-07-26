/*
MIT License

Copyright (c) 2026 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.deci.impl

import com.github.labai.deci.RoundingMode
import com.github.labai.deci.RoundingMode.CEILING
import com.github.labai.deci.RoundingMode.DOWN
import com.github.labai.deci.RoundingMode.FLOOR
import com.github.labai.deci.RoundingMode.HALF_DOWN
import com.github.labai.deci.RoundingMode.HALF_EVEN
import com.github.labai.deci.RoundingMode.HALF_UP
import com.github.labai.deci.RoundingMode.UP
import com.github.labai.deci.impl.TinyDec4d.Companion.buildTiny4dOrErr
import com.github.labai.deci.impl.TinyUDecMath.TWOINT_ERR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertTrue

class TinyDec4dTest {


    @ParameterizedTest
    @CsvSource(
        "0,     0,   0",
        "0,     4,   0",
        "1110,  3,   1.11",
        "1,     4,   0.0001",
        "999999999, 4, 99999.9999",
        "999999999, 7, 99.9999999",
        "0,    -1,   (error)",
        "-1,    0,   (error)",
        "1,    -1,   (error)",
        "1,     8,   (error)",
        "999999999,  3, (error)",
        "1000000000, 4, (error)",
        "100000,     0, (error)",
        "1000000001, 4, (error)",
        "100001,     0, (error)",
    )
    fun test_d4d_build(unscaled: Int, pos: Int, expected: String) {
        if (expected == "(error)") {
            assertEquals(TinyDec4d.ERR, TinyDec4d.buildTiny4dOrErr(unscaled, pos))
            assertThrows<IllegalArgumentException> {
                TinyDec4d.buildTiny4d(unscaled, pos)
            }
        } else {
            val dec1 = TinyDec4d.buildTiny4d(unscaled, pos)
            val dec2 = TinyDec4d.buildTiny4dOrErr(unscaled, pos)
            assertEquals(expected, dec1.toString())
            assertEquals(expected, dec2.toString())
        }
    }

    //
    // parseString, toString
    //

    @ParameterizedTest
    @CsvSource(
        "3.0000,      30000,  4",
        "+3.0000,     30000,  4",
        "0.0000,      0,      4",
        "+0.0000,     0,      4",
        "0.0000,      0,      4",
        "+1.0000,     10000,  4",
        "1.00001,     100001, 5",
        "0.000010,    1,      5",
        "0.010000000, 100,    4",
        "1000.0000,   10000000, 4",
        ".00010,      1,      4",
        "+.00010,     1,      4",
        "10.,         100000, 4",
        "-3,          30000,  4", // will ignore minus (it should be handled separately)
    )
    fun test_d4d_parseString_correct(str: String, expectedUnscaled: Int, expectedPos: Int) {
        val dec = TinyDec4d.parseString(str)
        assertEquals(expectedUnscaled, dec.unscaled())
        assertEquals(expectedPos, dec.pos())

        val dec2 = TinyDec4d.parseStringOrErr(str)
        assertEquals(expectedUnscaled, dec2.unscaled())
        assertEquals(expectedPos, dec2.pos())
    }

    @ParameterizedTest
    @CsvSource(
        "++3",
        "1.0.0",
        "1..0",
        "1000000000",
        "10.00000001",
        "0x1",
        "1 1",
    )
    fun test_d4d_parseString_invalid(str: String) {
        assertEquals(TinyDec4d.ERR, TinyDec4d.parseStringOrErr(str))
        assertThrows<IllegalArgumentException> { TinyDec4d.parseString(str) }
    }

    @Test
    fun test_d4d_parseString_invalid_space() {
        test_d4d_parseString_invalid(" 1")
        test_d4d_parseString_invalid("1 ")
        test_d4d_parseString_invalid("1,1")
    }

    @ParameterizedTest
    @CsvSource(
        "3,        3",
        "0,        0",
        "0.0,      0",
        "1.0,      1",
        "0.010,    0.01",
        "99999,    99999",
        "10.0,     10",
        "10001.001,10001.001",
    )
    fun test_d4d_toString(decStr: String, expectedStr: String) {
        val dec = dec(decStr)
        assertEquals(expectedStr, dec.toString())
    }

    @Test
    fun test_d4d_toString_err() {
        assertEquals("Err", TinyDec4d.ERR.toString())
    }

    //
    // round
    //
    private fun round4d(tiny: TinyDec4d, scale: Int, roundingMode: RoundingMode): TinyDec4d {
        val r = TinyUDecMath.round(tiny.unscaled(), tiny.pos(), scale, roundingMode)
        if (r == TWOINT_ERR)
            return TinyDec4d.ERR
        return buildTiny4dOrErr(r.first(), r.second())
    }

    @ParameterizedTest
    @CsvSource(
        "1.14, 1, 1.1",
        "1.15, 1, 1.2",
        "1.1,  1, 1.1",
        "1,    1, 1",
        "10,   0, 10",
        "10,   -1, (error)",
    )
    fun test_d4d_round(a: String, scale: Int, expected: String) {
        val a = dec(a)
        if (expected == "(error)") {
            // assertThrows<IllegalArgumentException> { a.round(scale, HALF_UP) }
            assertEquals(TinyDec4d.ERR, round4d(a, scale, HALF_UP))
        } else {
            assertEquals(dec(expected), round4d(a, scale, HALF_UP))
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
    fun test_d4d_round_modes(a: String, expUp: String, expDown: String, expCeiling: String, expFloor: String, expHalfUp: String, expHalfDown: String, expHalfEven: String) {
        val a = dec(a)
        assertEquals(dec(expUp), round4d(a, 1, UP))
        assertEquals(dec(expDown), round4d(a, 1, DOWN))
        assertEquals(dec(expCeiling), round4d(a, 1, CEILING))
        assertEquals(dec(expFloor), round4d(a, 1, FLOOR))
        assertEquals(dec(expHalfUp), round4d(a, 1, HALF_UP))
        assertEquals(dec(expHalfDown), round4d(a, 1, HALF_DOWN))
        assertEquals(dec(expHalfEven), round4d(a, 1, HALF_EVEN))
    }

    @ParameterizedTest
    @CsvSource(
        "5000.5, 0, 5001",
        "50.00005, 4, 50.0001",
        "ERR, 0, ERR",
    )
    fun test_d4d_round_limit(a: String, pos: Int, expect: String) {
        val a = dec(a)
        assertEquals(dec(expect), round4d(a, pos, HALF_UP))
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
        "5e4,         50000",
        "50000.0000,   50000",
        "-1,          1", // unsigned, i.e. sign is lost
        // no trim trailing zeros
        "0.100000000, ERR",
        "50000.00000, ERR",
        "9.9e4,       99000",
        // overflow
        "1e5,         ERR",
        "0.00000001,  ERR",
    )
    fun test_d4d_convertToTiny_bigDec(d: String, expected: String) {
        val dec = d.toBigDecimal()
        val result = TinyUDecMath.fromBigDecimalUnsigned(dec, 7)
        val tiny = if (result == TWOINT_ERR) TinyDec4d.ERR else TinyDec4d.buildTiny4dOrErr(result.first(), result.second())
        assertDecEquals(dec(expected), tiny)
    }

    @ParameterizedTest
    @CsvSource(
        "0,       0",
        "100,     100",
        "50000,   50000",
        "-1,      ERR",
        "100000,  ERR",
    )
    fun test_d4d_convertToTiny_long(d: String, expected: String) {
        val long = d.toLong()
        val res1 = TinyDec4d.valueOf(long)
        assertEquals(dec(expected), res1)

        val int = d.toInt()
        val res2 = TinyDec4d.valueOf(int)
        assertEquals(dec(expected), res2)
    }

    @ParameterizedTest
    @CsvSource(
        "2.1,       2",
        "200.020,   200",
        "50000,     50000",
        "ERR,       (error)",
    )
    fun test_d4d_getIntPart(dstr: String, expected: String) {
        val dec = dec(dstr)
        if (expected == "(error)") {
            assertThrows<IllegalArgumentException> {
                TinyUDecMath.getIntPart(dec.unscaled(), dec.pos())
            }
        } else {
            val result = TinyUDecMath.getIntPart(dec.unscaled(), dec.pos())
            assertEquals(expected.toInt(), result)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "5,         5.0000",
        "0.1,       0.1000",
        "0.120,     0.1200",
        "50000,     50000.0000",
        "ERR,       (error)",
    )
    fun test_d4d_toBigDecimal(dstr: String, expected: String) {
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

    private fun dec(s: String): TinyDec4d {
        if (s == "ERR")
            return TinyDec4d.ERR
        return TinyDec4d.parseString(s)
    }

    private fun assertDecEquals(dec1: TinyDec4d, dec2: TinyDec4d) {
        val comp = TinyUDecMath.compare(dec1.unscaled(), dec1.pos(), dec2.unscaled(), dec2.pos())
        assertTrue(comp == 0, "Decimals are not equal ($dec1 vs $dec2)")
    }
}
