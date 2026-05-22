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
package com.github.labai.deci

import com.github.labai.deci.RoundingMode.HALF_UP
import com.github.labai.deci.impl.TinyDec
import com.github.labai.deci.impl.TinyUDecMath.ERR_VALUE
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        "0,    0",
        "-1.1, 1.1",
        "1.1,  -1.1",
        "1000000000.1, -1000000000.1",
        "-1000000000.1, 1000000000.1",
    )
    fun test_negative_negate(a: String, expected: String) {
        assertEquals(expected.deci, a.deci.unaryMinus())
    }

    @Test
    fun test_var_math_tinyDec() {
        val perc = 30.deci
        var d = Deci("1")
        var dd: Deci
        assertDecEquals("1".deci, d)

        d = (d + "12.25".deci * 12 + "1.200".deci)
        assertDecEquals("149.2".deci, d)

        dd = Deci("1.2") * 5 * perc / 100
        d += dd
        assertDecEquals("1.8".deci, dd)
        assertDecEquals("151".deci, d)

        dd = (50.deci / 10) % 600
        d -= dd

        assertDecEquals("5".deci, dd)
        assertDecEquals("146".deci, d)
        if (d > 100.deci)
            d -= 100
        assertDecEquals("46".deci, d)

        assertNull(d.decimal)
        assertEquals("46", d.tinyDec.toString())
    }

    @Test
    fun test_deciTiny_tryInitTinyDec() {
        // TinyDec
        val d1 = Deci("-12.100000".toBigDecimal())
        assertEquals(ERR_VALUE, d1.tinyDec.raw)
        d1.tryInitTinyDec()
        assertFalse(isFlagSet(d1.mixed, FLAG_TINY_DEC4))
        assertTrue(isFlagSet(d1.mixed, FLAG_NEGATIVE))
        assertTrue(isFlagSet(d1.mixed, FLAG_TINY_TRIM))
        assertTrue(isFlagSet(d1.mixed, FLAG_BIGD_TRIM))
        assertEquals("12.1", d1.tinyDec.toString())
        assertEquals("-12.1", d1.toString())

        // TinyDec4d
        val d2 = Deci("-12.1000100000".toBigDecimal())
        assertEquals(ERR_VALUE, d2.tinyDec.raw)
        d2.tryInitTinyDec()
        assertTrue(isFlagSet(d2.mixed, FLAG_TINY_DEC4))
        assertTrue(isFlagSet(d2.mixed, FLAG_NEGATIVE))
        assertTrue(isFlagSet(d2.mixed, FLAG_TINY_TRIM))
        assertTrue(isFlagSet(d1.mixed, FLAG_BIGD_TRIM))
        assertEquals("-12.10001", d2.toString())
    }

    @Test
    fun test_deciTiny_round() {
        // TinyDec
        step {
            val d = Deci("-12.11".toBigDecimal())
            d.tryInitTinyDec()
            assertFalse(isFlagSet(d.mixed, FLAG_TINY_DEC4))
            val res = d.round(1)
            assertEquals("-12.1", res.toString())
        }

        // TinyDec4d
        val d1 = Deci("-12.123456".toBigDecimal())
        step {
            d1.tryInitTinyDec()
            assertTrue(isFlagSet(d1.mixed, FLAG_TINY_DEC4))
            val res = d1.round(5)
            assertEquals("-12.12346", res.toString())
        }

        // TinyDec4d -> TinyDec
        step {
            val res = d1.round(2)
            assertEquals("-12.12", res.toString())
            assertFalse(isFlagSet(res.mixed, FLAG_TINY_DEC4))
        }
    }

    @Test
    fun test_deciTiny_init() {
        step {
            val d = 1_000_000_001.deci
            assertEquals(1_000_000_001.toBigDecimal(), d.toBigDecimal())
            assertEquals(ERR_VALUE, d.tinyDec.raw)
        }

        step {
            val d = (-1L).deci
            assertEquals("-1", d.toString())
            assertTrue(isFlagSet(d.mixed, FLAG_NEGATIVE))
            assertEquals(1, d.tinyDec.raw)
        }

        val ctx1 = DeciContext.of(scale = 1, roundingMode = HALF_UP, precision = 1)
        val template = Deci.valueOf("1", ctx1)

        // TinyDec
        step {
            val res = template.createFromUnscaledPos(1234, 3, false)!!
            assertEquals(ERR_VALUE, res.tinyDec.raw)
            assertEquals("1.2",res.toString())
        }

        // TinyDec4d
        step {
            val res = template.createFromUnscaledPos(12345, 4, false)!!
            assertEquals(ERR_VALUE, res.tinyDec.raw)
            assertEquals("1.2",res.toString())
        }
    }

    @Test
    fun test_deciTiny_toInt() {
        val d = (-1).deci
        assertEquals(-1, d.toInt())
        assertEquals(-1L, d.toLong())
        assertEquals((-1).toShort(), d.toShort())
        assertEquals((-1).toByte(), d.toByte())
        assertEquals((-1).toDouble(), d.toDouble())
        assertEquals((-1).toFloat(), d.toFloat())
    }

    @Test
    fun test_var_math_tinyDec4d() {
        val perc = 30.deci
        var d = Deci("1")
        var dd: Deci
        assertDecEquals("1".deci, d)
        assertNull(d.decimal)
        assertEquals(1, d.tinyDec.raw)

        d = d + "12.25".deci * 12 + "1.2001".deci
        assertDecEquals("149.2001".deci, d)

        dd = Deci("1.2") * 5 * perc / 100
        d += dd
        assertDecEquals("1.8".deci, dd)
        assertDecEquals("151.0001".deci, d)

        dd = (50.deci / 10) % 600
        d -= dd

        assertDecEquals("5".deci, dd)
        assertDecEquals("146.0001".deci, d)
        if (d > 100.deci)
            d -= 100

        assertEquals("46.0001", d.toString())

        dd = d + "0.00000001".deci

        assertEquals(ERR_VALUE, dd.tinyDec.raw)
        assertEquals("46.00010001", dd.toString())
    }

    private fun dec(s: String): TinyDec {
        if (s == "ERR")
            return TinyDec.ERR
        return TinyDec.parseString(s)
    }

    private fun assertDecEquals(dec1: Deci, dec2: Deci) {
        assertEquals(dec1, dec2, "Decimals are not equal ($dec1 vs $dec2)")
    }

    // step of subtest - block, may reduce variables scope
    private fun step(testFn: () -> Unit) {
        testFn()
    }

    companion object {
        private const val FLAG_TINY_INIT: Int = 1 shl 26 // have tried to init tinyDec (even if failed)
        private const val FLAG_TINY_TRIM: Int = 1 shl 27
        private const val FLAG_BIGD_TRIM: Int = 1 shl 28
        private const val FLAG_NEGATIVE: Int = 1 shl 29
        private const val FLAG_TINY_DEC4: Int = 1 shl 30 // tinyDec is TinyDec4d, not TinyDec

        fun isFlagSet( mixed: Int, flag: Int) = mixed and flag != 0
    }
}
