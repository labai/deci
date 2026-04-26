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

import com.github.labai.deci.RoundingMode.HALF_UP
import platform.Foundation.NSDecimalNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Augustus
 * created on 2026-01-01
 */
class DeciTest {

    @Test
    fun ios_deciContext_default() {
        fun checkRound(expectRound: Boolean) {
            val ctx4 = DeciContext.of(scale = 4, roundingMode = HALF_UP, precision = 3)
            val d1 = Deci(1, ctx4) / 3.deci * 3.deci
            val d2 = Deci(1, ctx4) + "1.00004".deci + "1.00004".deci
            val d3 = Deci(3, ctx4) - "1.00004".deci - "1.00004".deci
            if (expectRound) {
                assertEquals("0.9999", d1.toString())
                assertEquals("3", d2.toString())
                assertEquals("1", d3.toString())
            } else {
                assertEquals("1", d1.toString()) // internally keep precision higher
                assertEquals("3.0001", d2.toString())
                assertEquals("0.9999", d3.toString())
            }
        }
        try {
            checkRound(true)
            println("Test step - changing default config to not round")
            Deci.defaultDeciContext = Deci.originalDefaultDeciContext
                .withConfig(DeciContextConfig(roundToScale = false))
            checkRound(false)
        } finally {
            println("Test step - restoring default config")
            Deci.defaultDeciContext = Deci.originalDefaultDeciContext
            checkRound(true)
        }
    }

    @Test
    fun ios_demo1() {
        class Demo1(val quantity: Deci, val price: Deci, val fee: Deci) {
            fun getPercent1(): Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
        }
        val demo = Demo1("12.2".deci, "55.97".deci, "15.5".deci)

        val res2: NSDecimalNumber = ((demo.price * demo.quantity - demo.fee) * 100 / (demo.price * demo.quantity) round 8).toNSDecimalNumber()

        assertDecEquals("97.73004859", res2)
        assertDecEquals("97.73", demo.getPercent1())
    }

    private fun assertDecEquals(dec1: String, dec2: Deci) = assertTrue(Deci(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: NSDecimalNumber) = assertTrue(NSDecimalNumber(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")

    private infix fun NSDecimalNumber?.eq(other: NSDecimalNumber?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        return this == other
    }
}
