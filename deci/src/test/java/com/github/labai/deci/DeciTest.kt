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

import com.github.labai.deci.Deci.DeciContext
import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.math.RoundingMode.DOWN
import java.math.RoundingMode.HALF_UP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * @author Augustus
 *         created on 2020.11.18
 */
class DeciTest {

    @Test
    fun test_equals() {
        // eq
        assertTrue(Deci(BigDecimal("12.2")) eq BigDecimal("12.2"))
        assertTrue(Deci(1) eq 1.deci)
        assertTrue(Deci("1.00") eq 1.deci)
        assertTrue(Deci(1) eq 1L.deci)
        assertTrue(1.deci eq 1)
        assertTrue(1.deci eq 1L)
        assertTrue(1.deci eq 1.0)
        assertTrue(1.deci eq 1.toShort())

        // ==
        assertEquals(Deci(1), 1.deci)
        assertEquals(Deci("1.00"), 1.deci)
        assertEquals(Deci(1), 1L.deci)
        assertEquals(Deci("1.00"), Deci("1.000") / 100000 * 100000)
    }

    @Test
    fun test_operators() {
        assertDecEquals("3", 2.deci + 1)
        assertDecEquals("3", 2.deci + 1L)
        assertDecEquals("3", 2.deci + 1.toBigDecimal())

        assertDecEquals("1.4", Deci("1.2") * 2L - 1)

        // unary minus
        assertDecEquals("-1.1", -Deci("1.1"))
    }

    @Test
    fun test_division_simple() {
        assertDecEquals("1", 2.deci / 2)
        assertDecEquals("1.5", 3.deci / 2L)
        assertDecEquals("1.01", Deci("2.02") / 2)
    }

    @Test
    fun test_division_complex() {
        // (1 - (1/365)) * (1 - (2/365)
        val d = (1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11
        assertDecEquals("0.99179583412", d)

        val d2 = (BigDecimal.ONE - BigDecimal.ONE / BigDecimal(365)) * (BigDecimal.ONE - BigDecimal(2) / BigDecimal(365))
        assertDecEquals("1", d2) // [INFO] WRONG with original BigDecimal!

        val d3 = 1.deci / Deci("1.23e10") * Deci("2.34e-10") * BigDecimal("1e20") round 11
        assertDecEquals("1.90243902439", d3)
    }

    @Test
    fun test_rounding() {
        assertDecEquals("1.11", Deci("1.114").round(2))
        assertDecEquals("1.12", Deci("1.115") round 2)
        assertEquals(BigDecimal("1.11000"), Deci("1.11").round(5).toBigDecimal())
    }

    @Test
    fun test_toBigDecimal() {
        assertEquals(BigDecimal("1.11"), (Deci("1.11") round 2).toBigDecimal())
        assertEquals(BigDecimal("1.1100"), (Deci("1.11") round 4).toBigDecimal())
        assertDecEquals("1.12", Deci("1.115") round 2)
    }

    @Test
    fun test_valueOf() {
        assertSame(0.deci, Deci.valueOf(0))
        assertSame(0.deci, Deci.valueOf(0L))

        assertEquals(2.deci, Deci.valueOf(2.toByte()))
        assertEquals(2.deci, Deci.valueOf(2.toShort()))
        assertEquals(2.deci, Deci.valueOf(2))
        assertEquals(2.deci, Deci.valueOf(2L))
        assertEquals(2.deci, Deci.valueOf(2.toBigDecimal()))
        assertEquals(2.deci, Deci.valueOf("2"))

        assertDecEquals("2.2".deci, Deci.valueOf(2.2) round 10)
        assertDecEquals("2.2".deci, Deci.valueOf(2.2.toFloat()) round 5)

        // floats are not precise
        assertFalse("2.2".deci eq Deci.valueOf(2.2.toFloat()))
    }

    @Test
    fun test_int_long() {
        assertDecEquals(10.deci, 5.deci + 5)
        assertDecEquals(10.deci, 5.deci + 5L)
    }

    @Test
    fun test_valueOf_withContext() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)

        assertEquals(0.deci, Deci.valueOf(0, ctx4))
        assertEquals(0.deci, Deci.valueOf(0L, ctx4))

        assertEquals(2.deci, Deci.valueOf(2.deci, ctx4))
        assertEquals(2.deci, Deci.valueOf(2.toByte(), ctx4))
        assertEquals(2.deci, Deci.valueOf(2.toShort(), ctx4))
        assertEquals(2.deci, Deci.valueOf(2, ctx4))
        assertEquals(2.deci, Deci.valueOf(2L, ctx4))
        assertEquals(2.deci, Deci.valueOf(2.toBigDecimal(), ctx4))
        assertEquals(2.deci, Deci.valueOf("2", ctx4))

        assertEquals(ctx4, Deci.valueOf(2.deci, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2.toByte(), ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2.toShort(), ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2L, ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf(2.toBigDecimal(), ctx4).deciContext)
        assertEquals(ctx4, Deci.valueOf("2", ctx4).deciContext)
    }

    @Test
    fun test_compare() {
        assertTrue(2.deci > 1.toBigDecimal())
        assertTrue(2.deci > 1)
        assertTrue(2.deci >= 2)
        assertTrue(2.deci <= 2L)
        assertTrue(2.deci <= 2.toByte())
        assertTrue(2.deci <= 2.toShort())
        assertTrue(2.deci < 2.2.toDouble())
        assertTrue(2.deci < 2.2.toFloat())
    }

    @Test
    fun test_hashcode() {
        val list = (0..5).map { Deci("$it.${it}000") }
        val map = list.map { it to it * 10 }.toMap()
        // searching in map uses hashcode
        assertEquals(22.deci, map[Deci("2.2")])
        // should be cached
        val d = 22.deci
        assertTrue(d.hashCode() === d.hashCode())
    }

    @Test
    fun test_exceptions() {
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
    fun test_deciContext() {
        // should keep first operator DeciContext

        val d1 = Deci(BigDecimal("1.2"), DeciContext(55))
        val d2 = d1 / 7.deci
        assertEquals(55, d2.toBigDecimal().scale())

        val d3 = Deci(BigDecimal("1.192"), DeciContext(1, DOWN, 1))
        assertDecEquals("1.1", d3) // rounded down
    }

    @Test
    fun test_scale() {
        // (ctx4) should keep scale
        //  - if provided < 4 - then use provided scale
        //  - use 4 - if provided scale is bigger
        //      - but keep minimum precision 3 (minimum non zero digits)
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkScale(expectedScale: Int, num: String) {
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).toBigDecimal().scale())
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).toBigDecimal().scale()) // check with negative value also
        }

        checkScale(0, "1.1e+5")
        checkScale(4, "123.123456")
        checkScale(2, "1.12")
        checkScale(2, "0.12")
        checkScale(3, "0.120") // trailing zeros are not ignored
        checkScale(3, "0.123")
        checkScale(4, "0.1234")
        checkScale(4, "0.01234")
        checkScale(5, "0.001234")
        checkScale(6, "0.0001234")
        checkScale(7, "0.0000001")
        checkScale(8, "0.00000010")
        checkScale(6, "1.1e-5")
        checkScale(0, "0")
        checkScale(1, "0.0") // from 2022-01-26 - keep scale for zero
        checkScale(6, "0.000000")
        checkScale(0, "10")
    }

    @Test
    fun test_divScale() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkDivScale(expectedScale: Int, num: String, divisor: String) {
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).calcDivScale(BigDecimal(divisor)))
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).calcDivScale(BigDecimal(divisor))) // check with negative value also
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).calcDivScale(BigDecimal("-$divisor")))
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).calcDivScale(BigDecimal("-$divisor")))
        }

        checkDivScale(4, "10.1", "12.2")
        checkDivScale(4, "100", "11")
        checkDivScale(5, "10.1", "1000") // 0.01010
        checkDivScale(5, "10.1", "9999") // 0.00101
        checkDivScale(6, "11", "10000") // 0.00110
        checkDivScale(6, "11", "99999") // 0.000110
        checkDivScale(7, "0.011", "100") // 0.0001100
        checkDivScale(7, "0.011", "999") // 0.0000110
        checkDivScale(5, "1", "100") // ?
        checkDivScale(5, "0", "999") //

        checkDivScale(4, "0.011", "0.01") // 1.1000
        checkDivScale(7, "0.0000110", "0.01") // 0.001100 (left original scale)
        checkDivScale(7, "0.0000110", "0.09") // 0.000122 (left original scale)
        checkDivScale(5, "0.00011", "0.01") // 0.01100
        checkDivScale(5, "0.00011", "0.0999") // 0.00110
    }

    @Test
    fun test_round_precedence() {
        // round should be on result after all operators executed, not for last argument
        assertDecEquals("1.2".deci, ("1.16".deci - ("0.02".deci round 1) round 1)) // when rounded last argument
        assertDecEquals("1.1".deci, "1.16".deci - "0.02".deci round 1) // when rounded result
    }

    @Test
    fun test_sumOf() {
        val list = listOf(Deci("1.2"), 1.deci)
        assertDecEquals("2.2", list.sumOf { it })
    }

    @Test
    fun test_toString() {
        assertEquals("-12.02", Deci("-12.0200").toString())
        assertEquals("12", Deci("12.0000").toString())
        assertEquals("1200", Deci("1200.0000").toString())
        assertEquals("1200000", Deci("12e5").toString())
        assertEquals("0.00000000000000000000012", Deci("0.00000000000000000000012").toString())
        assertEquals("12000", Deci("12000.0".toBigDecimal().setScale(-2)).toString())
    }

    @Test
    fun test_applyDeciContext() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        val dec1 = Deci.valueOf("1.0123456789")
        val res = dec1.applyDeciContext(ctx4)
        assertEquals(ctx4, res.deciContext)
        assertEquals("1.0123", res.toString())
    }

    @Test
    fun test_all_operations() {
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

        assertDecEquals(15.deci, num + BigDecimal.TEN)
        assertDecEquals((-5).deci, num - BigDecimal.TEN)
        assertDecEquals(50.deci, num * BigDecimal.TEN)
        assertDecEquals("0.5".deci, num / BigDecimal.TEN)
        assertDecEquals(5.deci, num % BigDecimal.TEN)

        assertDecEquals(15.deci, num + 10.deci)
        assertDecEquals((-5).deci, num - 10.deci)
        assertDecEquals(50.deci, num * 10.deci)
        assertDecEquals("0.5".deci, num / 10.deci)
        assertDecEquals(5.deci, num % 10.deci)

        assertDecEquals("0.1".deci, "2.5".deci % "1.2".deci) // 2.5 - 2.4 = 0.1
    }

    @Test
    fun test_orZero() {
        val num: Deci? = null
        assertEquals(0.deci, num.orZero())
    }

    @Test
    fun test_serializable() {
        val num: Deci = 5.deci

        var ex: Exception? = null
        try {
            ObjectOutputStream(ByteArrayOutputStream()).writeObject(num)
        } catch (e: NotSerializableException) {
            ex = e
        }
        assertNull(ex, "Expect Deci to be Serializable")
    }

    @Test
    fun test_demo1() {
        class Demo1(val quantity: Deci, val price: Deci, val fee: Deci) {
            fun getPercent1(): Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
        }
        val demo = Demo1("12.2".deci, "55.97".deci, "15.5".deci)

        val res2: BigDecimal = ((demo.price * demo.quantity - demo.fee) * 100 / (demo.price * demo.quantity) round 8).toBigDecimal()

        assertDecEquals("97.73004859", res2)
        assertDecEquals("97.73", demo.getPercent1())
    }

    private fun assertDecEquals(dec1: BigDecimal, dec2: BigDecimal) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: Deci, dec2: BigDecimal) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: BigDecimal, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: Deci, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: Deci) = assertTrue(Deci(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: BigDecimal) = assertTrue(BigDecimal(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")

    private infix fun BigDecimal?.eq(other: BigDecimal?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        return this.compareTo(other) == 0
    }
}
