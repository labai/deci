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

import platform.Foundation.NSDecimalNumber
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @author Augustus
 * created on 2026-01-01
 */
class DeciTest {

    @Test
    fun js_demo1() {
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
