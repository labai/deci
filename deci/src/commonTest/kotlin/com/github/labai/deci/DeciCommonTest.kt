/*
MIT License

Copyright (c) 2020 Augustus

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

import com.github.labai.deci.RoundingMode.DOWN
import com.github.labai.deci.RoundingMode.HALF_UP
import kotlin.test.*

/**
 * @author Augustus
 *         created on 2020.11.18
 */
class DeciCommonTest {

    @Test
    fun common_equals() {
        // eq
        assertTrue(Deci(1) eq 1.deci)
        assertTrue(Deci("1.00") eq 1.deci)
        assertTrue(Deci(1) eq 1L.deci)
        assertTrue(1.deci eq 1)
        assertTrue(1.deci eq 1L)
        assertTrue(1.deci eq 1.0)
        assertTrue(1.deci eq 1.toShort())

        // ==
        assertEquals(Deci(1), Deci(1))
        assertEquals(Deci(1), 1.deci)
        assertEquals(Deci("1.00"), 1.deci)
        assertEquals(Deci(1), 1L.deci)
        assertEquals(Deci("1.00"), Deci("1.000") / 100000 * 100000)
    }

    @Test
    fun common_operators() {
        assertDecEquals("3", 2.deci + 1)
        assertDecEquals("3", 2.deci + 1L)

        assertDecEquals("1.4", Deci("1.2") * 2L - 1)

        // unary minus
        assertDecEquals("-1.1", -Deci("1.1"))
    }

    @Test
    fun common_division_simple() {
        assertDecEquals("1", 2.deci / 2)
        assertDecEquals("1.5", 3.deci / 2L)
        assertDecEquals("1.01", Deci("2.02") / 2)
    }

    @Test
    fun common_division_complex() {
        // (1 - (1/365)) * (1 - (2/365)
        val d = (1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11
        assertDecEquals("0.99179583412", d)

        val d3 = 1.deci / Deci("1.23e10") * Deci("2.34e-10") * Deci("1e20") round 11
        assertDecEquals("1.90243902439", d3)
    }

    @Test
    fun common_rounding() {
        assertDecEquals("1.11", Deci("1.114").round(2))
        assertDecEquals("1.12", Deci("1.115") round 2)
    }

    @Test
    fun common_valueOf() {
        assertSame(0.deci, Deci.valueOf(0))
        assertSame(0.deci, Deci.valueOf(0L))

        assertEquals(2.deci, Deci.valueOf(2.toByte()))
        assertEquals(2.deci, Deci.valueOf(2.toShort()))
        assertEquals(2.deci, Deci.valueOf(2))
        assertEquals(2.deci, Deci.valueOf(2L))
        assertEquals(2.deci, Deci.valueOf("2"))

        assertDecEquals("2.2".deci, Deci.valueOf(2.2) round 10)
        assertDecEquals("2.2".deci, Deci.valueOf(2.2.toFloat()) round 5)
    }

    @Test
    fun common_int_long() {
        assertDecEquals(10.deci, 5.deci + 5)
        assertDecEquals(10.deci, 5.deci + 5L)
    }

    @Test
    fun common_valueOf_withContext() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)

        assertEquals(0.deci, Deci.valueOf(0, ctx4))
        assertEquals(0.deci, Deci.valueOf(0L, ctx4))

        assertEquals(2.deci, Deci.valueOf(2.deci, ctx4))
        assertEquals(2.deci, Deci.valueOf(2.toByte(), ctx4))
        assertEquals(2.deci, Deci.valueOf(2.toShort(), ctx4))
        assertEquals(2.deci, Deci.valueOf(2, ctx4))
        assertEquals(2.deci, Deci.valueOf(2L, ctx4))
        assertEquals(2.deci, Deci.valueOf("2", ctx4))

        assertEquals(ctx4, Deci.valueOf(2.deci, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2.toByte(), ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2.toShort(), ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2L, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf("2", ctx4).deciContext)
    }

    @Test
    fun common_compare() {
        assertTrue(2.deci > 1)
        assertTrue(2.deci >= 2)
        assertTrue(2.deci <= 2L)
        assertTrue(2.deci <= 2.toByte())
        assertTrue(2.deci <= 2.toShort())
        assertTrue(2.deci < 2.2.toDouble())
        assertTrue(2.deci < 2.2.toFloat())
    }

    @Test
    fun common_hashcode() {
        val list = (0..5).map { Deci("$it.${it}000") }
        val map = list.map { it to it * 10 }.toMap()
        // searching in map uses hashcode
        assertEquals(22.deci, map[Deci("2.2")])
        // should be cached
        val d = 22.deci
        assertTrue(d.hashCode() === d.hashCode())
    }

    @Test
    fun common_exceptions() {
        val d1: Deci = 0.deci

        val d2: Deci? = try {
            Deci("12.2") / d1
            throw IllegalStateException("Expected div/0")
        } catch (e: Exception) {
            null
        }
        assertNull(d2)
    }

    @Test
    fun common_deciContext() {
        // should keep first operator DeciContext

        val d1 = Deci("1.2", DeciContext(55))
        val d2 = d1 / 7.deci
        assertEquals(55, d2.deciContext.scale)

        val d3 = Deci("1.192", DeciContext(1, DOWN, 1))
        assertEquals("1.1", d3.toString()) // rounded down
    }

    @Test
    fun common_round_precedence() {
        // round should be on result after all operators executed, not for last argument
        assertDecEquals("1.2".deci, ("1.16".deci - ("0.02".deci round 1) round 1)) // when rounded last argument
        assertDecEquals("1.1".deci, "1.16".deci - "0.02".deci round 1) // when rounded result
    }

    @Test
    fun common_sumOf() {
        val list = listOf(Deci("1.2"), 1.deci)
        assertDecEquals("2.2", list.sumOf { it })
    }

    @Test
    fun common_toString() {
        assertEquals("-12.02", Deci("-12.0200").toString())
        assertEquals("12", Deci("12.0000").toString())
        assertEquals("1200", Deci("1200.0000").toString())
        assertEquals("1200000", Deci("12e5").toString())
        assertEquals("0.00000000000000000000012", Deci("0.00000000000000000000012").toString())
    }

    @Test
    fun common_applyDeciContext() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        val dec1 = Deci.valueOf("1.0123456789")
        val res = dec1.applyDeciContext(ctx4)
        assertEquals(ctx4, res.deciContext)
        assertEquals("1.0123", res.toString())
    }

    @Test
    fun common_all_operations() {
        val num: Deci = 5.deci

        assertDecEquals((-5).deci, -num)

        assertDecEquals(15.deci, num + 10L)
        assertDecEquals((-5).deci, num - 10L)
        assertDecEquals(50.deci, num * 10L)
        assertDecEquals("0.5".deci, num / 10L)
        assertDecEquals(5.deci, num % 10L)

        assertDecEquals(15.deci, num + 10)
        assertDecEquals((-5).deci, num - 10)
        assertDecEquals(50.deci, num * 10)
        assertDecEquals("0.5".deci, num / 10)
        assertDecEquals(5.deci, num % 10)

        assertDecEquals(15.deci, num + 10.deci)
        assertDecEquals((-5).deci, num - 10.deci)
        assertDecEquals(50.deci, num * 10.deci)
        assertDecEquals("0.5".deci, num / 10.deci)
        assertDecEquals(5.deci, num % 10.deci)

        assertDecEquals("0.1".deci, "2.5".deci % "1.2".deci) // 2.5 - 2.4 = 0.1
    }

    @Test
    fun common_orZero() {
        val num: Deci? = null
        assertEquals(0.deci, num.orZero())
    }

    private fun assertDecEquals(dec1: Deci, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: Deci) = assertTrue(Deci(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
}
