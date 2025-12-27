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
import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * @author Augustus
 *         created on 2020.11.18
 *
 * additional jvm tests to DeciCommonTest
 * including BigDecimal
 */
class DeciTest {

    @Test
    fun jvm_equals() {
        assertTrue(Deci(BigDecimal("12.2")) eq BigDecimal("12.2"))
    }

    @Test
    fun jvm_operators() {
        assertDecEquals("3", 2.deci + 1.toBigDecimal())
    }

    @Test
    fun jvm_division_complex() {
        val d2 = (BigDecimal.ONE - BigDecimal.ONE / BigDecimal(365)) * (BigDecimal.ONE - BigDecimal(2) / BigDecimal(365))
        assertDecEquals("1", d2) // [INFO] WRONG with original BigDecimal!
    }

    @Test
    fun jvm_rounding() {
        assertEquals(BigDecimal("1.11000"), Deci("1.11").round(5).toBigDecimal())
    }

    @Test
    fun jvm_toBigDecimal() {
        assertEquals(BigDecimal("1.11"), (Deci("1.11") round 2).toBigDecimal())
        assertEquals(BigDecimal("1.1100"), (Deci("1.11") round 4).toBigDecimal())
        assertDecEquals("1.12", Deci("1.115") round 2)
    }

    @Test
    fun jvm_valueOf() {
        assertEquals(2.deci, Deci.valueOf(2.toBigDecimal()))

        // floats are not precise
        assertFalse("2.2".deci eq Deci.valueOf(2.2.toFloat()))
    }

    @Test
    fun jvm_valueOf_withContext() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)

        assertEquals(2.deci, Deci.valueOf(2.toBigDecimal(), ctx4))
        assertEquals(ctx4, Deci.valueOf(2.toBigDecimal(), ctx4).deciContext)
    }

    @Test
    fun jvm_compare() {
        assertTrue(2.deci > 1.toBigDecimal())
    }

    @Test
    fun jvm_scale() {
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
    fun jvm_divScale() {
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
    fun jvm_toString() {
        assertEquals("12000", Deci("12000.0".toBigDecimal().setScale(-2)).toString())
    }

    @Test
    fun jvm_all_operations() {
        val num: Deci = 5.deci

        assertDecEquals(15.deci, num + BigDecimal.TEN)
        assertDecEquals((-5).deci, num - BigDecimal.TEN)
        assertDecEquals(50.deci, num * BigDecimal.TEN)
        assertDecEquals("0.5".deci, num / BigDecimal.TEN)
        assertDecEquals(5.deci, num % BigDecimal.TEN)
    }

    @Test
    fun jvm_serializable() {
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
    fun jvm_demo1() {
        class Demo1(val quantity: Deci, val price: Deci, val fee: Deci) {
            fun getPercent1(): Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
        }
        val demo = Demo1("12.2".deci, "55.97".deci, "15.5".deci)

        val res2: BigDecimal = ((demo.price * demo.quantity - demo.fee) * 100 / (demo.price * demo.quantity) round 8).toBigDecimal()

        assertDecEquals("97.73004859", res2)
        assertDecEquals("97.73", demo.getPercent1())
    }

    private fun assertDecEquals(dec1: Deci, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: Deci) = assertTrue(Deci(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: BigDecimal) = assertTrue(BigDecimal(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")

    private infix fun BigDecimal?.eq(other: BigDecimal?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        return this.compareTo(other) == 0
    }
}
